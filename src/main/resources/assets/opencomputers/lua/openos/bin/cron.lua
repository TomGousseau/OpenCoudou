-- cron.lua - Task scheduler / cron-like daemon for OpenOS
-- Usage: cron [start|stop|list|add|remove]
--   cron start               -> start the cron daemon in background
--   cron list                -> list scheduled jobs
--   cron add <interval> <cmd> -> add a recurring job
--   cron remove <id>         -> remove a job

local shell  = require("shell")
local fs     = require("filesystem")
local event  = require("event")
local serialization = require("serialization")

local CRON_FILE  = "/etc/crontab"
local CRON_PID_F = "/var/run/cron.pid"

local args = { ... }
local subcmd = args[1] or "list"

-- -----------------------------------------------------------------------
-- Job management
-- -----------------------------------------------------------------------

local function loadJobs()
    if not fs.exists(CRON_FILE) then return {} end
    local content = fs.readAll(CRON_FILE)
    if not content or content == "" then return {} end
    local jobs = serialization.unserialize(content)
    return type(jobs) == "table" and jobs or {}
end

local function saveJobs(jobs)
    fs.makeDirectory("/etc")
    fs.writeAll(CRON_FILE, serialization.serialize(jobs))
end

local function nextId(jobs)
    local max = 0
    for _, j in ipairs(jobs) do
        if j.id > max then max = j.id end
    end
    return max + 1
end

-- -----------------------------------------------------------------------
-- Subcommands
-- -----------------------------------------------------------------------

if subcmd == "list" then
    local jobs = loadJobs()
    if #jobs == 0 then
        print("No scheduled jobs.")
        return 0
    end
    print(string.format("%-4s  %-10s  %-8s  %s", "ID", "Interval", "Next", "Command"))
    print(string.rep("-", 60))
    local now = computer.uptime()
    for _, j in ipairs(jobs) do
        local nextIn = math.max(0, (j.nextRun or 0) - now)
        print(string.format("%-4d  %-10.1f  in %-5.0fs  %s", j.id, j.interval, nextIn, j.cmd))
    end
    return 0

elseif subcmd == "add" then
    local intervalStr = args[2]
    local cmd = table.concat(args, " ", 3)
    if not intervalStr or cmd == "" then
        io.stderr:write("Usage: cron add <interval_seconds> <command>\n")
        return 1
    end
    local interval = tonumber(intervalStr)
    if not interval or interval < 1 then
        io.stderr:write("Interval must be a positive number of seconds.\n")
        return 1
    end
    local jobs = loadJobs()
    local id = nextId(jobs)
    table.insert(jobs, {
        id        = id,
        interval  = interval,
        cmd       = cmd,
        nextRun   = computer.uptime() + interval,
        lastRun   = nil,
        runCount  = 0,
    })
    saveJobs(jobs)
    print(string.format("Job #%d added: every %.0f seconds -> %s", id, interval, cmd))
    return 0

elseif subcmd == "remove" or subcmd == "rm" then
    local id = tonumber(args[2])
    if not id then
        io.stderr:write("Usage: cron remove <id>\n")
        return 1
    end
    local jobs = loadJobs()
    local newJobs = {}
    local found = false
    for _, j in ipairs(jobs) do
        if j.id == id then
            found = true
        else
            table.insert(newJobs, j)
        end
    end
    if not found then
        io.stderr:write("Job #" .. id .. " not found.\n")
        return 1
    end
    saveJobs(newJobs)
    print("Job #" .. id .. " removed.")
    return 0

elseif subcmd == "stop" then
    -- Signal the running daemon to stop
    computer.pushSignal("cron_stop")
    print("Sent stop signal to cron daemon.")
    return 0

elseif subcmd == "start" then
    -- -----------------------------------------------------------------------
    -- Daemon mode - runs in a thread
    -- -----------------------------------------------------------------------
    print("Starting cron daemon...")
    
    -- Write PID file
    fs.makeDirectory("/var/run")
    
    local process = require("process")
    
    process.spawn(function()
        local running = true
        
        -- Listen for stop signal
        event.listen("cron_stop", function()
            running = false
        end)
        
        print("cron: daemon started")
        
        while running do
            local now = computer.uptime()
            local jobs = loadJobs()
            local modified = false
            
            for _, j in ipairs(jobs) do
                if j.nextRun and now >= j.nextRun then
                    -- Run the job
                    local ok, err = pcall(shell.run, j.cmd)
                    j.lastRun  = now
                    j.runCount = (j.runCount or 0) + 1
                    j.nextRun  = now + j.interval
                    modified   = true
                    
                    if not ok then
                        -- Log error
                        if fs.exists("/var/log") or pcall(fs.makeDirectory, "/var/log") then
                            local logEntry = string.format("[%.1f] cron job #%d error: %s\n", now, j.id, tostring(err))
                            local existing = fs.exists("/var/log/cron.log") and fs.readAll("/var/log/cron.log") or ""
                            -- Keep only last 4KB of log
                            if #existing > 4096 then existing = existing:sub(-4096) end
                            fs.writeAll("/var/log/cron.log", existing .. logEntry)
                        end
                    end
                end
            end
            
            if modified then
                saveJobs(jobs)
            end
            
            -- Sleep until next job, or max 60s
            local minWait = 60
            for _, j in ipairs(jobs) do
                if j.nextRun then
                    local wait = j.nextRun - computer.uptime()
                    if wait < minWait then minWait = wait end
                end
            end
            
            if minWait > 0 then
                event.pull(minWait, "cron_stop")
            end
        end
        
        print("cron: daemon stopped")
    end, "cron-daemon")
    
    return 0

else
    io.stderr:write("Usage: cron <list|add|remove|start|stop>\n")
    return 1
end
