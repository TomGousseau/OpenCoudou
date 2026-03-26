package li.cil.oc.server.machine

import li.cil.oc.OpenComputers
import li.cil.oc.Settings
import li.cil.oc.api.machine.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.item.ItemStack
import org.luaj.vm2.*
import org.luaj.vm2.compiler.LuaC
import org.luaj.vm2.lib.*
import org.luaj.vm2.lib.jse.JseBaseLib
import org.luaj.vm2.lib.jse.JseMathLib
import org.luaj.vm2.lib.jse.JseOsLib
import org.luaj.vm2.lib.jse.JseStringLib
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream

/**
 * Lua architecture implementation using LuaJ.
 * 
 * This provides a pure-Java Lua 5.2/5.3 compatible runtime for computers.
 * While not as fast as native Lua (JNLua), it works on all platforms
 * without requiring native libraries.
 * 
 * The architecture provides:
 * - Sandboxed Lua environment
 * - Component API for accessing OC components
 * - Computer API for machine management
 * - Unicode API for string manipulation
 * - Memory limiting based on installed RAM
 */
class LuaJArchitecture(override val machine: Machine) : Architecture {
    
    private var globals: Globals? = null
    private var mainThread: LuaThread? = null
    private var memoryLimit: Int = 0
    private var _isInitialized = false
    private var synchronizedResult: Varargs? = null
    private var pendingSignal: MachineImpl.Signal? = null
    
    override val isInitialized: Boolean
        get() = _isInitialized
    
    // ========================================
    // Lifecycle
    // ========================================
    
    override fun initialize(): Boolean {
        try {
            // Create sandboxed globals
            globals = createSandboxedGlobals()
            
            // Install OC APIs
            installComponentAPI()
            installComputerAPI()
            installUnicodeAPI()
            installSystemAPI()
            
            // Load boot code
            val bootCode = getBootCode()
            if (bootCode != null) {
                val chunk = globals!!.load(bootCode, "=machine", "t", globals!!)
                mainThread = LuaThread(globals!!, chunk)
            } else {
                // No boot code - create a minimal REPL
                val repl = """
                    local component = component
                    local computer = computer
                    print("OpenComputers " .. _VERSION)
                    print("No boot device found. Type Lua code directly.")
                    while true do
                        io.write("> ")
                        local line = io.read()
                        if line then
                            local fn, err = load(line)
                            if fn then
                                local ok, result = pcall(fn)
                                if ok then
                                    if result ~= nil then print(result) end
                                else
                                    print("Error: " .. tostring(result))
                                end
                            else
                                print("Syntax error: " .. tostring(err))
                            end
                        end
                        coroutine.yield()
                    end
                """.trimIndent()
                val chunk = globals!!.load(repl, "=repl", "t", globals!!)
                mainThread = LuaThread(globals!!, chunk)
            }
            
            _isInitialized = true
            OpenComputers.LOGGER.debug("Lua architecture initialized")
            return true
        } catch (e: Exception) {
            OpenComputers.LOGGER.error("Failed to initialize Lua architecture", e)
            return false
        }
    }
    
    override fun close() {
        globals = null
        mainThread = null
        _isInitialized = false
    }
    
    override fun recomputeMemory(memory: Iterable<ItemStack>): Boolean {
        memoryLimit = 0
        for (stack in memory) {
            // Calculate memory from item (simplified - would check item components)
            val tier = getTierFromStack(stack)
            memoryLimit += Settings.MemorySizes.forTier(tier)
        }
        return memoryLimit > 0
    }
    
    private fun getTierFromStack(stack: ItemStack): Int {
        // Simplified - a full implementation would check item data components
        return 0
    }
    
    // ========================================
    // Execution
    // ========================================
    
