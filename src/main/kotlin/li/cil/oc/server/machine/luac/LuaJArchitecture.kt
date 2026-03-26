package li.cil.oc.server.machine.luac

import li.cil.oc.api.machine.Architecture
import li.cil.oc.api.machine.ExecutionResult
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.Signal
import li.cil.oc.common.machine.MachineImpl
import li.cil.oc.server.component.ComponentRegistry
import li.cil.oc.util.scheduling.SchedulerSystem
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.*
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * LuaJ-based Lua 5.2/5.3/5.4 architecture implementation.
 * Provides a sandboxed Lua environment for running computer programs.
 */
class LuaJArchitecture(private var machine: Machine?) : Architecture {
    
    companion object {
        const val NAME = "Lua 5.4"
        
        // Memory limits per tier (in bytes, but Lua uses different units)
        val MEMORY_TIERS = mapOf(
            1 to 192 * 1024,  // Tier 1: 192 KB
            2 to 256 * 1024,  // Tier 2: 256 KB
            3 to 384 * 1024,  // Tier 3: 384 KB
            4 to 512 * 1024,  // Tier 3.5: 512 KB
            5 to 768 * 1024,  // Tier 4: 768 KB
            6 to 1024 * 1024  // Tier 5: 1 MB
        )
        
        // CPU call budgets per tier
        val CALL_BUDGETS = mapOf(
            1 to 0.5,   // Tier 1: slowest
            2 to 1.0,   // Tier 2: normal
            3 to 1.5    // Tier 3: fastest
        )
    }
    
    private var globals: Globals? = null
    private var mainThread: LuaThread? = null
    private var bootCode: ByteArray? = null
    private val signalQueue = ConcurrentLinkedQueue<Signal>()
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private var lastRunTime = AtomicLong(0)
    private var memoryLimit = 256 * 1024
    private var callBudget = 1.0
    
    override fun name(): String = NAME
    
    override fun isInitialized(): Boolean = globals != null
    
    override fun recomputeMemory(components: Iterable<Any>): Boolean {
        // Calculate total memory from RAM components
        var totalMemory = 0
        
        for (component in components) {
            // Check if it's a memory component and add its capacity
            if (component is MemoryProvider) {
                totalMemory += component.getMemorySize()
            }
        }
        
        memoryLimit = totalMemory.coerceAtLeast(192 * 1024) // Minimum 192 KB
        return true
    }
    
    override fun initialize(): Boolean {
        if (machine == null) return false
        
        try {
            // Create sandboxed Lua globals
            globals = createSandboxedGlobals()
            
            // Set up the component API
            setupComponentAPI()
            
            // Set up the computer API
            setupComputerAPI()
            
            // Set up the unicode API
            setupUnicodeAPI()
            
            // Load boot code (EEPROM)
            bootCode = machine?.popBootCode()
            if (bootCode == null || bootCode!!.isEmpty()) {
                bootCode = getDefaultBootCode()
            }
            
            return true
        } catch (e: Exception) {
            machine?.crash("Failed to initialize Lua architecture: ${e.message}")
            return false
        }
    }
    
