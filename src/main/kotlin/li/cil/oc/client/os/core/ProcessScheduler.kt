package li.cil.oc.client.os.core

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Advanced Process Scheduler for SkibidiOS2.
 * Implements priority-based preemptive scheduling with time slicing.
 * 
 * Features:
 * - Priority-based scheduling (5 levels)
 * - Round-robin within priority levels
 * - Time slice allocation based on priority
 * - Process groups and sessions
 * - CPU time accounting
 * - Nice values for dynamic priority adjustment
 * - Deadline scheduling for real-time tasks
 */
class ProcessScheduler(private val os: KotlinOS) {
    
    companion object {
        // Time slice durations (ms)
        const val TIME_SLICE_REALTIME = 2L
        const val TIME_SLICE_HIGH = 5L
        const val TIME_SLICE_NORMAL = 10L
        const val TIME_SLICE_LOW = 20L
        const val TIME_SLICE_IDLE = 50L
        
        // Scheduler tick rate
        const val SCHEDULER_TICK_MS = 1L
        
        // Max processes
        const val MAX_PROCESSES = 256
        
        // Nice value range
        const val NICE_MIN = -20
        const val NICE_MAX = 19
    }
    
    // Process tables
    private val processes = ConcurrentHashMap<Int, ScheduledProcess>()
    private val processGroups = ConcurrentHashMap<Int, ProcessGroup>()
    private val sessions = ConcurrentHashMap<Int, Session>()
    
    // Ready queues (one per priority level)
    private val readyQueues = Array(5) { PriorityBlockingQueue<ScheduledProcess>(16, compareBy { it.virtualRuntime }) }
    
    // Blocked processes waiting on I/O or events
    private val blockedProcesses = ConcurrentHashMap<Int, BlockedInfo>()
    
    // Scheduler state
    private val schedulerLock = Mutex()
    private var currentProcess: ScheduledProcess? = null
    private val pidCounter = AtomicInteger(1)
    private val pgidCounter = AtomicInteger(1)
    private val sidCounter = AtomicInteger(1)
    
    // Statistics
    private val totalCpuTime = AtomicLong(0)
    private val contextSwitches = AtomicLong(0)
    private val schedulerTicks = AtomicLong(0)
    
    // Coroutine scope for scheduler
    private val schedulerScope = CoroutineScope(
        Dispatchers.Default + SupervisorJob() + CoroutineName("Scheduler")
    )
    private var schedulerJob: Job? = null
    
    // Event channels
    private val processEvents = Channel<ProcessEvent>(Channel.BUFFERED)
    
    /**
     * Start the scheduler.
     */
    fun start() {
        schedulerJob = schedulerScope.launch {
            schedulerLoop()
        }
        
        // Start event handler
        schedulerScope.launch {
            handleEvents()
        }
    }
    
    /**
     * Stop the scheduler.
     */
    fun stop() {
        schedulerJob?.cancel()
        schedulerScope.cancel()
    }
    
    /**
     * Main scheduler loop.
     */
    private suspend fun schedulerLoop() {
        while (isActive) {
            schedulerLock.withLock {
                tick()
            }
            delay(SCHEDULER_TICK_MS)
            schedulerTicks.incrementAndGet()
        }
    }
    
    /**
     * Single scheduler tick.
     */
    private fun tick() {
        // Check if current process needs preemption
        currentProcess?.let { current ->
            current.cpuTimeSliceUsed++
            current.totalCpuTime++
            totalCpuTime.incrementAndGet()
            
            val timeSlice = getTimeSlice(current.effectivePriority)
            if (current.cpuTimeSliceUsed >= timeSlice) {
                // Time slice expired, preempt
                preempt(current)
            }
        }
        
        // Check blocked processes for unblocking
        checkBlockedProcesses()
        
        // If no current process, schedule next
        if (currentProcess == null || currentProcess?.state != SchedulerState.RUNNING) {
            scheduleNext()
        }
    }
    
    /**
     * Preempt current process.
     */
    private fun preempt(process: ScheduledProcess) {
        if (process.state == SchedulerState.RUNNING) {
            process.state = SchedulerState.READY
            process.cpuTimeSliceUsed = 0
            process.virtualRuntime += getTimeSlice(process.effectivePriority)
            
            // Re-queue
            val queueIndex = process.effectivePriority.ordinal
            readyQueues[queueIndex].offer(process)
            
            currentProcess = null
            contextSwitches.incrementAndGet()
        }
    }
    
