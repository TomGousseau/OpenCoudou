package li.cil.oc.client.os.components

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Hardware Abstraction Layer (HAL) for SkibidiOS2.
 * Provides a unified interface to all hardware components.
 * 
 * Features:
 * - Device enumeration and hot-plugging
 * - Interrupt handling
 * - DMA transfers
 * - Power management
 * - Driver framework
 */
class HardwareAbstractionLayer {
    
    companion object {
        const val HAL_VERSION = "2.0.0"
    }
    
    // Device registry
    private val devices = ConcurrentHashMap<String, HardwareDevice>()
    private val devicesByType = ConcurrentHashMap<DeviceClass, MutableList<HardwareDevice>>()
    private val drivers = ConcurrentHashMap<DeviceClass, DeviceDriver<*>>()
    
    // Interrupt handling
    private val interruptHandlers = ConcurrentHashMap<Int, InterruptHandler>()
    private val pendingInterrupts = Channel<Interrupt>(Channel.BUFFERED)
    private var interruptCounter = AtomicLong(0)
    
    // Power management
    private val powerState = MutableStateFlow(PowerState.RUNNING)
    private val devicePowerStates = ConcurrentHashMap<String, DevicePowerState>()
    
    // Event bus
    private val eventBus = MutableSharedFlow<HardwareEvent>(replay = 0, extraBufferCapacity = 64)
    
    // Coroutine scope
    private val halScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    /**
     * Initialize the HAL.
     */
    fun initialize() {
        // Register built-in drivers
        registerDriver(DeviceClass.GPU, GpuDriver())
        registerDriver(DeviceClass.SCREEN, ScreenDriver())
        registerDriver(DeviceClass.KEYBOARD, KeyboardDriver())
        registerDriver(DeviceClass.FILESYSTEM, FilesystemDriver())
        registerDriver(DeviceClass.NETWORK, NetworkDriver())
        registerDriver(DeviceClass.EEPROM, EepromDriver())
        registerDriver(DeviceClass.REDSTONE, RedstoneDriver())
        
        // Start interrupt handler
        halScope.launch {
            handleInterrupts()
        }
    }
    
    /**
     * Shutdown the HAL.
     */
    fun shutdown() {
        // Put all devices in low power state
        devices.values.forEach { device ->
            setDevicePower(device.address, DevicePowerState.SUSPENDED)
        }
        halScope.cancel()
    }
    
    // ============================================================
    // Device Management
    // ============================================================
    
    /**
     * Register a hardware device.
     */
    fun registerDevice(device: HardwareDevice): Boolean {
        if (devices.containsKey(device.address)) return false
        
        devices[device.address] = device
        devicesByType.getOrPut(device.deviceClass) { mutableListOf() }.add(device)
        devicePowerStates[device.address] = DevicePowerState.ACTIVE
        
        // Initialize device with driver
        drivers[device.deviceClass]?.let { driver ->
            @Suppress("UNCHECKED_CAST")
            (driver as DeviceDriver<HardwareDevice>).initialize(device)
        }
        
        // Emit event
        halScope.launch {
            eventBus.emit(HardwareEvent.DeviceConnected(device))
        }
        
        return true
    }
    
    /**
     * Unregister a hardware device.
     */
    fun unregisterDevice(address: String): Boolean {
        val device = devices.remove(address) ?: return false
        devicesByType[device.deviceClass]?.remove(device)
        devicePowerStates.remove(address)
        
        // Emit event
        halScope.launch {
            eventBus.emit(HardwareEvent.DeviceDisconnected(device))
        }
        
        return true
    }
    
    /**
     * Get a device by address.
     */
    fun getDevice(address: String): HardwareDevice? = devices[address]
    
    /**
     * Get all devices of a specific class.
     */
    fun getDevicesByClass(deviceClass: DeviceClass): List<HardwareDevice> {
        return devicesByType[deviceClass]?.toList() ?: emptyList()
    }
    
    /**
     * Get the primary device of a class.
     */
    fun getPrimaryDevice(deviceClass: DeviceClass): HardwareDevice? {
        return devicesByType[deviceClass]?.firstOrNull()
    }
    