    override fun runThreaded(isSynchronizedReturn: Boolean): ExecutionResult {
        val thread = mainThread ?: return ExecutionResult.Error("No main thread")
        val g = globals ?: return ExecutionResult.Error("No globals")
        
        try {
            // Check for pending signal
            val signal = if (isSynchronizedReturn) {
                synchronizedResult?.let { result ->
                    synchronizedResult = null
                    return resumeWithResult(thread, result)
                }
                null
            } else {
                (machine as? MachineImpl)?.popSignal()
            }
            
            // Resume the main thread
            val args = if (signal != null) {
                LuaValue.varargsOf(arrayOf(
                    LuaValue.valueOf(signal.name),
                    *signal.args.map { toLuaValue(it) }.toTypedArray()
                ))
            } else {
                LuaValue.NONE
            }
            
            return resumeWithResult(thread, args)
        } catch (e: LuaError) {
            return ExecutionResult.Error(e.message ?: "Lua error")
        } catch (e: Exception) {
            OpenComputers.LOGGER.error("Error in Lua execution", e)
            return ExecutionResult.Error(e.message ?: "Unknown error")
        }
    }
    
    private fun resumeWithResult(thread: LuaThread, args: Varargs): ExecutionResult {
        val result = thread.resume(args)
        
        return when {
            // Thread returned normally
            result.arg1().toboolean() -> {
                // Check what the thread yielded
                val yieldValue = result.arg(2)
                when {
                    yieldValue.isnil() -> ExecutionResult.Sleep(1)
                    yieldValue.isnumber() -> ExecutionResult.Sleep(yieldValue.toint())
                    yieldValue.isstring() && yieldValue.tojstring() == "sync" -> {
                        ExecutionResult.SynchronizedCall
                    }
                    else -> ExecutionResult.Sleep(1)
                }
            }
            // Thread errored
            else -> {
                val error = result.arg(2).tojstring()
                if (error.contains("shutdown") || error.contains("reboot")) {
                    ExecutionResult.Shutdown
                } else {
                    ExecutionResult.Error(error)
                }
            }
        }
    }
    
    override fun runSynchronized() {
        // Execute any pending synchronized call
        // Results will be stored in synchronizedResult for the next runThreaded
    }
    
    override fun onSignal() {
        // Wake up sleeping thread
    }
    
    // ========================================
    // Sandboxing
    // ========================================
    
    private fun createSandboxedGlobals(): Globals {
        val globals = Globals()
        
        // Load safe standard libraries
        globals.load(JseBaseLib())
        globals.load(PackageLib())
        globals.load(Bit32Lib())
        globals.load(TableLib())
        globals.load(JseStringLib())
        globals.load(JseMathLib())
        globals.load(CoroutineLib())
        
        // Remove or restrict dangerous functions
        globals.set("dofile", LuaValue.NIL)
        globals.set("loadfile", LuaValue.NIL)
        globals.set("load", createSafeLoad(globals))
        globals.set("print", createPrintFunction())
        
        // Add OC version info
        globals.set("_OSVERSION", "OpenComputers 3.0")
        globals.set("_VERSION", "Lua 5.4 (LuaJ)")
        
        // Set up stdout redirection
        val outputStream = ByteArrayOutputStream()
        globals.STDOUT = PrintStream(outputStream)
        
        // Install compiler
        LuaC.install(globals)
        
        return globals
    }
    
    private fun createSafeLoad(globals: Globals): LuaFunction {
        return object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val source = args.checkstring(1)
                val chunkName = args.optjstring(2, "=chunk")
                val mode = args.optjstring(3, "t")
                
                // Only allow text mode (no bytecode)
                if (mode != "t" && mode != "bt") {
                    return LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf("binary chunks disabled"))
                }
                
