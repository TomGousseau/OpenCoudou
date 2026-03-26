-- computer.lua - Higher-level computer information library
-- Wraps the raw computer API with convenience functions

local computer = {}

-- Re-export raw API functions
computer.address        = _G.computer and _G.computer.address        or function() return "00000000-0000-0000-0000-000000000000" end
computer.tmpAddress     = _G.computer and _G.computer.tmpAddress     or function() return nil end
computer.freeMemory     = _G.computer and _G.computer.freeMemory     or function() return 0 end
computer.totalMemory    = _G.computer and _G.computer.totalMemory    or function() return 0 end
computer.uptime         = _G.computer and _G.computer.uptime         or function() return 0 end
computer.energy         = _G.computer and _G.computer.energy         or function() return 0 end
computer.maxEnergy      = _G.computer and _G.computer.maxEnergy      or function() return 0 end
computer.users          = _G.computer and _G.computer.users          or function() return {} end
computer.shutdown       = _G.computer and _G.computer.shutdown       or function() end
computer.pushSignal     = _G.computer and _G.computer.pushSignal     or function() end
computer.pullSignal     = _G.computer and _G.computer.pullSignal     or require("event").pull
computer.beep           = _G.computer and _G.computer.beep           or function() end

-- Get short address (first 8 hex chars)
function computer.shortAddress()
    return computer.address():sub(1, 8)
end

-- Format uptime as HH:MM:SS
function computer.formatUptime()
    local secs = math.floor(computer.uptime())
    local hours = math.floor(secs / 3600)
    local mins  = math.floor((secs % 3600) / 60)
    local s     = secs % 60
    return string.format("%02d:%02d:%02d", hours, mins, s)
end

-- Get used memory
function computer.usedMemory()
    return computer.totalMemory() - computer.freeMemory()
end

-- Memory usage as fraction 0.0 - 1.0
function computer.memoryUsage()
    local total = computer.totalMemory()
    if total == 0 then return 0 end
    return computer.usedMemory() / total
end

-- Energy usage as fraction 0.0 - 1.0
function computer.energyUsage()
    local max = computer.maxEnergy()
    if max == 0 then return 0 end
    return computer.energy() / max
end

-- Format bytes as human-readable string
function computer.formatBytes(bytes)
    if bytes >= 1024 * 1024 then
        return string.format("%.1f MB", bytes / (1024 * 1024))
    elseif bytes >= 1024 then
        return string.format("%.1f KB", bytes / 1024)
    else
        return bytes .. " B"
    end
end

-- Play a sequence of beeps: takes a string like "...-..." (. = short, - = long)
-- or a table of { freq, duration } pairs
function computer.beepSequence(seq, baseFreq)
    baseFreq = baseFreq or 440
    if type(seq) == "string" then
        for i = 1, #seq do
            local ch = seq:sub(i, i)
            if ch == "." then
                computer.beep(baseFreq, 0.1)
                os.sleep(0.05)
            elseif ch == "-" then
                computer.beep(baseFreq, 0.3)
                os.sleep(0.05)
            elseif ch == " " then
                os.sleep(0.2)
            end
        end
    elseif type(seq) == "table" then
        for _, note in ipairs(seq) do
            computer.beep(note[1] or baseFreq, note[2] or 0.1)
            os.sleep((note[2] or 0.1) * 0.5)
        end
    end
end

-- Play startup beep sequence
function computer.startupBeep()
    computer.beep(770, 0.05)
    os.sleep(0.02)
    computer.beep(1100, 0.05)
end

-- Play error beep
function computer.errorBeep()
    computer.beep(200, 0.2)
    os.sleep(0.1)
    computer.beep(150, 0.3)
end

-- Check if current user is in the users list
function computer.isUser(name)
    for _, u in ipairs({ computer.users() }) do
        if u == name then return true end
    end
    return false
end

-- Add user to the computer (wrapper with error handling)
function computer.addUser(name)
    local ok, err = pcall(function()
        component.invoke(computer.address(), "addUser", name)
    end)
    return ok, err
end

-- Remove user from the computer
function computer.removeUser(name)
    local ok, err = pcall(function()
        component.invoke(computer.address(), "removeUser", name)
    end)
    return ok, err
end

-- Get the hostname (short address by default, or from /etc/hostname)
function computer.getHostname()
    local ok, content = pcall(require("filesystem").readAll, "/etc/hostname")
    if ok and content then
        return content:gsub("%s+", "")
    end
    return "oc-" .. computer.shortAddress()
end

-- Set the hostname by writing /etc/hostname
function computer.setHostname(name)
    return require("filesystem").writeAll("/etc/hostname", name .. "\n")
end

-- Get system info table
function computer.info()
    return {
        address     = computer.address(),
        uptime      = computer.uptime(),
        freeMemory  = computer.freeMemory(),
        totalMemory = computer.totalMemory(),
        energy      = computer.energy(),
        maxEnergy   = computer.maxEnergy(),
        users       = { computer.users() },
        hostname    = computer.getHostname(),
    }
end

-- Wait for a specific signal with optional timeout
-- Returns signal data table, or nil on timeout
function computer.waitFor(signalName, timeout)
    local deadline = timeout and (computer.uptime() + timeout)
    while true do
        local remaining = deadline and (deadline - computer.uptime())
        if remaining and remaining <= 0 then return nil end
        local event = { computer.pullSignal(remaining or math.huge) }
        if event[1] == signalName then
            return event
        end
    end
end

-- Sleep for a number of seconds (using pullSignal)
function computer.sleep(seconds)
    local deadline = computer.uptime() + seconds
    repeat
        computer.pullSignal(deadline - computer.uptime())
    until computer.uptime() >= deadline
end

-- Schedule a function to run after a delay (adds a thread via process)
function computer.schedule(delay, fn)
    local ok, process = pcall(require, "process")
    if not ok then return end
    process.addThread(coroutine.create(function()
        computer.sleep(delay)
        fn()
    end))
end

-- Run garbage collection and return freed memory
function computer.gc()
    local before = computer.freeMemory()
    collectgarbage("collect")
    local after = computer.freeMemory()
    return after - before
end

return computer