    /**
     * Schedule next process.
     */
    private fun scheduleNext() {
        // Find highest priority non-empty queue
        for (priority in ProcessPriority.values()) {
            val queue = readyQueues[priority.ordinal]
            val next = queue.poll()
            if (next != null) {
                currentProcess = next
                next.state = SchedulerState.RUNNING
                next.lastScheduledTime = System.currentTimeMillis()
                contextSwitches.incrementAndGet()
                
                // Resume the coroutine
                next.continuation?.resume(Unit)
                return
            }
        }
        
        // No process to run - idle
        currentProcess = null
    }
    
    /**
     * Check blocked processes for unblocking.
     */
    private fun checkBlockedProcesses() {
        val now = System.currentTimeMillis()
        
        blockedProcesses.entries.removeIf { (pid, info) ->
            val process = processes[pid] ?: return@removeIf true
            
            when (info) {
                is BlockedInfo.Sleep -> {
                    if (now >= info.wakeTime) {
                        unblockProcess(process)
                        true
                    } else false
                }
                is BlockedInfo.IO -> {
                    if (info.isComplete()) {
                        unblockProcess(process)
                        true
                    } else false
                }
                is BlockedInfo.Event -> {
                    if (info.eventReceived) {
                        unblockProcess(process)
                        true
                    } else false
                }
                is BlockedInfo.Deadline -> {
                    if (now >= info.deadline) {
                        unblockProcess(process)
                        true
                    } else false
                }
            }
        }
    }
    
    /**
     * Unblock a process.
     */
    private fun unblockProcess(process: ScheduledProcess) {
        process.state = SchedulerState.READY
        process.cpuTimeSliceUsed = 0
        
        // Boost priority temporarily for interactive response
        if (process.wasBlocked) {
            process.priorityBoost = 1
        }
        
        val queueIndex = process.effectivePriority.ordinal
        readyQueues[queueIndex].offer(process)
        process.wasBlocked = false
    }
    
    /**
     * Create a new process.
     */
    fun createProcess(
        name: String,
        priority: ProcessPriority = ProcessPriority.NORMAL,
        parentPid: Int? = null,
        pgid: Int? = null,
        sid: Int? = null,
        block: suspend () -> Unit
    ): ScheduledProcess {
        val pid = pidCounter.getAndIncrement()
        
        // Inherit or create process group
        val processGroup = pgid?.let { processGroups[it] }
            ?: parentPid?.let { processes[it]?.processGroup }
            ?: createProcessGroup()
        
        // Inherit or create session  
        val session = sid?.let { sessions[it] }
            ?: parentPid?.let { processes[it]?.session }
            ?: createSession()
        
        val process = ScheduledProcess(
            pid = pid,
            name = name,
            basePriority = priority,
            parentPid = parentPid,
            processGroup = processGroup,
            session = session,
            creationTime = System.currentTimeMillis()
        )
        
        // Create the coroutine
        process.job = schedulerScope.launch(start = CoroutineStart.LAZY) {
            try {
                block()
                process.state = SchedulerState.TERMINATED
                process.exitCode = 0
            } catch (e: CancellationException) {
                process.state = SchedulerState.TERMINATED
                process.exitCode = -1
            } catch (e: Exception) {
                process.state = SchedulerState.TERMINATED
                process.exitCode = -1
                process.exitReason = e.message
            } finally {
                onProcessExit(process)
            }
        }
        
        processes[pid] = process
        processGroup.processes.add(process)
        
        // Add to ready queue
        process.state = SchedulerState.READY
        readyQueues[priority.ordinal].offer(process)
        
        // Emit event
        processEvents.trySend(ProcessEvent.Created(process))
        
        return process
    }
    
    /**
     * Create a process group.
     */
    fun createProcessGroup(): ProcessGroup {
        val pgid = pgidCounter.getAndIncrement()
        val group = ProcessGroup(pgid)
        processGroups[pgid] = group
        return group
    }
    
    /**
     * Create a session.
     */
    fun createSession(): Session {
        val sid = sidCounter.getAndIncrement()
        val session = Session(sid)
        sessions[sid] = session
        return session
    }
    
