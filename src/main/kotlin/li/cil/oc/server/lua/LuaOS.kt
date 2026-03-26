package li.cil.oc.server.lua

import li.cil.oc.OpenComputers
import li.cil.oc.api.machine.Machine
import li.cil.oc.api.machine.Value
import net.minecraft.server.MinecraftServer
import org.luaj.vm2.*
import org.luaj.vm2.lib.*
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Lua OS implementation for OpenComputers.
 * Provides the boot sequence and core operating system functions.
 */
object LuaOS {
    
    /**
     * Core system libraries loaded at boot time.
     */
    val CORE_LIBRARIES = listOf(
        "boot.lua",
        "computer.lua",
        "component.lua",
        "event.lua",
        "filesystem.lua",
        "unicode.lua",
        "text.lua",
        "keyboard.lua",
        "term.lua",
        "shell.lua",
        "package.lua"
    )
    
    /**
     * Get the boot script that initializes the Lua environment.
     * This is the first script run when the computer starts.
     */
    fun getBootScript(): String = """
        -- OpenComputers Boot Script
        -- This initializes the Lua environment and loads the operating system.
        
        local _OSVERSION = "OpenOS 1.8.0"
        local _MCVERSION = "1.21.4"
        
        -- Disable debug library for security
        debug = nil
        
        -- Core utilities
        local function checkArg(n, have, ...)
            have = type(have)
            local function check(want, ...)
                if not want then
                    return false
                else
                    return have == want or check(...)
                end
            end
            if not check(...) then
                error(string.format("bad argument #%d (%s expected, got %s)", n, table.concat({...}, " or "), have), 3)
            end
        end
        
        _G.checkArg = checkArg
        
        -- Initialize computer API
        local computer = computer or {}
        
        -- Store native functions
        local nativeComputer = {
            address = computer.address,
            tmpAddress = computer.tmpAddress,
            freeMemory = computer.freeMemory,
            totalMemory = computer.totalMemory,
            pushSignal = computer.pushSignal,
            pullSignal = computer.pullSignal,
            uptime = computer.uptime,
            energy = computer.energy,
            maxEnergy = computer.maxEnergy,
            users = computer.users,
            addUser = computer.addUser,
            removeUser = computer.removeUser,
            shutdown = computer.shutdown,
            getBootAddress = computer.getBootAddress,
            setBootAddress = computer.setBootAddress,
            beep = computer.beep
        }
        
        -- Signal queue
        local signalQueue = {}
        
        -- Event handlers
        local eventHandlers = {}
        
        -- Pull a signal with optional timeout
        function computer.pullSignal(timeout)
            checkArg(1, timeout, "number", "nil")
            local deadline = computer.uptime() + (timeout or math.huge)
            repeat
                local result = {nativeComputer.pullSignal(deadline - computer.uptime())}
                if result[1] then
                    return table.unpack(result)
                end
            until computer.uptime() >= deadline
        end
        
        -- Push a signal to the queue
        function computer.pushSignal(name, ...)
            checkArg(1, name, "string")
            return nativeComputer.pushSignal(name, ...)
        end
        
        -- Beep
        function computer.beep(frequency, duration)
            checkArg(1, frequency, "number", "string", "nil")
            checkArg(2, duration, "number", "nil")
            if type(frequency) == "string" then
                return nativeComputer.beep(frequency)
            end
            return nativeComputer.beep(frequency or 440, duration or 0.1)
        end
        
        _G.computer = computer
        
        -- Initialize component API
        local component = component or {}
        
        local nativeComponent = {
            list = component.list,
            type = component.type,
            slot = component.slot,
            methods = component.methods,
            invoke = component.invoke,
            doc = component.doc,
            proxy = component.proxy
        }
        
        -- Cache for component proxies
        local proxyCache = setmetatable({}, { __mode = "v" })
        
        -- Get a proxy for a component
        function component.proxy(address)
            checkArg(1, address, "string")
            if not proxyCache[address] then
                local ctype = nativeComponent.type(address)
                if not ctype then
                    return nil, "no such component"
                end
                local proxy = { address = address, type = ctype }
                local methods = nativeComponent.methods(address)
                for method, direct in pairs(methods) do
                    proxy[method] = setmetatable({}, {
                        __call = function(self, ...)
                            return nativeComponent.invoke(address, method, ...)
                        end,
                        __tostring = function()
                            return (nativeComponent.doc(address, method) or "")
                        end
                    })
                end
                proxyCache[address] = proxy
            end
            return proxyCache[address]
        end
        
        -- List components of a specific type
        function component.list(filter, exact)
            checkArg(1, filter, "string", "nil")
            checkArg(2, exact, "boolean", "nil")
            local list = nativeComponent.list(filter, exact)
            local key = nil
            return setmetatable(list, {
                __call = function()
                    key = next(list, key)
                    if key then
                        return key, list[key]
                    end
                end
            })
        end
        
        -- Get first component of type
        function component.get(address, componentType)
            checkArg(1, address, "string")
            checkArg(2, componentType, "string", "nil")
            for c, t in component.list(componentType, true) do
                if c:sub(1, #address) == address then
                    return c
                end
            end
            return nil, "no such component"
        end
        
        -- Check if a component is available
        function component.isAvailable(componentType)
            checkArg(1, componentType, "string")
            return component.list(componentType, true)() ~= nil
        end
        
        -- Get primary component of type
        local primary = {}
        function component.getPrimary(componentType)
            checkArg(1, componentType, "string")
            if primary[componentType] then
                return component.proxy(primary[componentType])
            end
            local address = component.list(componentType, true)()
            if address then
                return component.proxy(address)
            end
            return nil, "no primary '" .. componentType .. "' available"
        end
        
        function component.setPrimary(componentType, address)
            checkArg(1, componentType, "string")
            checkArg(2, address, "string", "nil")
            if address then
                local t = nativeComponent.type(address)
                if t ~= componentType then
                    error("component type mismatch")
                end
            end
            primary[componentType] = address
        end
        
        _G.component = component
        
        -- Event library
        local event = {}
        local handlers = {}
        local lastInterrupt = 0
        
        function event.listen(name, callback)
            checkArg(1, name, "string")
            checkArg(2, callback, "function")
            handlers[name] = handlers[name] or {}
            for _, handler in ipairs(handlers[name]) do
                if handler == callback then
                    return false
                end
            end
            table.insert(handlers[name], callback)
            return true
        end
        
        function event.ignore(name, callback)
            checkArg(1, name, "string")
            checkArg(2, callback, "function")
            if handlers[name] then
                for i, handler in ipairs(handlers[name]) do
                    if handler == callback then
                        table.remove(handlers[name], i)
                        return true
                    end
                end
            end
            return false
        end
        
        function event.pull(...)
            local args = {...}
            local timeout = math.huge
            local filter = {}
            
            if type(args[1]) == "number" then
                timeout = table.remove(args, 1)
            end
            for _, arg in ipairs(args) do
                if type(arg) == "string" then
                    table.insert(filter, arg)
                end
            end
            
            local deadline = computer.uptime() + timeout
            repeat
                local signal = {computer.pullSignal(deadline - computer.uptime())}
                if signal[1] then
                    -- Dispatch to handlers
                    if handlers[signal[1]] then
                        for _, handler in ipairs(handlers[signal[1]]) do
                            local ok, err = pcall(handler, table.unpack(signal))
                            if not ok then
                                -- Log error but continue
                            end
                        end
                    end
                    
                    -- Check filter
                    if #filter == 0 then
                        return table.unpack(signal)
                    end
                    for _, name in ipairs(filter) do
                        if signal[1] == name then
                            return table.unpack(signal)
                        end
                    end
                end
            until computer.uptime() >= deadline
        end
        
        function event.push(...)
            return computer.pushSignal(...)
        end
        
        function event.timer(interval, callback, times)
            checkArg(1, interval, "number")
            checkArg(2, callback, "function")
            checkArg(3, times, "number", "nil")
            times = times or math.huge
            local id = math.random(1, 0x7FFFFFFF)
            local function tick()
                if times > 0 then
                    times = times - 1
                    callback()
                    if times > 0 then
                        event.timer(interval, tick, 1)
                    end
                end
            end
            event.timer(interval, tick, 1)
            return id
        end
        
        function event.cancel(id)
            -- Cancel timer by ID
            return true
        end
        
        _G.event = event
        
        -- Unicode library
        local unicode = unicode or {}
        
        function unicode.char(...)
            return utf8.char(...)
        end
        
        function unicode.len(s)
            return utf8.len(s) or #s
        end
        
        function unicode.sub(s, i, j)
            checkArg(1, s, "string")
            checkArg(2, i, "number")
            checkArg(3, j, "number", "nil")
            i = i or 1
            j = j or -1
            
            if i < 0 then i = unicode.len(s) + i + 1 end
            if j < 0 then j = unicode.len(s) + j + 1 end
            
            if i < 1 then i = 1 end
            if j > unicode.len(s) then j = unicode.len(s) end
            if i > j then return "" end
            
            local offset = utf8.offset(s, i)
            local endOffset = utf8.offset(s, j + 1)
            if not offset then return "" end
            if not endOffset then endOffset = #s + 1 end
            return s:sub(offset, endOffset - 1)
        end
        
        function unicode.lower(s)
            return s:lower()
        end
        
        function unicode.upper(s)
            return s:upper()
        end
        
        function unicode.isWide(s)
            -- Simplified: assume non-ASCII chars are wide
            local byte = s:byte(1)
            if not byte then return false end
            return byte > 127
        end
        
        function unicode.charWidth(s)
            return unicode.isWide(s) and 2 or 1
        end
        
        function unicode.wlen(s)
            local len = 0
            for _, c in utf8.codes(s) do
                len = len + (unicode.isWide(utf8.char(c)) and 2 or 1)
            end
            return len
        end
        
        function unicode.wtrunc(s, n)
            local result = ""
            local width = 0
            for _, c in utf8.codes(s) do
                local char = utf8.char(c)
                local cw = unicode.isWide(char) and 2 or 1
                if width + cw > n then break end
                result = result .. char
                width = width + cw
            end
            return result
        end
        
        _G.unicode = unicode
        
        -- Print function
        function _G.print(...)
            local args = {...}
            local gpu = component.list("gpu")()
            local screen = component.list("screen")()
            
            if not gpu or not screen then
                return
            end
            
            gpu = component.proxy(gpu)
            gpu.bind(screen)
            
            local w, h = gpu.getResolution()
            local line = ""
            for i, v in ipairs(args) do
                if i > 1 then line = line .. "\t" end
                line = line .. tostring(v)
            end
            
            -- Simple scroll and print
            gpu.copy(1, 1, w, h - 1, 0, -1)
            gpu.fill(1, h, w, 1, " ")
            gpu.set(1, h, line)
        end
        
        -- Load boot filesystem
        local function loadFile(path)
            local eeprom = component.list("eeprom")()
            if not eeprom then
                error("No EEPROM found")
            end
            
            local bootAddress = computer.getBootAddress()
            if not bootAddress or bootAddress == "" then
                -- Try to find a bootable filesystem
                for address in component.list("filesystem") do
                    local proxy = component.proxy(address)
                    if proxy and proxy.exists("init.lua") then
                        bootAddress = address
                        computer.setBootAddress(address)
                        break
                    end
                end
            end
            
            if not bootAddress then
                error("No bootable medium found")
            end
            
            local fs = component.proxy(bootAddress)
            if not fs then
                error("Boot filesystem not found: " .. tostring(bootAddress))
            end
            
            local handle = fs.open(path, "r")
            if not handle then
                return nil, "file not found: " .. path
            end
            
            local content = ""
            repeat
                local chunk = fs.read(handle, math.huge)
                if chunk then
                    content = content .. chunk
                end
            until not chunk
            
            fs.close(handle)
            return content
        end
        
        -- Boot sequence
        local function boot()
            -- Load init.lua from boot filesystem
            local init, err = loadFile("init.lua")
            if not init then
                error("Boot failed: " .. tostring(err))
            end
            
            local fn, err = load(init, "=init.lua")
            if not fn then
                error("Boot failed: " .. tostring(err))
            end
            
            -- Run init
            local ok, err = pcall(fn)
            if not ok then
                error("Boot failed: " .. tostring(err))
            end
        end
        
        -- Start boot process
        local ok, err = pcall(boot)
        if not ok then
            print("Boot failed: " .. tostring(err))
            print("Press any key to continue...")
            event.pull("key_down")
        end
    """.trimIndent()
    
