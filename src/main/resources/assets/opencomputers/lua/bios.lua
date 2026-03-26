-- OpenComputers Boot ROM (EEPROM)
-- This is the default BIOS that boots from available media

local function boot()
    -- Initialize component API wrappers
    local component = component
    local computer = computer
    
    -- Find first GPU and screen for output
    local gpu, screen
    for address, type in component.list("gpu") do
        gpu = address
        break
    end
    for address, type in component.list("screen") do
        screen = address
        break
    end
    
    -- Bind GPU to screen if found
    if gpu and screen then
        component.invoke(gpu, "bind", screen)
    end
    
    -- Simple print function for early boot
    local function print(msg)
        if gpu then
            local w, h = component.invoke(gpu, "getResolution")
            local y = 1
            -- Scroll if needed
            component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
            component.invoke(gpu, "fill", 1, h, w, 1, " ")
            component.invoke(gpu, "set", 1, h, tostring(msg))
        end
    end
    
    -- Initialize screen
    if gpu and screen then
        local maxW, maxH = component.invoke(gpu, "maxResolution")
        local w = math.min(maxW, 80)
        local h = math.min(maxH, 25)
        component.invoke(gpu, "setResolution", w, h)
        component.invoke(gpu, "setBackground", 0x000000)
        component.invoke(gpu, "setForeground", 0xFFFFFF)
        component.invoke(gpu, "fill", 1, 1, w, h, " ")
    end
    
    print("OpenComputers BIOS")
    print("Searching for bootable medium...")
    
    -- Find bootable filesystem
    local bootfs
    local bootAddress = computer.getBootAddress()
    
    if bootAddress then
        -- Try configured boot address first
        local type = component.type(bootAddress)
        if type == "filesystem" then
            if component.invoke(bootAddress, "exists", "init.lua") then
                bootfs = bootAddress
            end
        end
    end
    
    if not bootfs then
        -- Search all filesystems
        for address, type in component.list("filesystem") do
            if component.invoke(address, "exists", "init.lua") then
                bootfs = address
                break
            end
        end
    end
    
    if not bootfs then
        -- Try to find OpenOS installer
        for address, type in component.list("filesystem") do
            if component.invoke(address, "getLabel") == "openos" then
                if component.invoke(address, "exists", ".install") then
                    bootfs = address
                    break
                end
            end
        end
    end
    
    if not bootfs then
        print("No bootable medium found!")
        print("Insert a bootable disk or press any key...")
        while true do
            local signal = computer.pullSignal()
            if signal == "key_down" then
                computer.shutdown(true)
            elseif signal == "component_added" then
                -- Component added, try again
                computer.shutdown(true)
            end
        end
    end
    
    -- Set boot address
    computer.setBootAddress(bootfs)
    print("Booting from " .. bootfs:sub(1, 8) .. "...")
    
    -- Load init.lua
    local handle, err = component.invoke(bootfs, "open", "init.lua", "r")
    if not handle then
        print("Failed to open init.lua: " .. tostring(err))
        print("Press any key to reboot...")
        repeat until computer.pullSignal() == "key_down"
        computer.shutdown(true)
    end
    
    local code = ""
    repeat
        local chunk = component.invoke(bootfs, "read", handle, math.huge)
        code = code .. (chunk or "")
    until not chunk
    component.invoke(bootfs, "close", handle)
    
    -- Execute init.lua
    local init, err = load(code, "=init.lua", "bt", _G)
    if not init then
        print("Failed to load init.lua: " .. tostring(err))
        print("Press any key to reboot...")
        repeat until computer.pullSignal() == "key_down"
        computer.shutdown(true)
    end
    
    -- Create filesystem proxy
    local fs = component.proxy(bootfs)
    
    -- Execute init
    local ok, err = xpcall(init, debug.traceback, fs)
    if not ok then
        print("Boot failed: " .. tostring(err))
        print("Press any key to reboot...")
        repeat until computer.pullSignal() == "key_down"
        computer.shutdown(true)
    end
end

-- Run boot with error handling
local ok, err = xpcall(boot, debug.traceback)
if not ok then
    -- Last resort error display
    local gpu = component.list("gpu")()
    local screen = component.list("screen")()
    if gpu and screen then
        component.invoke(gpu, "bind", screen)
        component.invoke(gpu, "setResolution", 50, 16)
        component.invoke(gpu, "setBackground", 0x0000FF)
        component.invoke(gpu, "fill", 1, 1, 50, 16, " ")
        component.invoke(gpu, "setForeground", 0xFFFFFF)
        component.invoke(gpu, "set", 1, 1, "FATAL BOOT ERROR")
        component.invoke(gpu, "set", 1, 3, tostring(err):sub(1, 50))
    end
    
    while true do
        local signal = computer.pullSignal()
        if signal == "key_down" then
            computer.shutdown(true)
        end
    end
end