    /**
     * Terminate a process.
     */
    fun terminateProcess(pid: Int, signal: Signal = Signal.SIGTERM): Boolean {
        val process = processes[pid] ?: return false
        
        when (signal) {
            Signal.SIGKILL -> {
                // Force kill
                process.job?.cancel()
                process.state = SchedulerState.TERMINATED
                process.exitCode = 137 // 128 + 9
                onProcessExit(process)
            }
            Signal.SIGTERM -> {
                // Request termination
                process.pendingSignals.add(signal)
            }
            Signal.SIGSTOP -> {
                // Suspend
                process.state = SchedulerState.STOPPED
                removeFromReadyQueue(process)
            }
            Signal.SIGCONT -> {
                // Resume
                if (process.state == SchedulerState.STOPPED) {
                    process.state = SchedulerState.READY
                    readyQueues[process.effectivePriority.ordinal].offer(process)
                }
            }
            else -> {
                process.pendingSignals.add(signal)
            }
        }
        
        return true
    }
    
    /**
     * Remove process from ready queue.
     */
    private fun removeFromReadyQueue(process: ScheduledProcess) {
        for (queue in readyQueues) {
            queue.remove(process)
        }
        if (currentProcess == process) {
            currentProcess = null
        }
    }
    
    /**
     * Handle process exit.
     */
    private fun onProcessExit(process: ScheduledProcess) {
        removeFromReadyQueue(process)
        blockedProcesses.remove(process.pid)
        
        // Notify parent
        process.parentPid?.let { parentPid ->
            processes[parentPid]?.let { parent ->
                parent.childExited(process)
            }
        }
        
        // Clean up after a delay (zombie state)
        schedulerScope.launch {
            delay(1000) // Allow parent to wait()
            processes.remove(process.pid)
            process.processGroup.processes.remove(process)
        }
        
        processEvents.trySend(ProcessEvent.Exited(process))
    }
    
    /**
     * Block current process for sleep.
     */
    suspend fun sleep(durationMs: Long) {
        currentProcess?.let { process ->
            process.state = SchedulerState.BLOCKED
            process.wasBlocked = true
            blockedProcesses[process.pid] = BlockedInfo.Sleep(
                System.currentTimeMillis() + durationMs
            )
            
            // Yield to scheduler
            suspendCancellableCoroutine<Unit> { cont ->
                process.continuation = cont
                currentProcess = null
            }
        }
    }
    
    /**
     * Block current process waiting for I/O.
     */
    suspend fun waitForIO(ioOperation: () -> Boolean) {
        currentProcess?.let { process ->
            process.state = SchedulerState.BLOCKED
            process.wasBlocked = true
            blockedProcesses[process.pid] = BlockedInfo.IO(ioOperation)
            
            suspendCancellableCoroutine<Unit> { cont ->
                process.continuation = cont
                currentProcess = null
            }
        }
    }
    
    /**
     * Block current process waiting for event.
     */
    suspend fun waitForEvent(): ProcessEvent {
        return processEvents.receive()
    }
    
    /**
     * Yield current process voluntarily.
     */
    suspend fun yield() {
        currentProcess?.let { process ->
            preempt(process)
            
            suspendCancellableCoroutine<Unit> { cont ->
                process.continuation = cont
            }
        }
    }
    
    /**
     * Set nice value for process.
     */
    fun setNice(pid: Int, nice: Int): Boolean {
        val process = processes[pid] ?: return false
        process.nice = nice.coerceIn(NICE_MIN, NICE_MAX)
        return true
    }
    
    /**
     * Get time slice for priority.
     */
    private fun getTimeSlice(priority: ProcessPriority): Long {
        return when (priority) {
            ProcessPriority.REALTIME -> TIME_SLICE_REALTIME
            ProcessPriority.HIGH -> TIME_SLICE_HIGH
            ProcessPriority.NORMAL -> TIME_SLICE_NORMAL
            ProcessPriority.LOW -> TIME_SLICE_LOW
            ProcessPriority.IDLE -> TIME_SLICE_IDLE
        }
    }
    
    /**
     * Handle process events.
     */
    private suspend fun handleEvents() {
        for (event in processEvents) {
            when (event) {
                is ProcessEvent.Created -> {}
                is ProcessEvent.Exited -> {}
                is ProcessEvent.Signaled -> {}
            }
        }
    }
    
    // ============================================================
    // Getters
    // ============================================================
    
    fun getProcess(pid: Int): ScheduledProcess? = processes[pid]
    
    fun getCurrentProcess(): ScheduledProcess? = currentProcess
    
    fun listProcesses(): List<ScheduledProcess> = processes.values.toList()
    
    fun getProcessGroup(pgid: Int): ProcessGroup? = processGroups[pgid]
    
