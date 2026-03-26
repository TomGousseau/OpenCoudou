-- install.lua - OpenOS installer
local shell = require("shell")
local fs = require("filesystem")
local term = require("term")
local text = require("text")
local serialization = require("serialization")

local args = { ... }

-- Get possible source filesystems (OPROM, floppy, etc.)
local function findSources()
    local sources = {}
    for addr, ctype in component.list("filesystem") do
        -- Check if it has openos/init.lua (the OpenOS floppy)
        local ok, result = pcall(component.invoke, addr, "exists", "/openos/init.lua")
        if ok and result then
            table.insert(sources, { addr = addr, type = "openos" })
        else
            -- Check for a ROM/resource filesystem
            local ok2, ro = pcall(component.invoke, addr, "isReadOnly")
            if ok2 and ro then
                table.insert(sources, { addr = addr, type = "rom" })
            end
        end
    end
    return sources
end

local function findTargets()
    local targets = {}
    for addr, _ in component.list("filesystem") do
        local ok, ro = pcall(component.invoke, addr, "isReadOnly")
        if ok and not ro then
            local ok2, free = pcall(component.invoke, addr, "spaceAvailable")
            if ok2 and free > 512 * 1024 then -- Need at least 512KB free
                table.insert(targets, { addr = addr, free = free })
            end
        end
    end
    return targets
end

local function copyDir(srcAddr, srcPath, dstPath, callback)
    local ok, files = pcall(component.invoke, srcAddr, "list", srcPath)
    if not ok or not files then return end
    
    for _, name in ipairs(files) do
        local srcFull = srcPath:match("/$") and (srcPath .. name) or (srcPath .. "/" .. name)
        local dstFull = dstPath:match("/$") and (dstPath .. name) or (dstPath .. "/" .. name)
        
        local ok2, isDir = pcall(component.invoke, srcAddr, "isDirectory", srcFull)
        if ok2 and isDir then
            fs.makeDirectory(dstFull)
            copyDir(srcAddr, srcFull, dstFull, callback)
        else
            -- Read and write file
            local ok3, handle = pcall(component.invoke, srcAddr, "open", srcFull, "r")
            if ok3 and handle then
                local data = ""
                while true do
                    local ok4, chunk = pcall(component.invoke, srcAddr, "read", handle, 4096)
                    if not ok4 or not chunk then break end
                    data = data .. chunk
                end
                pcall(component.invoke, srcAddr, "close", handle)
                
                fs.writeAll(dstFull, data)
                if callback then callback(dstFull) end
            end
        end
    end
end

-- Main installer

local function run()
    term.clear()
    term.setForeground(0x00AAFF)
    local w, h = term.getSize()
    print(text.center("=== OpenOS Installer ===", w))
    term.setForeground(0xFFFFFF)
    print("")
    
    -- Find sources
    term.write("Scanning for OpenOS source... ")
    local sources = findSources()
    if #sources == 0 then
        term.setForeground(0xFF4444)
        print("NOT FOUND")
        print("Please insert an OpenOS floppy disk.")
        term.setForeground(0xFFFFFF)
        return false
    end
    term.setForeground(0x44FF44)
    print("Found " .. #sources .. " source(s)")
    term.setForeground(0xFFFFFF)
    
    -- Find targets
    term.write("Scanning for writable drives... ")
    local targets = findTargets()
    if #targets == 0 then
        term.setForeground(0xFF4444)
        print("NONE FOUND")
        print("Please insert a writable disk drive.")
        term.setForeground(0xFFFFFF)
        return false
    end
    term.setForeground(0x44FF44)
    print("Found " .. #targets .. " target(s)")
    term.setForeground(0xFFFFFF)
    
    -- Select source
    local source = sources[1]
    local target = targets[1]
    
    print("")
    print("Source: " .. source.addr:sub(1, 8) .. " (" .. source.type .. ")")
    local freeStr = string.format("%.1f MB", target.free / (1024 * 1024))
    print("Target: " .. target.addr:sub(1, 8) .. " (free: " .. freeStr .. ")")
    print("")
    
    term.write("Install OpenOS to this drive? [y/N] ")
    local answer = term.read()
    if not answer or answer:lower() ~= "y" then
        print("Installation cancelled.")
        return false
    end
    
    print("")
    term.setForeground(0xFFFF44)
    print("Installing OpenOS...")
    term.setForeground(0xFFFFFF)
    
    local copied = 0
    local function onCopy(path)
        copied = copied + 1
        if copied % 10 == 0 then
            io.write("\r  Copied " .. copied .. " files...   ")
        end
    end
    
    -- Create directory structure on target
    local dirs = { "/bin", "/lib", "/etc", "/home/root", "/tmp", "/usr/bin", "/usr/lib" }
    for _, d in ipairs(dirs) do
        fs.makeDirectory(d)
    end
    
    -- Copy from source
    copyDir(source.addr, "/openos", "/", onCopy)
    
    print("\r  Copied " .. copied .. " files." .. string.rep(" ", 20))
    
    -- Write default config
    local defaultProfile = [[
-- /etc/profile.lua - System profile
-- Runs when a new shell starts

require("shell").setPath("/bin:/usr/bin:/home/root/bin")
]]
    fs.writeAll("/etc/profile.lua", defaultProfile)
    
    -- Make the target bootable by setting boot address
    component.invoke(component.list("eeprom")(), "setData", target.addr)
    
    print("")
    term.setForeground(0x44FF44)
    print("Installation complete!")
    term.setForeground(0xFFFFFF)
    print("Remove the floppy and reboot to start OpenOS.")
    print("")
    term.write("Reboot now? [y/N] ")
    local reboot = term.read()
    if reboot and reboot:lower() == "y" then
        computer.shutdown(true)
    end
    
    return true
end

run()