    /**
     * Get the EEPROM BIOS code that bootstraps the boot sequence.
     */
    fun getDefaultBiosCode(): String = """
        -- OpenComputers BIOS
        local component = component
        local computer = computer
        
        -- Find boot filesystem
        local function findBoot()
            local bootAddr = computer.getBootAddress()
            if bootAddr and bootAddr ~= "" then
                return bootAddr
            end
            
            for addr in component.list("filesystem") do
                local proxy = component.proxy(addr)
                if proxy.exists("init.lua") then
                    return addr
                end
            end
        end
        
        local bootAddr = findBoot()
        if not bootAddr then
            error("No bootable medium found")
        end
        
        computer.setBootAddress(bootAddr)
        
        -- Load and run init.lua
        local fs = component.proxy(bootAddr)
        local handle = fs.open("init.lua", "r")
        local code = ""
        repeat
            local chunk = fs.read(handle, math.huge)
            if chunk then code = code .. chunk end
        until not chunk
        fs.close(handle)
        
        local fn, err = load(code, "=init.lua")
        if not fn then error(err) end
        fn()
    """.trimIndent()
    
    /**
     * Get the init.lua script for OpenOS.
     */
    fun getInitScript(): String = """
        -- OpenOS Init Script
        
        local computer = computer
        local component = component
        local unicode = unicode
        
        -- Basic require implementation
        local loaded = {}
        local loading = {}
        
        function require(name)
            if loaded[name] then
                return loaded[name]
            end
            
            if loading[name] then
                error("circular dependency: " .. name)
            end
            
            loading[name] = true
            
            -- Try to find and load the module
            local fs = component.proxy(computer.getBootAddress())
            local paths = {
                "/lib/" .. name .. ".lua",
                "/lib/" .. name .. "/init.lua",
                "/usr/lib/" .. name .. ".lua",
                "/" .. name .. ".lua"
            }
            
            for _, path in ipairs(paths) do
                if fs.exists(path) then
                    local handle = fs.open(path, "r")
                    local code = ""
                    repeat
                        local chunk = fs.read(handle, math.huge)
                        if chunk then code = code .. chunk end
                    until not chunk
                    fs.close(handle)
                    
                    local fn, err = load(code, "=" .. path)
                    if fn then
                        local result = fn()
                        loaded[name] = result or true
                        loading[name] = nil
                        return loaded[name]
                    else
                        loading[name] = nil
                        error("Error loading " .. name .. ": " .. err)
                    end
                end
            end
            
            loading[name] = nil
            error("module not found: " .. name)
        end
        
        _G.require = require
        
        -- Load core libraries
        local term = require("term")
        local shell = require("shell")
        
        -- Welcome message
        term.clear()
        term.write("OpenOS 1.8.0\n")
        term.write("Minecraft 1.21.4 / NeoForge\n")
        term.write("\n")
        
        -- Run shell
        local ok, err = pcall(shell.run)
        if not ok then
            print("Shell error: " .. tostring(err))
        end
    """.trimIndent()
    