    fun getSession(sid: Int): Session? = sessions[sid]
    
    fun getStats(): SchedulerStats = SchedulerStats(
        totalProcesses = processes.size,
        runningProcesses = processes.values.count { it.state == SchedulerState.RUNNING },
        blockedProcesses = blockedProcesses.size,
        totalCpuTime = totalCpuTime.get(),
        contextSwitches = contextSwitches.get(),
        schedulerTicks = schedulerTicks.get()
    )
}

/**
 * Scheduled process with additional scheduler metadata.
 */
data class ScheduledProcess(
    val pid: Int,
    val name: String,
    val basePriority: ProcessPriority,
    val parentPid: Int?,
    val processGroup: ProcessGroup,
    val session: Session,
    val creationTime: Long,
    
    var state: SchedulerState = SchedulerState.NEW,
    var exitCode: Int = 0,
    var exitReason: String? = null,
    
    // Scheduling
    var nice: Int = 0,
    var priorityBoost: Int = 0,
    var virtualRuntime: Long = 0,
    var cpuTimeSliceUsed: Long = 0,
    var totalCpuTime: Long = 0,
    var lastScheduledTime: Long = 0,
    var wasBlocked: Boolean = false,
    
    // Signals
    val pendingSignals: MutableList<Signal> = mutableListOf(),
    
    // Children
    val children: MutableList<ScheduledProcess> = mutableListOf(),
    val exitedChildren: MutableList<ScheduledProcess> = mutableListOf(),
    
    // Coroutine
    var job: Job? = null,
    var continuation: CancellableContinuation<Unit>? = null
) {
    val effectivePriority: ProcessPriority
        get() {
            val niceAdjustment = nice / 7 // Map -20..19 to roughly -2..2
            val boostAdjustment = -priorityBoost
            val adjusted = basePriority.ordinal + niceAdjustment + boostAdjustment
            return ProcessPriority.values()[adjusted.coerceIn(0, 4)]
        }
    
    fun childExited(child: ScheduledProcess) {
        children.remove(child)
        exitedChildren.add(child)
    }
    
    fun wait(): ScheduledProcess? {
        return exitedChildren.removeFirstOrNull()
    }
}

/**
 * Scheduler states.
 */
enum class SchedulerState {
    NEW,
    READY,
    RUNNING,
    BLOCKED,
    STOPPED,
    TERMINATED
}

/**
 * Process group.
 */
data class ProcessGroup(
    val pgid: Int,
    val processes: MutableList<ScheduledProcess> = mutableListOf()
)

/**
 * Session.
 */
data class Session(
    val sid: Int,
    var controllingTerminal: String? = null,
    var foregroundGroup: ProcessGroup? = null
)

/**
 * Blocked process info.
 */
sealed class BlockedInfo {
    data class Sleep(val wakeTime: Long) : BlockedInfo()
    data class IO(val checkComplete: () -> Boolean) : BlockedInfo() {
        fun isComplete(): Boolean = checkComplete()
    }
    data class Event(var eventReceived: Boolean = false) : BlockedInfo()
    data class Deadline(val deadline: Long) : BlockedInfo()
}

/**
 * Unix-style signals.
 */
enum class Signal(val value: Int) {
    SIGHUP(1),
    SIGINT(2),
    SIGQUIT(3),
    SIGILL(4),
    SIGTRAP(5),
    SIGABRT(6),
    SIGBUS(7),
    SIGFPE(8),
    SIGKILL(9),
    SIGUSR1(10),
    SIGSEGV(11),
    SIGUSR2(12),
    SIGPIPE(13),
    SIGALRM(14),
    SIGTERM(15),
    SIGCHLD(17),
    SIGCONT(18),
    SIGSTOP(19),
    SIGTSTP(20),
    SIGTTIN(21),
    SIGTTOU(22)
}

/**
 * Process events.
 */
sealed class ProcessEvent {
    data class Created(val process: ScheduledProcess) : ProcessEvent()
    data class Exited(val process: ScheduledProcess) : ProcessEvent()
    data class Signaled(val process: ScheduledProcess, val signal: Signal) : ProcessEvent()
}

/**
 * Scheduler statistics.
 */
data class SchedulerStats(
    val totalProcesses: Int,
    val runningProcesses: Int,
    val blockedProcesses: Int,
    val totalCpuTime: Long,
    val contextSwitches: Long,
    val schedulerTicks: Long
)