    /**
     * List all devices.
     */
    fun listDevices(): List<HardwareDevice> = devices.values.toList()
    
    // ============================================================
    // Driver Framework
    // ============================================================
    
    /**
     * Register a device driver.
     */
    fun <T : HardwareDevice> registerDriver(deviceClass: DeviceClass, driver: DeviceDriver<T>) {
        drivers[deviceClass] = driver
        
        // Initialize existing devices
        devicesByType[deviceClass]?.forEach { device ->
            @Suppress("UNCHECKED_CAST")
            driver.initialize(device as T)
        }
    }
    
    /**
     * Get driver for device class.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : HardwareDevice> getDriver(deviceClass: DeviceClass): DeviceDriver<T>? {
        return drivers[deviceClass] as? DeviceDriver<T>
    }
    
    // ============================================================
    // Device I/O
    // ============================================================
    
    /**
     * Read from device.
     */
    fun read(address: String, offset: Int, length: Int): ByteArray? {
        val device = devices[address] ?: return null
        if (devicePowerStates[address] != DevicePowerState.ACTIVE) return null
        
        val driver = drivers[device.deviceClass] ?: return null
        @Suppress("UNCHECKED_CAST")
        return (driver as DeviceDriver<HardwareDevice>).read(device, offset, length)
    }
    
    /**
     * Write to device.
     */
    fun write(address: String, offset: Int, data: ByteArray): Boolean {
        val device = devices[address] ?: return false
        if (devicePowerStates[address] != DevicePowerState.ACTIVE) return false
        
        val driver = drivers[device.deviceClass] ?: return false
        @Suppress("UNCHECKED_CAST")
        return (driver as DeviceDriver<HardwareDevice>).write(device, offset, data)
    }
    
    /**
     * Invoke device method.
     */
    fun invoke(address: String, method: String, vararg args: Any?): Any? {
        val device = devices[address] ?: return null
        if (devicePowerStates[address] != DevicePowerState.ACTIVE) return null
        
        val driver = drivers[device.deviceClass] ?: return null
        @Suppress("UNCHECKED_CAST")
        return (driver as DeviceDriver<HardwareDevice>).invoke(device, method, *args)
    }
    
    // ============================================================
    // Interrupt Handling
    // ============================================================
    
    /**
     * Register an interrupt handler.
     */
    fun registerInterruptHandler(irq: Int, handler: InterruptHandler) {
        interruptHandlers[irq] = handler
    }
    
    /**
     * Unregister an interrupt handler.
     */
    fun unregisterInterruptHandler(irq: Int) {
        interruptHandlers.remove(irq)
    }
    
    /**
     * Raise an interrupt.
     */
    fun raiseInterrupt(irq: Int, data: Any? = null) {
        val interrupt = Interrupt(
            id = interruptCounter.incrementAndGet(),
            irq = irq,
            timestamp = System.currentTimeMillis(),
            data = data
        )
        pendingInterrupts.trySend(interrupt)
    }
    