    /**
     * Get the term library script.
     */
    fun getTermLibrary(): String = """
        -- Terminal library
        local component = component
        local unicode = unicode
        
        local term = {}
        
        local gpu = nil
        local screen = nil
        local cursorX, cursorY = 1, 1
        local cursorBlink = true
        local w, h = 80, 25
        
        function term.bind(gpuAddr, screenAddr)
            gpu = component.proxy(gpuAddr)
            screen = screenAddr
            if gpu and screen then
                gpu.bind(screen)
                w, h = gpu.getResolution()
            end
        end
        
        function term.getGPU()
            return gpu and gpu.address
        end
        
        function term.getScreen()
            return screen
        end
        
        function term.isAvailable()
            return gpu ~= nil and screen ~= nil
        end
        
        function term.getResolution()
            return w, h
        end
        
        function term.getCursor()
            return cursorX, cursorY
        end
        
        function term.setCursor(x, y)
            cursorX = math.max(1, math.min(w, x))
            cursorY = math.max(1, math.min(h, y))
        end
        
        function term.setCursorBlink(enable)
            cursorBlink = enable
        end
        
        function term.clear()
            if gpu then
                gpu.fill(1, 1, w, h, " ")
                cursorX, cursorY = 1, 1
            end
        end
        
        function term.clearLine()
            if gpu then
                gpu.fill(1, cursorY, w, 1, " ")
                cursorX = 1
            end
        end
        
        function term.scroll(lines)
            lines = lines or 1
            if gpu then
                if lines > 0 then
                    gpu.copy(1, lines + 1, w, h - lines, 0, -lines)
                    gpu.fill(1, h - lines + 1, w, lines, " ")
                elseif lines < 0 then
                    gpu.copy(1, 1, w, h + lines, 0, -lines)
                    gpu.fill(1, 1, w, -lines, " ")
                end
            end
        end
        
        function term.write(text, wrap)
            if not gpu then return end
            
            text = tostring(text)
            wrap = wrap ~= false
            
            for c in text:gmatch(".") do
                if c == "\n" then
                    cursorX = 1
                    cursorY = cursorY + 1
                    if cursorY > h then
                        term.scroll(1)
                        cursorY = h
                    end
                elseif c == "\r" then
                    cursorX = 1
                elseif c == "\t" then
                    cursorX = cursorX + (8 - ((cursorX - 1) % 8))
                else
                    gpu.set(cursorX, cursorY, c)
                    cursorX = cursorX + 1
                    if wrap and cursorX > w then
                        cursorX = 1
                        cursorY = cursorY + 1
                        if cursorY > h then
                            term.scroll(1)
                            cursorY = h
                        end
                    end
                end
            end
        end
        
        function term.read(history, dobreak, hint, pwchar)
            local buffer = ""
            local cursor = 1
            local historyIndex = 0
            history = history or {}
            
            local startX, startY = term.getCursor()
            
            while true do
                -- Draw input
                term.setCursor(startX, startY)
                local display = pwchar and string.rep(pwchar, #buffer) or buffer
                term.write(display .. " ")
                term.setCursor(startX + cursor - 1, startY)
                
                local event, _, char, code = event.pull("key_down")
                
                if code == 28 then -- Enter
                    if dobreak ~= false then
                        term.write("\n")
                    end
                    return buffer
                elseif code == 14 then -- Backspace
                    if cursor > 1 then
                        buffer = buffer:sub(1, cursor - 2) .. buffer:sub(cursor)
                        cursor = cursor - 1
                    end
                elseif code == 211 then -- Delete
                    if cursor <= #buffer then
                        buffer = buffer:sub(1, cursor - 1) .. buffer:sub(cursor + 1)
                    end
                elseif code == 203 then -- Left
                    cursor = math.max(1, cursor - 1)
                elseif code == 205 then -- Right
                    cursor = math.min(#buffer + 1, cursor + 1)
                elseif code == 199 then -- Home
                    cursor = 1
                elseif code == 207 then -- End
                    cursor = #buffer + 1
                elseif code == 200 then -- Up (history)
                    if #history > 0 then
                        historyIndex = math.min(historyIndex + 1, #history)
                        buffer = history[#history - historyIndex + 1] or ""
                        cursor = #buffer + 1
                    end
                elseif code == 208 then -- Down (history)
                    if historyIndex > 0 then
                        historyIndex = historyIndex - 1
                        if historyIndex == 0 then
                            buffer = ""
                        else
                            buffer = history[#history - historyIndex + 1] or ""
                        end
                        cursor = #buffer + 1
                    end
                elseif char >= 32 and char < 127 then
                    buffer = buffer:sub(1, cursor - 1) .. string.char(char) .. buffer:sub(cursor)
                    cursor = cursor + 1
                end
            end
        end
        
        -- Auto-bind on first load
        local gpuAddr = component.list("gpu")()
        local screenAddr = component.list("screen")()
        if gpuAddr and screenAddr then
            term.bind(gpuAddr, screenAddr)
        end
        
        return term
    """.trimIndent()
    
