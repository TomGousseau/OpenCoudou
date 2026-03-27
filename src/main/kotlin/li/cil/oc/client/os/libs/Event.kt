package li.cil.oc.client.os.libs

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Event system library for SkibidiOS2.
 * Compatible with SkibidiLuaOS/OpenComputers event API.
 * Provides event pushing, pulling, and listener registration.
 */
object Event {
    
    /**
     * Event data class.
     */
    data class EventData(
        val name: String,
        val args: List<Any?>,
        val timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * Listener callback type.
     */
    typealias EventListener = suspend (EventData) -> Boolean?
    
    /**
     * Timer callback type.
     */
    typealias TimerCallback = suspend () -> Unit
    
    // Event queue
    private val eventQueue = Channel<EventData>(Channel.UNLIMITED)
    
    // Registered listeners: event name -> list of (id, listener)
    private val listeners = ConcurrentHashMap<String, MutableList<Pair<Long, EventListener>>>()
    
    // All listeners (for wildcard listening)
    private val globalListeners = mutableListOf<Pair<Long, EventListener>>()
    
    // Listener ID counter
    private val listenerIdCounter = AtomicLong(0)
    
    // Active timers: id -> Job
    private val timers = ConcurrentHashMap<Long, Job>()
    private val timerIdCounter = AtomicLong(0)
    
    // Scope for coroutines
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    // Mutex for listener modifications
    private val listenerMutex = Mutex()
    
    // Event processing job
    private var processingJob: Job? = null
    
    /**
     * Start the event processing loop.
     */
    fun start() {
        if (processingJob?.isActive == true) return
        
        processingJob = scope.launch {
            for (event in eventQueue) {
                processEvent(event)
            }
        }
    }
    
    /**
     * Stop the event processing loop.
     */
    fun stop() {
        processingJob?.cancel()
        processingJob = null
        timers.values.forEach { it.cancel() }
        timers.clear()
    }
    
    /**
     * Push an event to the queue.
     */
    fun push(name: String, vararg args: Any?) {
        scope.launch {
            eventQueue.send(EventData(name, args.toList()))
        }
    }
    
    /**
     * Push an event and wait for it to be queued.
     */
    suspend fun pushSync(name: String, vararg args: Any?) {
        eventQueue.send(EventData(name, args.toList()))
    }
    
    /**
     * Pull the next event, optionally filtering by name.
     * Blocks until an event is available or timeout.
     * 
     * @param timeout Timeout in seconds (null for infinite)
     * @param filterNames Event names to filter for (empty for any)
     * @return Event data or null on timeout
     */
    suspend fun pull(timeout: Double? = null, vararg filterNames: String): EventData? {
        val timeoutMs = timeout?.let { (it * 1000).toLong() }
        val filterSet = filterNames.toSet()
        
        return withTimeoutOrNull(timeoutMs ?: Long.MAX_VALUE) {
            while (true) {
                val event = eventQueue.receive()
                
                // Process through listeners first
                processEvent(event)
                
                // Check if this event matches our filter
                if (filterSet.isEmpty() || event.name in filterSet) {
                    return@withTimeoutOrNull event
                }
            }
            @Suppress("UNREACHABLE_CODE")
            null
        }
    }
    
    /**
     * Pull the next event synchronously (for non-coroutine contexts).
     */
    fun pullBlocking(timeout: Double? = null, vararg filterNames: String): EventData? {
        return runBlocking {
            pull(timeout, *filterNames)
        }
    }
    
    /**
     * Register a listener for a specific event.
     * 
     * @param name Event name to listen for (or "*" for all events)
     * @param listener Callback that receives event data. Return false to unregister.
     * @return Listener ID for later removal
     */
    suspend fun listen(name: String, listener: EventListener): Long {
        val id = listenerIdCounter.incrementAndGet()
        
        listenerMutex.withLock {
            if (name == "*") {
                globalListeners.add(id to listener)
            } else {
                listeners.getOrPut(name) { mutableListOf() }.add(id to listener)
            }
        }
        
        return id
    }
    
    /**
     * Register a listener (blocking version).
     */
    fun listenBlocking(name: String, listener: EventListener): Long {
        return runBlocking { listen(name, listener) }
    }
    
    /**
     * Unregister a listener by ID.
     */
    suspend fun ignore(listenerId: Long): Boolean {
        listenerMutex.withLock {
            // Check global listeners
            val globalRemoved = globalListeners.removeIf { it.first == listenerId }
            if (globalRemoved) return true
            
            // Check specific listeners
            for ((_, list) in listeners) {
                if (list.removeIf { it.first == listenerId }) {
                    return true
                }
            }
        }
        return false
    }
    
    /**
     * Unregister all listeners for an event name.
     */
    suspend fun ignoreAll(name: String): Int {
        listenerMutex.withLock {
            if (name == "*") {
                val count = globalListeners.size
                globalListeners.clear()
                return count
            } else {
                val list = listeners.remove(name)
                return list?.size ?: 0
            }
        }
    }
    
    /**
     * Create a timer that fires once after a delay.
     * 
     * @param interval Delay in seconds
     * @param callback Function to call
     * @return Timer ID
     */
    fun timer(interval: Double, callback: TimerCallback): Long {
        val id = timerIdCounter.incrementAndGet()
        val delayMs = (interval * 1000).toLong()
        
        val job = scope.launch {
            delay(delayMs)
            try {
                callback()
            } finally {
                timers.remove(id)
            }
        }
        
        timers[id] = job
        return id
    }
    
    /**
     * Create a repeating timer.
     * 
     * @param interval Interval in seconds
     * @param callback Function to call repeatedly. Return false to stop.
     * @return Timer ID
     */
    fun interval(interval: Double, callback: suspend () -> Boolean): Long {
        val id = timerIdCounter.incrementAndGet()
        val delayMs = (interval * 1000).toLong()
        
        val job = scope.launch {
            while (isActive) {
                delay(delayMs)
                if (!callback()) {
                    break
                }
            }
            timers.remove(id)
        }
        
        timers[id] = job
        return id
    }
    
    /**
     * Cancel a timer.
     */
    fun cancelTimer(timerId: Long): Boolean {
        val job = timers.remove(timerId)
        job?.cancel()
        return job != null
    }
    
    /**
     * Clear all timers.
     */
    fun clearTimers() {
        timers.values.forEach { it.cancel() }
        timers.clear()
    }
    
    /**
     * Process an event through all listeners.
     */
    private suspend fun processEvent(event: EventData) {
        val toRemove = mutableListOf<Long>()
        
        listenerMutex.withLock {
            // Global listeners
            for ((id, listener) in globalListeners.toList()) {
                try {
                    if (listener(event) == false) {
                        toRemove.add(id)
                    }
                } catch (e: Exception) {
                    push("event_error", id, e.message)
                }
            }
            
            // Specific listeners
            listeners[event.name]?.toList()?.forEach { (id, listener) ->
                try {
                    if (listener(event) == false) {
                        toRemove.add(id)
                    }
                } catch (e: Exception) {
                    push("event_error", id, e.message)
                }
            }
        }
        
        // Remove listeners that returned false
        for (id in toRemove) {
            ignore(id)
        }
    }
    
    /**
     * Get the number of queued events.
     */
    fun queueSize(): Int {
        return eventQueue.isEmpty.let { if (it) 0 else -1 } // Approximate
    }
    
    /**
     * Get the number of registered listeners.
     */
    suspend fun listenerCount(name: String? = null): Int {
        listenerMutex.withLock {
            return if (name == null) {
                globalListeners.size + listeners.values.sumOf { it.size }
            } else if (name == "*") {
                globalListeners.size
            } else {
                listeners[name]?.size ?: 0
            }
        }
    }
    
    /**
     * Get list of event names with registered listeners.
     */
    suspend fun getListenerNames(): List<String> {
        listenerMutex.withLock {
            val names = listeners.keys.toMutableList()
            if (globalListeners.isNotEmpty()) {
                names.add("*")
            }
            return names
        }
    }
    
    /**
     * Common event names.
     */
    object Names {
        const val KEY_DOWN = "key_down"
        const val KEY_UP = "key_up"
        const val TOUCH = "touch"
        const val DRAG = "drag"
        const val DROP = "drop"
        const val SCROLL = "scroll"
        const val COMPONENT_ADDED = "component_added"
        const val COMPONENT_REMOVED = "component_removed"
        const val REDSTONE_CHANGED = "redstone_changed"
        const val MODEM_MESSAGE = "modem_message"
        const val CLIPBOARD = "clipboard"
        const val SCREEN_RESIZED = "screen_resized"
        const val INTERRUPTED = "interrupted"
    }
}

/**
 * Extension function for easy event listening.
 */
suspend fun <T> withEvent(
    eventName: String,
    timeout: Double? = null,
    block: suspend (Event.EventData) -> T
): T? {
    val event = Event.pull(timeout, eventName) ?: return null
    return block(event)
}