    private fun createSandboxedGlobals(): Globals {
        val globals = Globals()
        
        // Load safe standard libraries
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(StringLib())
        globals.load(CoroutineLib())
        globals.load(JseMathLib())
        globals.load(JseOsLib())
        
        // Install compiler
        LuaC.install(globals)
        
        // Remove unsafe functions
        removeDangerous(globals)
        
        // Add custom print function
        globals.set("print", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    if (i > 1) sb.append("\t")
                    sb.append(args.arg(i).tojstring())
                }
                machine?.signal("print", sb.toString())
                return LuaValue.NIL
            }
        })
        
        // Add checkArg function
        globals.set("checkArg", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val n = args.checkint(1)
                val have = args.arg(2).typename()
                
                for (i in 3..args.narg()) {
                    val want = args.checkjstring(i)
                    if (have == want || (want == "nil" && args.arg(2).isnil())) {
                        return LuaValue.NIL
                    }
                }
                
                val expected = (3..args.narg()).map { args.checkjstring(it) }.joinToString(" or ")
                throw LuaError("bad argument #$n ($expected expected, got $have)")
            }
        })
        
        return globals
    }
    
    private fun removeDangerous(globals: Globals) {
        // Remove dangerous functions
        globals.set("collectgarbage", LuaValue.NIL)
        globals.set("dofile", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("module", LuaValue.NIL)
        globals.set("require", LuaValue.NIL) // We'll add our own
        globals.set("rawequal", LuaValue.NIL)
        globals.set("rawget", LuaValue.NIL)
        globals.set("rawlen", LuaValue.NIL)
        globals.set("rawset", LuaValue.NIL)
        globals.set("getfenv", LuaValue.NIL)
        globals.set("setfenv", LuaValue.NIL)
        
        // Remove debug library entirely
        globals.set("debug", LuaValue.NIL)
        
        // Sandbox os library
        val os = globals.get("os")
        if (os.istable()) {
            val osTable = os.checktable()
            val safeOs = LuaTable()
            
            // Only keep safe os functions
            listOf("clock", "date", "difftime", "time").forEach { fname ->
                val f = osTable.get(fname)
                if (!f.isnil()) safeOs.set(fname, f)
            }
            
            globals.set("os", safeOs)
        }
        
        // Sandbox string library (remove dump)
        val string = globals.get("string")
        if (string.istable()) {
            string.checktable().set("dump", LuaValue.NIL)
        }
    }
    
    private fun setupComponentAPI() {
        val g = globals ?: return
        val componentTable = LuaTable()
        
        // component.list(filter?: string, exact?: boolean): iterator
        componentTable.set("list", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val filter = if (args.narg() >= 1 && !args.arg(1).isnil()) args.checkjstring(1) else null
                val exact = args.optboolean(2, false)
                
                val components = machine?.components() ?: emptyMap()
                val filtered = components.filter { (_, type) ->
                    when {
                        filter == null -> true
                        exact -> type == filter
                        else -> type.contains(filter)
                    }
                }
                
                // Return as iterator
                val iter = filtered.iterator()
                return LuaValue.varargsOf(arrayOf(
                    object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            return if (iter.hasNext()) {
                                val (address, type) = iter.next()
                                LuaValue.varargsOf(LuaValue.valueOf(address), LuaValue.valueOf(type))
                            } else {
                                LuaValue.NIL
                            }
                        }
                    }
                ))
            }
        })
        
        // component.invoke(address: string, method: string, ...): ...
        componentTable.set("invoke", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val method = args.checkjstring(2)
                
                // Collect remaining arguments
                val luaArgs = mutableListOf<Any?>()
                for (i in 3..args.narg()) {
                    luaArgs.add(toLuaValue(args.arg(i)))
                }
                
                return try {
                    val result = machine?.invoke(address, method, luaArgs.toTypedArray())
                    resultToVarargs(result)
                } catch (e: Exception) {
                    LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "unknown error"))
                }
            }
        })
        
        // component.methods(address: string): table
        componentTable.set("methods", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val methods = machine?.methods(address) ?: return LuaValue.NIL
                
                val result = LuaTable()
                for ((name, info) in methods) {
                    val infoTable = LuaTable()
                    infoTable.set("direct", LuaValue.valueOf(info.isDirect))
                    infoTable.set("getter", LuaValue.valueOf(info.isGetter))
                    infoTable.set("setter", LuaValue.valueOf(info.isSetter))
                    result.set(name, infoTable)
                }
                return result
            }
        })
        
        // component.proxy(address: string): table
        componentTable.set("proxy", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val type = machine?.components()?.get(address) ?: return LuaValue.varargsOf(
                    LuaValue.NIL, 
                    LuaValue.valueOf("no such component")
                )
                
                val methods = machine?.methods(address) ?: return LuaValue.varargsOf(
                    LuaValue.NIL,
                    LuaValue.valueOf("failed to get methods")
                )
                
                val proxy = LuaTable()
                proxy.set("address", LuaValue.valueOf(address))
                proxy.set("type", LuaValue.valueOf(type))
                
                // Add method wrappers
                for ((methodName, info) in methods) {
                    proxy.set(methodName, object : VarArgFunction() {
                        override fun invoke(args: Varargs): Varargs {
                            val luaArgs = mutableListOf<Any?>()
                            for (i in 1..args.narg()) {
                                luaArgs.add(toLuaValue(args.arg(i)))
                            }
                            
                            return try {
                                val result = machine?.invoke(address, methodName, luaArgs.toTypedArray())
                                resultToVarargs(result)
                            } catch (e: Exception) {
                                throw LuaError(e.message ?: "component invocation failed")
                            }
                        }
                    })
                }
                
                return proxy
            }
        })
        
        // component.type(address: string): string?
        componentTable.set("type", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val type = machine?.components()?.get(address)
                return if (type != null) LuaValue.valueOf(type) else LuaValue.NIL
            }
        })
        
        // component.slot(address: string): number
        componentTable.set("slot", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                // Return slot number (would need component registry lookup)
                return LuaValue.valueOf(-1) // TODO: implement slot lookup
            }
        })
        
        // component.get(address: string, type?: string): string?
        componentTable.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val partialAddress = args.checkjstring(1)
                val componentType = if (args.narg() >= 2 && !args.arg(2).isnil()) args.checkjstring(2) else null
                
                val components = machine?.components() ?: return LuaValue.NIL
                
                for ((address, type) in components) {
                    if (address.startsWith(partialAddress)) {
                        if (componentType == null || type == componentType) {
                            return LuaValue.valueOf(address)
                        }
                    }
                }
                
                return LuaValue.NIL
            }
        })

        // component.isAvailable(type: string): boolean
        componentTable.set("isAvailable", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val componentType = args.checkjstring(1)
                val components = machine?.components() ?: return LuaValue.FALSE
                
                return LuaValue.valueOf(components.values.any { it == componentType })
            }
        })
        
        // component.getPrimary(type: string): string?
        componentTable.set("getPrimary", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val componentType = args.checkjstring(1)
                val components = machine?.components() ?: return LuaValue.NIL
                
                val address = components.entries.firstOrNull { it.value == componentType }?.key
                return if (address != null) LuaValue.valueOf(address) else LuaValue.NIL
            }
        })
        
        g.set("component", componentTable)
    }
    
    private fun setupComputerAPI() {
        val g = globals ?: return
        val computerTable = LuaTable()
        
        // computer.address(): string
        computerTable.set("address", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine?.node()?.address() ?: "unknown")
            }
        })
        
        // computer.tmpAddress(): string?
        computerTable.set("tmpAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return machine?.tmpAddress()?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
            }
        })
        
        // computer.freeMemory(): number
        computerTable.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val usedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024
                return LuaValue.valueOf((memoryLimit - usedMemory).coerceAtLeast(0).toDouble())
            }
        })
        
        // computer.totalMemory(): number
        computerTable.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(memoryLimit.toDouble())
            }
        })
        
        // computer.uptime(): number
        computerTable.set("uptime", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf((machine?.upTime() ?: 0.0))
            }
        })
        
        // computer.energy(): number
        computerTable.set("energy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine?.host()?.energyStored() ?: 0.0)
            }
        })
        
        // computer.maxEnergy(): number
        computerTable.set("maxEnergy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine?.host()?.energyCapacity() ?: 0.0)
            }
        })
        
        // computer.users(): table
        computerTable.set("users", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val users = machine?.users() ?: emptyArray()
                val result = LuaTable()
                users.forEachIndexed { index, user ->
                    result.set(index + 1, LuaValue.valueOf(user))
                }
                return result
            }
        })
        
        // computer.addUser(username: string): boolean, string?
        computerTable.set("addUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val username = arg.checkjstring()
                return try {
                    machine?.addUser(username)
                    LuaValue.TRUE
                } catch (e: Exception) {
                    LuaValue.varargsOf(LuaValue.FALSE, LuaValue.valueOf(e.message ?: "unknown error"))
                }
            }
        })
        
        // computer.removeUser(username: string): boolean
        computerTable.set("removeUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val username = arg.checkjstring()
                return LuaValue.valueOf(machine?.removeUser(username) ?: false)
            }
        })
        
        // computer.shutdown(reboot?: boolean)  
        computerTable.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.optboolean(1, false)
                if (reboot) {
                    machine?.reboot()
                } else {
                    machine?.stop()
                }
                return LuaValue.NIL
            }
        })
        
        // computer.pushSignal(name: string, ...)
        computerTable.set("pushSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val name = args.checkjstring(1)
                val signalArgs = mutableListOf<Any?>()
                for (i in 2..args.narg()) {
                    signalArgs.add(toLuaValue(args.arg(i)))
                }
                machine?.signal(name, *signalArgs.toTypedArray())
                return LuaValue.TRUE
            }
        })
        
        // computer.pullSignal(timeout?: number): string?, ...
        computerTable.set("pullSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val timeout = args.optdouble(1, Double.MAX_VALUE)
                
                // This should yield and resume when a signal arrives
                // For now, just check the queue
                val signal = signalQueue.poll()
                return if (signal != null) {
                    val values = mutableListOf<LuaValue>()
                    values.add(LuaValue.valueOf(signal.name()))
                    signal.args().forEach { arg ->
                        values.add(javaToLua(arg))
                    }
                    LuaValue.varargsOf(values.toTypedArray())
                } else {
                    LuaValue.NIL
                }
            }
        })
        
        // computer.beep(frequency?: number, duration?: number)
        computerTable.set("beep", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val frequency = args.optdouble(1, 440.0)
                val duration = args.optdouble(2, 0.1)
                machine?.host()?.beep(frequency.toFloat(), duration.toFloat())
                return LuaValue.NIL
            }
        })
        
        // computer.getBootAddress(): string?
        computerTable.set("getBootAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return machine?.bootAddress()?.let { LuaValue.valueOf(it) } ?: LuaValue.NIL
            }
        })
        
        // computer.setBootAddress(address: string?)
        computerTable.set("setBootAddress", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = if (args.narg() >= 1 && !args.arg(1).isnil()) args.checkjstring(1) else null
                machine?.setBootAddress(address)
                return LuaValue.NIL
            }
        })
        
        // computer.getArchitecture(): string
        computerTable.set("getArchitecture", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(name())
            }
        })
        
        // computer.getDeviceInfo(): table
        computerTable.set("getDeviceInfo", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val info = machine?.deviceInfo() ?: return LuaTable()
                val result = LuaTable()
                
                for ((address, deviceInfo) in info) {
                    val deviceTable = LuaTable()
                    deviceInfo.forEach { (key, value) ->
                        deviceTable.set(key, LuaValue.valueOf(value))
                    }
                    result.set(address, deviceTable)
                }
                
                return result
            }
        })
        
        g.set("computer", computerTable)
    }
    
    private fun setupUnicodeAPI() {
        val g = globals ?: return
        val unicodeTable = LuaTable()
        
        // unicode.char(value: number...): string
        unicodeTable.set("char", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    val codePoint = args.checkint(i)
                    if (Character.isValidCodePoint(codePoint)) {
                        sb.appendCodePoint(codePoint)
                    }
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        // unicode.charWidth(char: string): number
        unicodeTable.set("charWidth", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val str = arg.checkjstring()
                if (str.isEmpty()) return LuaValue.valueOf(0)
                
                val codePoint = str.codePointAt(0)
                val width = getCharWidth(codePoint)
                return LuaValue.valueOf(width)
            }
        })
        
        // unicode.isWide(char: string): boolean
        unicodeTable.set("isWide", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val str = arg.checkjstring()
                if (str.isEmpty()) return LuaValue.FALSE
                
                val codePoint = str.codePointAt(0)
                return LuaValue.valueOf(getCharWidth(codePoint) == 2)
            }
        })
        
        // unicode.len(str: string): number
        unicodeTable.set("len", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val str = arg.checkjstring()
                return LuaValue.valueOf(str.codePointCount(0, str.length))
            }
        })
        
        // unicode.lower(str: string): string
        unicodeTable.set("lower", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().lowercase())
            }
        })
        
        // unicode.upper(str: string): string
        unicodeTable.set("upper", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().uppercase())
            }
        })
        
        // unicode.reverse(str: string): string
        unicodeTable.set("reverse", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val str = arg.checkjstring()
                val codePoints = str.codePoints().toArray()
                val sb = StringBuilder()
                for (i in codePoints.indices.reversed()) {
                    sb.appendCodePoint(codePoints[i])
                }
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        // unicode.sub(str: string, i: number, j?: number): string
        unicodeTable.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val str = args.checkjstring(1)
                val len = str.codePointCount(0, str.length)
                
                var i = args.checkint(2)
                var j = args.optint(3, len)
                
                // Convert Lua 1-based indices to 0-based
                if (i < 0) i = len + i + 1
                if (j < 0) j = len + j + 1
                
                i = i.coerceIn(1, len + 1)
                j = j.coerceIn(0, len)
                
                if (i > j) return LuaValue.valueOf("")
                
                // Get code point offsets
                val startOffset = str.offsetByCodePoints(0, i - 1)
                val endOffset = str.offsetByCodePoints(0, j)
                
                return LuaValue.valueOf(str.substring(startOffset, endOffset))
            }
        })
        
        // unicode.wlen(str: string): number
        unicodeTable.set("wlen", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val str = arg.checkjstring()
                var width = 0
                str.codePoints().forEach { cp ->
                    width += getCharWidth(cp)
                }
                return LuaValue.valueOf(width)
            }
        })
        
        // unicode.wtrunc(str: string, width: number): string
        unicodeTable.set("wtrunc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val str = arg1.checkjstring()
                val maxWidth = arg2.checkint()
                
                val sb = StringBuilder()
                var currentWidth = 0
                
                str.codePoints().forEach { cp ->
                    val charWidth = getCharWidth(cp)
                    if (currentWidth + charWidth <= maxWidth) {
                        sb.appendCodePoint(cp)
                        currentWidth += charWidth
                    }
                }
                
                return LuaValue.valueOf(sb.toString())
            }
        })
        
        g.set("unicode", unicodeTable)
    }
    
    private fun getCharWidth(codePoint: Int): Int {
        // Simple width detection - full width characters
        return when {
            codePoint in 0x1100..0x115F -> 2 // Hangul Jamo
            codePoint in 0x2E80..0x9FFF -> 2 // CJK
            codePoint in 0xAC00..0xD7A3 -> 2 // Hangul Syllables
            codePoint in 0xF900..0xFAFF -> 2 // CJK Compatibility
            codePoint in 0xFE10..0xFE1F -> 2 // Vertical forms
            codePoint in 0xFE30..0xFE6F -> 2 // CJK Compatibility Forms
            codePoint in 0xFF00..0xFF60 -> 2 // Fullwidth Forms
            codePoint in 0xFFE0..0xFFE6 -> 2 // Fullwidth Forms
            codePoint in 0x20000..0x2FFFD -> 2 // CJK Extension B-F
            codePoint in 0x30000..0x3FFFD -> 2 // CJK Extension G+
            else -> 1
        }
    }
    
    override fun runSynchronized(): ExecutionResult {
        if (machine == null || globals == null) {
            return ExecutionResult.Error("Machine not initialized")
        }
        
        return try {
            // If there's no main thread, compile and start boot code
            if (mainThread == null) {
                val code = bootCode ?: return ExecutionResult.Error("No boot code")
                val chunk = globals!!.load(ByteArrayInputStream(code), "=bios.lua", "bt", globals)
                mainThread = LuaThread(globals!!, chunk)
            }
            
            // Resume the coroutine
            val thread = mainThread!!
            if (thread.state.status == LuaThread.STATUS_DEAD) {
                return ExecutionResult.Shutdown(false)
            }
            
            // Pop any pending signals
            val signal = signalQueue.poll()
            val args = if (signal != null) {
                val values = mutableListOf<LuaValue>()
                values.add(LuaValue.valueOf(signal.name()))
                signal.args().forEach { arg ->
                    values.add(javaToLua(arg))
                }
                LuaValue.varargsOf(values.toTypedArray())
            } else {
                LuaValue.NIL
            }
            
            val result = thread.resume(args)
            
            when {
                thread.state.status == LuaThread.STATUS_DEAD -> {
                    if (result.arg1().isboolean() && !result.arg1().toboolean()) {
                        ExecutionResult.Error(result.arg(2).tojstring())
                    } else {
                        ExecutionResult.Shutdown(false)
                    }
                }
                else -> {
                    // Yielded - check if there's a sleep request
                    val sleepTime = if (result.narg() > 1) result.arg(2).optdouble(0.05) else 0.05
                    ExecutionResult.Sleep((sleepTime * 20).toInt().coerceAtLeast(1))
                }
            }
        } catch (e: LuaError) {
            ExecutionResult.Error(e.message ?: "Lua error")
        } catch (e: Exception) {
            ExecutionResult.Error(e.message ?: "Unknown error")
        }
    }
    
    override fun runThreaded(isSynchronizedReturn: Boolean): ExecutionResult {
        return runSynchronized()
    }
    
    override fun onSignal(): Unit {
        // Signals are queued and handled in runSynchronized
    }
    
    override fun onConnect(): Unit {
        // Component connected
    }
    
    override fun onDisconnect(): Unit {
        // Component disconnected
    }
    
    override fun load(nbt: net.minecraft.nbt.CompoundTag): Unit {
        // Restore state from NBT
        if (nbt.contains("bootCode")) {
            bootCode = nbt.getByteArray("bootCode")
        }
    }
    
    override fun save(nbt: net.minecraft.nbt.CompoundTag): Unit {
        // Save state to NBT
        bootCode?.let { nbt.putByteArray("bootCode", it) }
    }
    
    override fun close(): Unit {
        mainThread = null
        globals = null
        machine = null
    }
    
    // Helper methods
    
    private fun toLuaValue(value: LuaValue): Any? {
        return when {
            value.isnil() -> null
            value.isboolean() -> value.toboolean()
            value.isnumber() -> value.todouble()
            value.isstring() -> value.tojstring()
            value.istable() -> {
                val table = value.checktable()
                val map = mutableMapOf<Any?, Any?>()
                var k = LuaValue.NIL
                while (true) {
                    val n = table.next(k)
                    if (n.arg1().isnil()) break
                    k = n.arg1()
                    map[toLuaValue(k)] = toLuaValue(n.arg(2))
                }
                map
            }
            else -> value.tojstring()
        }
    }
    
    private fun javaToLua(value: Any?): LuaValue {
        return when (value) {
            null -> LuaValue.NIL
            is Boolean -> LuaValue.valueOf(value)
            is Number -> LuaValue.valueOf(value.toDouble())
            is String -> LuaValue.valueOf(value)
            is ByteArray -> LuaValue.valueOf(value.decodeToString())
            is Array<*> -> {
                val table = LuaTable()
                value.forEachIndexed { index, item ->
                    table.set(index + 1, javaToLua(item))
                }
                table
            }
            is Map<*, *> -> {
                val table = LuaTable()
                for ((k, v) in value) {
                    table.set(javaToLua(k), javaToLua(v))
                }
                table
            }
            is Iterable<*> -> {
                val table = LuaTable()
                value.forEachIndexed { index, item ->
                    table.set(index + 1, javaToLua(item))
                }
                table
            }
            else -> LuaValue.valueOf(value.toString())
        }
    }
    
    private fun resultToVarargs(result: Array<Any?>?): Varargs {
        if (result == null || result.isEmpty()) return LuaValue.NIL
        
        val values = result.map { javaToLua(it) }.toTypedArray()
        return LuaValue.varargsOf(values)
    }
    
    private fun getDefaultBootCode(): ByteArray {
        return """
            -- Default BIOS
            local screen = component.list("screen")()
            local gpu = component.list("gpu")()
            
            if gpu and screen then
                component.invoke(gpu, "bind", screen)
                component.invoke(gpu, "setResolution", 50, 16)
                component.invoke(gpu, "fill", 1, 1, 50, 16, " ")
                component.invoke(gpu, "set", 1, 1, "No bootable medium found.")
                component.invoke(gpu, "set", 1, 2, "Press any key to reboot...")
            end
            
            while true do
                local signal = computer.pullSignal()
                if signal == "key_down" then
                    computer.shutdown(true)
                end
            end
        """.trimIndent().toByteArray()
    }
}

/**
 * Interface for memory-providing components
 */
interface MemoryProvider {
    fun getMemorySize(): Int
}