    /**
     * Get the shell library script.
     */
    fun getShellLibrary(): String = """
        -- Shell library
        local component = component
        local computer = computer
        local event = event
        local term = require("term")
        
        local shell = {}
        
        local workingDir = "/"
        local aliases = {
            dir = "ls",
            copy = "cp",
            rename = "mv",
            del = "rm",
            md = "mkdir"
        }
        local history = {}
        
        function shell.getWorkingDirectory()
            return workingDir
        end
        
        function shell.setWorkingDirectory(path)
            local fs = component.proxy(computer.getBootAddress())
            if fs.isDirectory(path) then
                workingDir = path
                if not workingDir:match("/$") then
                    workingDir = workingDir .. "/"
                end
                return true
            end
            return false, "not a directory"
        end
        
        function shell.resolve(path)
            if path:sub(1, 1) == "/" then
                return path
            end
            return workingDir .. path
        end
        
        function shell.setAlias(name, value)
            aliases[name] = value
        end
        
        function shell.getAlias(name)
            return aliases[name]
        end
        
        function shell.aliases()
            return pairs(aliases)
        end
        
        function shell.execute(command, ...)
            if not command or command == "" then
                return true
            end
            
            -- Check for alias
            local args = {...}
            local cmd = command:match("^(%S+)")
            if aliases[cmd] then
                command = aliases[cmd] .. command:sub(#cmd + 1)
                cmd = command:match("^(%S+)")
            end
            
            -- Built-in commands
            if cmd == "cd" then
                local path = command:match("^%S+%s+(.+)") or "/"
                local ok, err = shell.setWorkingDirectory(shell.resolve(path))
                if not ok then
                    print("cd: " .. err)
                end
                return ok
            elseif cmd == "pwd" then
                print(workingDir)
                return true
            elseif cmd == "ls" then
                local path = command:match("^%S+%s+(.+)") or workingDir
                path = shell.resolve(path)
                local fs = component.proxy(computer.getBootAddress())
                local list = fs.list(path)
                if list then
                    for _, entry in ipairs(list) do
                        term.write(entry .. "\n")
                    end
                else
                    print("ls: cannot access '" .. path .. "'")
                end
                return true
            elseif cmd == "cat" then
                local path = command:match("^%S+%s+(.+)")
                if path then
                    path = shell.resolve(path)
                    local fs = component.proxy(computer.getBootAddress())
                    local handle = fs.open(path, "r")
                    if handle then
                        repeat
                            local chunk = fs.read(handle, 4096)
                            if chunk then term.write(chunk) end
                        until not chunk
                        fs.close(handle)
                        term.write("\n")
                    else
                        print("cat: cannot open '" .. path .. "'")
                    end
                else
                    print("Usage: cat <file>")
                end
                return true
            elseif cmd == "clear" then
                term.clear()
                return true
            elseif cmd == "reboot" then
                computer.shutdown(true)
                return true
            elseif cmd == "shutdown" then
                computer.shutdown(false)
                return true
            elseif cmd == "exit" then
                return false, "exit"
            elseif cmd == "help" then
                print("Built-in commands:")
                print("  cd <dir>     - Change directory")
                print("  pwd          - Print working directory")
                print("  ls [dir]     - List directory")
                print("  cat <file>   - Print file contents")
                print("  clear        - Clear screen")
                print("  reboot       - Restart computer")
                print("  shutdown     - Turn off computer")
                print("  exit         - Exit shell")
                print("  help         - Show this help")
                return true
            end
            
            -- Try to find and execute program
            local fs = component.proxy(computer.getBootAddress())
            local paths = {
                shell.resolve(cmd),
                "/bin/" .. cmd .. ".lua",
                "/usr/bin/" .. cmd .. ".lua",
                "/home/bin/" .. cmd .. ".lua"
            }
            
            for _, path in ipairs(paths) do
                if fs.exists(path) then
                    local handle = fs.open(path, "r")
                    if handle then
                        local code = ""
                        repeat
                            local chunk = fs.read(handle, 4096)
                            if chunk then code = code .. chunk end
                        until not chunk
                        fs.close(handle)
                        
                        -- Parse remaining args from command
                        local cmdArgs = {}
                        for arg in command:gmatch("%S+") do
                            table.insert(cmdArgs, arg)
                        end
                        table.remove(cmdArgs, 1)  -- Remove command name
                        
                        local fn, err = load(code, "=" .. path)
                        if fn then
                            local ok, result = pcall(fn, table.unpack(cmdArgs))
                            if not ok then
                                print("Error: " .. tostring(result))
                            end
                            return ok
                        else
                            print("Syntax error: " .. tostring(err))
                            return false
                        end
                    end
                end
            end
            
            print(cmd .. ": command not found")
            return false
        end
        
        function shell.run()
            term.clear()
            term.write("OpenOS 1.8.0\n")
            term.write("Type 'help' for available commands.\n\n")
            
            while true do
                term.write(workingDir .. "> ")
                local command = term.read(history)
                
                if command and command ~= "" then
                    table.insert(history, command)
                    if #history > 50 then
                        table.remove(history, 1)
                    end
                    
                    local ok, err = shell.execute(command)
                    if err == "exit" then
                        break
                    end
                end
            end
        end
        
        return shell
    """.trimIndent()
}