                return try {
                    val chunk = globals.load(source.tojstring(), chunkName, globals)
                    LuaValue.varargsOf(chunk)
                } catch (e: LuaError) {
                    LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message))
                }
            }
        }
    }
    
    private fun createPrintFunction(): LuaFunction {
        return object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val sb = StringBuilder()
                for (i in 1..args.narg()) {
                    if (i > 1) sb.append("\t")
                    sb.append(args.arg(i).tojstring())
                }
                // In a full implementation, this would write to a terminal/screen
                OpenComputers.LOGGER.info("[OC] {}", sb.toString())
                return LuaValue.NONE
            }
        }
    }
    
    // ========================================
    // OC APIs
    // ========================================
    
    private fun installComponentAPI() {
        val g = globals ?: return
        
        val componentLib = LuaTable()
        
        // component.list([filter:string[, exact:boolean]]):table
        componentLib.set("list", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val filter = args.optjstring(1, null)
                val exact = args.optboolean(2, false)
                
                val result = LuaTable()
                var index = 1
                for ((address, name) in machine.components) {
                    val matches = when {
                        filter == null -> true
                        exact -> name == filter
                        else -> name.contains(filter)
                    }
                    if (matches) {
                        val entry = LuaTable()
                        entry.set(1, LuaValue.valueOf(address))
                        entry.set(2, LuaValue.valueOf(name))
                        result.set(index++, entry)
                    }
                }
                return result
            }
        })
        
        // component.type(address:string):string
        componentLib.set("type", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val address = arg.checkjstring()
                return LuaValue.valueOf(machine.components[address] ?: return LuaValue.NIL)
            }
        })
        
        // component.invoke(address:string, method:string, ...):...
        componentLib.set("invoke", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val address = args.checkjstring(1)
                val method = args.checkjstring(2)
                
                // Convert remaining args
                val methodArgs = (3..args.narg()).map { fromLuaValue(args.arg(it)) }.toTypedArray()
                
                return try {
                    val result = machine.invoke(address, method, *methodArgs)
                    LuaValue.varargsOf(result.map { toLuaValue(it) }.toTypedArray())
                } catch (e: Exception) {
                    LuaValue.varargsOf(LuaValue.NIL, LuaValue.valueOf(e.message ?: "error"))
                }
            }
        })
        
        // component.proxy(address:string):table
        componentLib.set("proxy", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val address = arg.checkjstring()
                val component = machine.component(address) ?: return LuaValue.NIL
                
                val proxy = LuaTable()
                proxy.set("address", LuaValue.valueOf(address))
                proxy.set("type", LuaValue.valueOf(component.name))
                
                // Add methods
                for ((name, method) in component.methods) {
                    proxy.set(name, createProxyMethod(address, name, method.doc))
                }
                
                return proxy
            }
        })
        
        // component.doc(address:string, method:string):string
        componentLib.set("doc", object : TwoArgFunction() {
            override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue {
                val address = arg1.checkjstring()
                val method = arg2.checkjstring()
                val component = machine.component(address) ?: return LuaValue.NIL
                val m = component.methods[method] ?: return LuaValue.NIL
                return LuaValue.valueOf(m.doc)
            }
        })
        
        // component.methods(address:string):table
        componentLib.set("methods", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                val address = arg.checkjstring()
                val component = machine.component(address) ?: return LuaValue.NIL
                
                val result = LuaTable()
                for ((name, method) in component.methods) {
                    val info = LuaTable()
                    info.set("direct", LuaValue.valueOf(method.isDirect))
                    result.set(name, info)
                }
                return result
            }
        })
        
        g.set("component", componentLib)
    }
    
    private fun createProxyMethod(address: String, method: String, doc: String): LuaFunction {
        return object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val methodArgs = (1..args.narg()).map { fromLuaValue(args.arg(it)) }.toTypedArray()
                return try {
                    val result = machine.invoke(address, method, *methodArgs)
                    LuaValue.varargsOf(result.map { toLuaValue(it) }.toTypedArray())
                } catch (e: Exception) {
                    error(e.message ?: "error")
                }
            }
        }
    }
    
    private fun installComputerAPI() {
        val g = globals ?: return
        
        val computerLib = LuaTable()
        
        // computer.address():string
        computerLib.set("address", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine.node().address)
            }
        })
        
        // computer.tmpAddress():string
        computerLib.set("tmpAddress", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Return address of temp filesystem (simplified)
                return LuaValue.NIL
            }
        })
        
        // computer.freeMemory():number
        computerLib.set("freeMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(memoryLimit - getCurrentMemoryUsage())
            }
        })
        
        // computer.totalMemory():number
        computerLib.set("totalMemory", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(memoryLimit)
            }
        })
        
        // computer.uptime():number
        computerLib.set("uptime", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine.uptime)
            }
        })
        
        // computer.energy():number
        computerLib.set("energy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                // Simplified - would check actual energy
                return LuaValue.valueOf(10000.0)
            }
        })
        
        // computer.maxEnergy():number
        computerLib.set("maxEnergy", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(10000.0)
            }
        })
        
        // computer.users():table
        computerLib.set("users", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                val result = LuaTable()
                machine.users.forEachIndexed { index, user ->
                    result.set(index + 1, LuaValue.valueOf(user))
                }
                return result
            }
        })
        
        // computer.addUser(name:string):boolean
        computerLib.set("addUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(machine.addUser(arg.checkjstring()))
            }
        })
        
        // computer.removeUser(name:string):boolean
        computerLib.set("removeUser", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(machine.removeUser(arg.checkjstring()))
            }
        })
        
        // computer.pushSignal(name:string, ...):boolean
        computerLib.set("pushSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val name = args.checkjstring(1)
                val signalArgs = (2..args.narg()).map { fromLuaValue(args.arg(it)) }.toTypedArray()
                return LuaValue.valueOf(machine.signal(name, *signalArgs))
            }
        })
        
        // computer.pullSignal([timeout:number]):...
        computerLib.set("pullSignal", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val timeout = args.optdouble(1, Double.MAX_VALUE)
                
                // Yield to wait for signal
                val ticks = if (timeout == Double.MAX_VALUE) Int.MAX_VALUE else (timeout * 20).toInt()
                
                // Check for pending signal
                val signal = (machine as? MachineImpl)?.popSignal()
                return if (signal != null) {
                    LuaValue.varargsOf(arrayOf(
                        LuaValue.valueOf(signal.name),
                        *signal.args.map { toLuaValue(it) }.toTypedArray()
                    ))
                } else {
                    // Yield and wait
                    LuaThread.yield(LuaValue.valueOf(ticks))
                }
            }
        })
        
        // computer.beep([frequency:number[, duration:number]])
        computerLib.set("beep", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                // Play beep sound (simplified)
                return LuaValue.NONE
            }
        })
        
        // computer.shutdown([reboot:boolean])
        computerLib.set("shutdown", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val reboot = args.optboolean(1, false)
                if (reboot) {
                    machine.stop()
                    machine.start()
                } else {
                    machine.stop()
                }
                error(if (reboot) "reboot" else "shutdown")
            }
        })
        
        g.set("computer", computerLib)
    }
    
    private fun installUnicodeAPI() {
        val g = globals ?: return
        
        val unicodeLib = LuaTable()
        
        // unicode.len(s:string):number
        unicodeLib.set("len", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().length)
            }
        })
        
        // unicode.sub(s:string, i:number[, j:number]):string
        unicodeLib.set("sub", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val s = args.checkjstring(1)
                val i = args.checkint(2)
                val j = args.optint(3, s.length)
                
                val start = if (i < 0) maxOf(0, s.length + i) else minOf(i - 1, s.length)
                val end = if (j < 0) maxOf(0, s.length + j + 1) else minOf(j, s.length)
                
                return LuaValue.valueOf(s.substring(start, end))
            }
        })
        
        // unicode.char(n:number):string
        unicodeLib.set("char", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(Char(arg.checkint()).toString())
            }
        })
        
        // unicode.lower(s:string):string
        unicodeLib.set("lower", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().lowercase())
            }
        })
        
        // unicode.upper(s:string):string
        unicodeLib.set("upper", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().uppercase())
            }
        })
        
        // unicode.reverse(s:string):string
        unicodeLib.set("reverse", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                return LuaValue.valueOf(arg.checkjstring().reversed())
            }
        })
        
        // unicode.wlen(s:string):number
        unicodeLib.set("wlen", object : OneArgFunction() {
            override fun call(arg: LuaValue): LuaValue {
                // Width calculation for terminal display
                var width = 0
                for (char in arg.checkjstring()) {
                    width += when {
                        char.code < 0x20 -> 0
                        Character.isIdeographic(char.code) -> 2
                        else -> 1
                    }
                }
                return LuaValue.valueOf(width)
            }
        })
        
        g.set("unicode", unicodeLib)
    }
    
    private fun installSystemAPI() {
        val g = globals ?: return
        
        val osLib = LuaTable()
        
        // os.clock():number
        osLib.set("clock", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf(machine.cpuTime)
            }
        })
        
        // os.date([format:string[, time:number]]):string|table
        osLib.set("date", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val format = args.optjstring(1, "%c")
                val time = args.optlong(2, System.currentTimeMillis() / 1000)
                
                // Simplified date formatting
                val date = java.util.Date(time * 1000)
                return LuaValue.valueOf(date.toString())
            }
        })
        
        // os.time():number
        osLib.set("time", object : ZeroArgFunction() {
            override fun call(): LuaValue {
                return LuaValue.valueOf((System.currentTimeMillis() / 1000).toDouble())
            }
        })
        
        g.set("os", osLib)
    }
    
    // ========================================
    // Boot Code
    // ========================================
    
    private fun getBootCode(): String? {
        // In a full implementation, this would load from EEPROM
        // For now, return a basic kernel
        return """
            -- OpenComputers Boot Kernel
            local component = component
            local computer = computer
            local unicode = unicode
            
            -- Find bootable filesystem
            local function findBootFS()
                for address, type in pairs(component.list("filesystem")) do
                    local proxy = component.proxy(address)
                    if proxy and proxy.exists("/init.lua") then
                        return proxy
                    end
                end
                return nil
            end
            
            -- Load and execute init.lua
            local bootfs = findBootFS()
            if bootfs then
                local handle = bootfs.open("/init.lua", "r")
                local code = ""
                repeat
                    local chunk = bootfs.read(handle, 4096)
                    if chunk then code = code .. chunk end
                until not chunk
                bootfs.close(handle)
                
                local fn, err = load(code, "=init.lua")
                if fn then
                    fn()
                else
                    error("Failed to load init.lua: " .. tostring(err))
                end
            else
                print("No bootable medium found.")
                print("Press any key to enter REPL...")
                computer.pullSignal()
                
                while true do
                    io.write("> ")
                    local input = ""
                    while true do
                        local signal, _, char = computer.pullSignal()
                        if signal == "key_down" then
                            if char == 13 then break
                            elseif char == 8 then input = input:sub(1, -2)
                            elseif char >= 32 then input = input .. string.char(char)
                            end
                        end
                    end
                    print()
                    if input ~= "" then
                        local fn, err = load(input)
                        if fn then
                            local ok, result = pcall(fn)
                            if ok then
                                if result ~= nil then print(tostring(result)) end
                            else
                                print("Error: " .. tostring(result))
                            end
                        else
                            print("Syntax error: " .. tostring(err))
                        end
                    end
                end
            end
        """.trimIndent()
    }
    
    // ========================================
    // Value Conversion
    // ========================================
    
    private fun toLuaValue(value: Any?): LuaValue = when (value) {
        null -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(value)
        is Int -> LuaValue.valueOf(value)
        is Long -> LuaValue.valueOf(value.toDouble())
        is Float -> LuaValue.valueOf(value.toDouble())
        is Double -> LuaValue.valueOf(value)
        is String -> LuaValue.valueOf(value)
        is ByteArray -> LuaValue.valueOf(String(value, Charsets.UTF_8))
        is Map<*, *> -> {
            val table = LuaTable()
            for ((k, v) in value) {
                table.set(toLuaValue(k), toLuaValue(v))
            }
            table
        }
        is List<*> -> {
            val table = LuaTable()
            value.forEachIndexed { index, item ->
                table.set(index + 1, toLuaValue(item))
            }
            table
        }
        is Array<*> -> {
            val table = LuaTable()
            value.forEachIndexed { index, item ->
                table.set(index + 1, toLuaValue(item))
            }
            table
        }
        else -> LuaValue.valueOf(value.toString())
    }
    
    private fun fromLuaValue(value: LuaValue): Any? = when {
        value.isnil() -> null
        value.isboolean() -> value.toboolean()
        value.isint() -> value.toint()
        value.isnumber() -> value.todouble()
        value.isstring() -> value.tojstring()
        value.istable() -> {
            val table = value.checktable()
            val map = mutableMapOf<Any?, Any?>()
            var key = LuaValue.NIL
            while (true) {
                val next = table.next(key)
                key = next.arg1()
                if (key.isnil()) break
                map[fromLuaValue(key)] = fromLuaValue(next.arg(2))
            }
            map
        }
        else -> value.tojstring()
    }
    
    private fun getCurrentMemoryUsage(): Int {
        // Approximate memory usage
        return (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()).toInt() / 10
    }
    
    // ========================================
    // Persistence
    // ========================================
    
    override fun load(tag: CompoundTag) {
        memoryLimit = tag.getInt("memoryLimit")
        // Full state persistence would require serializing the Lua state
    }
    
    override fun save(tag: CompoundTag) {
        tag.putInt("memoryLimit", memoryLimit)
    }
}