    /**
     * Process pending interrupts.
     */
    private suspend fun handleInterrupts() {
        for (interrupt in pendingInterrupts) {
            interruptHandlers[interrupt.irq]?.let { handler ->
                try {
                    handler.handle(interrupt)
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
    }
    
    // ============================================================
    // DMA (Direct Memory Access)
    // ============================================================
    
    /**
     * Perform a DMA transfer.
     */
    fun dmaTransfer(
        sourceDevice: String,
        sourceOffset: Int,
        destDevice: String,
        destOffset: Int,
        length: Int
    ): Boolean {
        val data = read(sourceDevice, sourceOffset, length) ?: return false
        return write(destDevice, destOffset, data)
    }
    
    /**
     * Perform async DMA transfer.
     */
    suspend fun dmaTransferAsync(
        sourceDevice: String,
        sourceOffset: Int,
        destDevice: String,
        destOffset: Int,
        length: Int
    ): Boolean = withContext(Dispatchers.IO) {
        dmaTransfer(sourceDevice, sourceOffset, destDevice, destOffset, length)
    }

    // ============================================================
    // Power Management
    // ============================================================
    
    /**
     * Get system power state.
     */
    fun getPowerState(): PowerState = powerState.value
    
    /**
     * Set system power state.
     */
    fun setPowerState(state: PowerState) {
        powerState.value = state
        
        when (state) {
            PowerState.RUNNING -> {
                // Wake all devices
                devices.keys.forEach { setDevicePower(it, DevicePowerState.ACTIVE) }
            }
            PowerState.IDLE -> {
                // Put non-essential devices to sleep
                devices.values
                    .filter { it.deviceClass !in listOf(DeviceClass.SCREEN, DeviceClass.KEYBOARD) }
                    .forEach { setDevicePower(it.address, DevicePowerState.IDLE) }
            }
            PowerState.SUSPENDED -> {
                // Suspend all devices
                devices.keys.forEach { setDevicePower(it, DevicePowerState.SUSPENDED) }
            }
            PowerState.HIBERNATING -> {
                // Deep sleep
                devices.keys.forEach { setDevicePower(it, DevicePowerState.OFF) }
            }
        }
        
        halScope.launch {
            eventBus.emit(HardwareEvent.PowerStateChanged(state))
        }
    }
    
    /**
     * Get device power state.
     */
    fun getDevicePower(address: String): DevicePowerState? = devicePowerStates[address]
    
    /**
     * Set device power state.
     */
    fun setDevicePower(address: String, state: DevicePowerState): Boolean {
        val device = devices[address] ?: return false
        devicePowerStates[address] = state
        
        val driver = drivers[device.deviceClass]
        @Suppress("UNCHECKED_CAST")
        (driver as? DeviceDriver<HardwareDevice>)?.setPower(device, state)
        
        halScope.launch {
            eventBus.emit(HardwareEvent.DevicePowerChanged(device, state))
        }
        
        return true
    }
    
    // ============================================================
    // Event Bus
    // ============================================================
    
    /**
     * Subscribe to hardware events.
     */
    fun observeEvents(): Flow<HardwareEvent> = eventBus.asSharedFlow()
    
    /**
     * Get HAL statistics.
     */
    fun getStats(): HalStats = HalStats(
        version = HAL_VERSION,
        deviceCount = devices.size,
        driverCount = drivers.size,
        interruptCount = interruptCounter.get(),
        powerState = powerState.value
    )
}

// ============================================================
// Device Classes
// ============================================================

enum class DeviceClass {
    GPU,
    SCREEN,
    KEYBOARD,
    FILESYSTEM,
    NETWORK,
    EEPROM,
    REDSTONE,
    ROBOT,
    DRONE,
    CRAFTING,
    INVENTORY,
    TANK,
    GEOLYZER,
    HOLOGRAM,
    NAVIGATION,
    CHUNKLOADER,
    GENERATOR,
    TRANSPOSER,
    DATA,
    DATABASE,
    PRINTER_3D,
    EXPERIENCE,
    LEASH,
    TRACTOR_BEAM,
    PISTON,
    SIGN,
    DEBUG,
    UNKNOWN
}

// ============================================================
// Hardware Device Base
// ============================================================

abstract class HardwareDevice(
    val address: String,
    val deviceClass: DeviceClass,
    val slot: Int = -1
) {
    abstract val deviceName: String
    abstract val methods: List<String>
    
    open val capabilities: Set<DeviceCapability> = emptySet()
    
    abstract fun invoke(method: String, vararg args: Any?): Any?
    abstract fun documentation(method: String): String?
}

enum class DeviceCapability {
    READ,
    WRITE,
    SEEK,
    DMA,
    INTERRUPT,
    POWER_CONTROL
}

// ============================================================
// Device Driver Framework
// ============================================================

interface DeviceDriver<T : HardwareDevice> {
    val supportedClass: DeviceClass
    
    fun initialize(device: T)
    fun shutdown(device: T)
    
    fun read(device: T, offset: Int, length: Int): ByteArray?
    fun write(device: T, offset: Int, data: ByteArray): Boolean
    fun invoke(device: T, method: String, vararg args: Any?): Any?
    
    fun setPower(device: T, state: DevicePowerState)
}

// ============================================================
// Concrete Device Types
// ============================================================

class GpuDevice(
    address: String,
    slot: Int = -1
) : HardwareDevice(address, DeviceClass.GPU, slot) {
    override val deviceName = "Graphics Processing Unit"
    override val methods = listOf(
        "bind", "getScreen", "getBackground", "setBackground",
        "getForeground", "setForeground", "getPaletteColor", "setPaletteColor",
        "maxDepth", "getDepth", "setDepth", "maxResolution", "getResolution",
        "setResolution", "getViewport", "setViewport", "get", "set", "copy", "fill"
    )
    override val capabilities = setOf(DeviceCapability.WRITE, DeviceCapability.DMA)
    
    var boundScreen: String? = null
    var foreground = 0xFFFFFF
    var background = 0x000000
    var resolution = Pair(160, 50)
    var depth = 8
    
    override fun invoke(method: String, vararg args: Any?): Any? {
        return when (method) {
            "bind" -> { boundScreen = args[0] as String; true }
            "getScreen" -> boundScreen
            "getBackground" -> background
            "setBackground" -> { val old = background; background = (args[0] as Number).toInt(); old }
            "getForeground" -> foreground
            "setForeground" -> { val old = foreground; foreground = (args[0] as Number).toInt(); old }
            "getResolution" -> resolution
            "setResolution" -> {
                resolution = Pair((args[0] as Number).toInt(), (args[1] as Number).toInt())
                true
            }
            "getDepth" -> depth
            "setDepth" -> { depth = (args[0] as Number).toInt(); depth }
            else -> null
        }
    }
    
    override fun documentation(method: String): String? = when (method) {
        "bind" -> "bind(address:string[, reset:boolean]):boolean -- Binds the GPU to a screen"
        "set" -> "set(x:number, y:number, value:string[, vertical:boolean]):boolean -- Sets characters"
        "fill" -> "fill(x:number, y:number, w:number, h:number, char:string):boolean -- Fills area"
        else -> null
    }
}

class ScreenDevice(
    address: String,
    slot: Int = -1
) : HardwareDevice(address, DeviceClass.SCREEN, slot) {
    override val deviceName = "Screen"
    override val methods = listOf(
        "isOn", "turnOn", "turnOff", "getAspectRatio",
        "getKeyboards", "setPrecise", "isPrecise",
        "setTouchModeInverted", "isTouchModeInverted"
    )
    
    var isOn = true
    var width = 160
    var height = 50
    var precise = false
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "isOn" -> isOn
        "turnOn" -> { isOn = true; true }
        "turnOff" -> { isOn = false; true }
        "getAspectRatio" -> Pair(width, height)
        "isPrecise" -> precise
        "setPrecise" -> { precise = args[0] as Boolean; precise }
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

class KeyboardDevice(
    address: String,
    slot: Int = -1,
    val screenAddress: String
) : HardwareDevice(address, DeviceClass.KEYBOARD, slot) {
    override val deviceName = "Keyboard"
    override val methods = listOf("getLayout", "setLayout", "pressedCodes", "pressedChars")
    override val capabilities = setOf(DeviceCapability.READ, DeviceCapability.INTERRUPT)
    
    var layout = "us"
    val pressedKeys = mutableSetOf<Int>()
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "getLayout" -> layout
        "setLayout" -> { layout = args[0] as String; true }
        "pressedCodes" -> pressedKeys.toList()
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

class FilesystemDevice(
    address: String,
    val label: String,
    val capacity: Long,
    val isReadOnly: Boolean = false
) : HardwareDevice(address, DeviceClass.FILESYSTEM) {
    override val deviceName = "Filesystem"
    override val methods = listOf(
        "spaceUsed", "open", "seek", "makeDirectory", "exists",
        "isReadOnly", "write", "spaceTotal", "isDirectory", "rename",
        "list", "lastModified", "getLabel", "remove", "close",
        "size", "read", "setLabel"
    )
    override val capabilities = setOf(DeviceCapability.READ, DeviceCapability.WRITE, DeviceCapability.SEEK)
    
    var usedSpace = 0L
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "spaceTotal" -> capacity
        "spaceUsed" -> usedSpace
        "isReadOnly" -> isReadOnly
        "getLabel" -> label
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

class NetworkDevice(
    address: String,
    val isWired: Boolean = true
) : HardwareDevice(address, DeviceClass.NETWORK) {
    override val deviceName = if (isWired) "Network Card" else "Wireless Network Card"
    override val methods = listOf(
        "isWireless", "isWired", "send", "broadcast", "open", "close", "isOpen",
        "getStrength", "setStrength", "getWakeMessage", "setWakeMessage"
    )
    
    var strength = 400
    val openPorts = mutableSetOf<Int>()
    var wakeMessage = ""
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "isWireless" -> !isWired
        "isWired" -> isWired
        "getStrength" -> strength
        "setStrength" -> { strength = (args[0] as Number).toInt(); strength }
        "open" -> { openPorts.add((args[0] as Number).toInt()); true }
        "close" -> { openPorts.remove((args[0] as Number).toInt()); true }
        "isOpen" -> openPorts.contains((args[0] as Number).toInt())
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

class EepromDevice(
    address: String,
    val size: Int = 4096
) : HardwareDevice(address, DeviceClass.EEPROM) {
    override val deviceName = "EEPROM"
    override val methods = listOf(
        "get", "set", "getLabel", "setLabel", "getSize",
        "getChecksum", "makeReadonly", "getData", "setData", "getDataSize"
    )
    
    var label = "EEPROM"
    var data = ByteArray(size)
    var isReadonly = false
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "getLabel" -> label
        "setLabel" -> { if (!isReadonly) { label = args[0] as String; label } else null }
        "getSize" -> size
        "get" -> String(data)
        "set" -> { if (!isReadonly) { data = (args[0] as String).toByteArray(); true } else false }
        "makeReadonly" -> { isReadonly = true; true }
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

class RedstoneDevice(
    address: String
) : HardwareDevice(address, DeviceClass.REDSTONE) {
    override val deviceName = "Redstone I/O"
    override val methods = listOf(
        "getInput", "getOutput", "setOutput", "getBundledInput",
        "getBundledOutput", "setBundledOutput", "getComparatorInput",
        "getWirelessInput", "getWirelessOutput", "setWirelessOutput",
        "getWirelessFrequency", "setWirelessFrequency", "getWakeThreshold",
        "setWakeThreshold"
    )
    
    val inputs = IntArray(6)
    val outputs = IntArray(6)
    var wakeThreshold = 0
    
    override fun invoke(method: String, vararg args: Any?): Any? = when (method) {
        "getInput" -> inputs[(args[0] as Number).toInt()]
        "getOutput" -> outputs[(args[0] as Number).toInt()]
        "setOutput" -> {
            val side = (args[0] as Number).toInt()
            val value = (args[1] as Number).toInt()
            outputs[side] = value
            value
        }
        "getWakeThreshold" -> wakeThreshold
        "setWakeThreshold" -> { wakeThreshold = (args[0] as Number).toInt(); wakeThreshold }
        else -> null
    }
    
    override fun documentation(method: String): String? = null
}

// ============================================================
// Device Drivers
// ============================================================

class GpuDriver : DeviceDriver<GpuDevice> {
    override val supportedClass = DeviceClass.GPU
    override fun initialize(device: GpuDevice) {}
    override fun shutdown(device: GpuDevice) {}
    override fun read(device: GpuDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: GpuDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: GpuDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: GpuDevice, state: DevicePowerState) {}
}

class ScreenDriver : DeviceDriver<ScreenDevice> {
    override val supportedClass = DeviceClass.SCREEN
    override fun initialize(device: ScreenDevice) {}
    override fun shutdown(device: ScreenDevice) { device.isOn = false }
    override fun read(device: ScreenDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: ScreenDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: ScreenDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: ScreenDevice, state: DevicePowerState) {
        device.isOn = state == DevicePowerState.ACTIVE
    }
}

class KeyboardDriver : DeviceDriver<KeyboardDevice> {
    override val supportedClass = DeviceClass.KEYBOARD
    override fun initialize(device: KeyboardDevice) {}
    override fun shutdown(device: KeyboardDevice) {}
    override fun read(device: KeyboardDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: KeyboardDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: KeyboardDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: KeyboardDevice, state: DevicePowerState) {}
}

class FilesystemDriver : DeviceDriver<FilesystemDevice> {
    override val supportedClass = DeviceClass.FILESYSTEM
    override fun initialize(device: FilesystemDevice) {}
    override fun shutdown(device: FilesystemDevice) {}
    override fun read(device: FilesystemDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: FilesystemDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: FilesystemDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: FilesystemDevice, state: DevicePowerState) {}
}

class NetworkDriver : DeviceDriver<NetworkDevice> {
    override val supportedClass = DeviceClass.NETWORK
    override fun initialize(device: NetworkDevice) {}
    override fun shutdown(device: NetworkDevice) { device.openPorts.clear() }
    override fun read(device: NetworkDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: NetworkDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: NetworkDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: NetworkDevice, state: DevicePowerState) {}
}

class EepromDriver : DeviceDriver<EepromDevice> {
    override val supportedClass = DeviceClass.EEPROM
    override fun initialize(device: EepromDevice) {}
    override fun shutdown(device: EepromDevice) {}
    override fun read(device: EepromDevice, offset: Int, length: Int): ByteArray {
        return device.data.copyOfRange(offset.coerceAtLeast(0), (offset + length).coerceAtMost(device.size))
    }
    override fun write(device: EepromDevice, offset: Int, data: ByteArray): Boolean {
        if (device.isReadonly) return false
        data.copyInto(device.data, offset)
        return true
    }
    override fun invoke(device: EepromDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: EepromDevice, state: DevicePowerState) {}
}

class RedstoneDriver : DeviceDriver<RedstoneDevice> {
    override val supportedClass = DeviceClass.REDSTONE
    override fun initialize(device: RedstoneDevice) {}
    override fun shutdown(device: RedstoneDevice) {}
    override fun read(device: RedstoneDevice, offset: Int, length: Int): ByteArray? = null
    override fun write(device: RedstoneDevice, offset: Int, data: ByteArray): Boolean = false
    override fun invoke(device: RedstoneDevice, method: String, vararg args: Any?): Any? = device.invoke(method, *args)
    override fun setPower(device: RedstoneDevice, state: DevicePowerState) {}
}

// ============================================================
// Interrupt System
// ============================================================

data class Interrupt(
    val id: Long,
    val irq: Int,
    val timestamp: Long,
    val data: Any? = null
)

fun interface InterruptHandler {
    fun handle(interrupt: Interrupt)
}

// Common IRQ numbers
object IRQ {
    const val KEYBOARD = 1
    const val SCREEN_TOUCH = 2
    const val NETWORK = 3
    const val FILESYSTEM = 4
    const val REDSTONE = 5
    const val TIMER = 8
    const val COMPONENT_ADD = 10
    const val COMPONENT_REMOVE = 11
}

// ============================================================
// Power Management
// ============================================================

enum class PowerState {
    RUNNING,
    IDLE,
    SUSPENDED,
    HIBERNATING
}

enum class DevicePowerState {
    ACTIVE,
    IDLE,
    SUSPENDED,
    OFF
}

// ============================================================
// Hardware Events
// ============================================================

sealed class HardwareEvent {
    data class DeviceConnected(val device: HardwareDevice) : HardwareEvent()
    data class DeviceDisconnected(val device: HardwareDevice) : HardwareEvent()
    data class DevicePowerChanged(val device: HardwareDevice, val state: DevicePowerState) : HardwareEvent()
    data class PowerStateChanged(val state: PowerState) : HardwareEvent()
    data class InterruptRaised(val interrupt: Interrupt) : HardwareEvent()
}

// ============================================================
// Statistics
// ============================================================

data class HalStats(
    val version: String,
    val deviceCount: Int,
    val driverCount: Int,
    val interruptCount: Long,
    val powerState: PowerState
)