/**
 * Represents a Lua value that can be passed to/from the Lua VM.
 */
interface LuaValue : Value {
    fun toLuaj(): org.luaj.vm2.LuaValue
}

/**
 * Wrapper for primitive Lua values.
 */
class PrimitiveLuaValue(private val value: Any?) : LuaValue {
    override fun toLuaj(): org.luaj.vm2.LuaValue = when (value) {
        null -> LuaValue.NIL
        is Boolean -> LuaValue.valueOf(value)
        is Number -> LuaValue.valueOf(value.toDouble())
        is String -> LuaValue.valueOf(value)
        is ByteArray -> LuaValue.valueOf(value)
        else -> LuaValue.valueOf(value.toString())
    }
    
    override fun load(nbt: net.minecraft.nbt.CompoundTag) {}
    override fun save(nbt: net.minecraft.nbt.CompoundTag) {}
    override fun dispose(context: li.cil.oc.api.machine.Context) {}
}

/**
 * Lua table wrapper.
 */
class LuaTable : LuaValue {
    private val values = mutableMapOf<Any, Any?>()
    
    operator fun get(key: Any): Any? = values[key]
    operator fun set(key: Any, value: Any?) { values[key] = value }
    
    fun keys(): Set<Any> = values.keys
    fun size(): Int = values.size
    
    override fun toLuaj(): org.luaj.vm2.LuaValue {
        val table = org.luaj.vm2.LuaTable()
        for ((k, v) in values) {
            val key = when (k) {
                is Number -> org.luaj.vm2.LuaValue.valueOf(k.toDouble())
                is String -> org.luaj.vm2.LuaValue.valueOf(k)
                else -> org.luaj.vm2.LuaValue.valueOf(k.toString())
            }
            val value = when (v) {
                null -> org.luaj.vm2.LuaValue.NIL
                is Number -> org.luaj.vm2.LuaValue.valueOf(v.toDouble())
                is String -> org.luaj.vm2.LuaValue.valueOf(v)
                is Boolean -> org.luaj.vm2.LuaValue.valueOf(v)
                is LuaValue -> v.toLuaj()
                else -> org.luaj.vm2.LuaValue.valueOf(v.toString())
            }
            table.set(key, value)
        }
        return table
    }
    
    override fun load(nbt: net.minecraft.nbt.CompoundTag) {}
    override fun save(nbt: net.minecraft.nbt.CompoundTag) {}
    override fun dispose(context: li.cil.oc.api.machine.Context) {}
}
