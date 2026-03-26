-- process.lua - Coroutine-based process manager

local computer = computer

local process = {}

local processes = {}   -- all running processes
local current = nil    -- currently executing process id
local nextPid = 1

-- Internal process structure:
-- { pid, name, coroutine, parent, env, handles, dead, exitCode, threads }

local function newProcess(name, fn, env, parent)
    local pid = nextPid
    nextPid = nextPid + 1
    local proc = {
        pid = pid,
        name = name or ("process" .. pid),
        coroutine = coroutine.create(fn),
        parent = parent,
        env = env or {},
        handles = {},
        dead = false,
        exitCode = 0,
        threads = {},
    }
    processes[pid] = proc
    return proc
end

-- Spawn a new process
function process.spawn(fn, name, env)
    local parentPid = current
    local proc = newProcess(name, fn, env, parentPid)
    return proc.pid
end

-- Get current process
function process.running()
    return current
end

-- Get process by pid
function process.info(pid)
    pid = pid or current
    local p = processes[pid]
    if not p then return nil end
    return {
        pid = p.pid,
        name = p.name,
        parent = p.parent,
        dead = p.dead,
        exitCode = p.exitCode,
    }
end

-- List all processes
function process.list()
    local list = {}
    for pid, p in pairs(processes) do
        table.insert(list, { pid = p.pid, name = p.name, dead = p.dead })
    end
    return list
end

-- Kill a process
function process.kill(pid, exitCode)
    local p = processes[pid]
    if not p then return false end
    p.dead = true
    p.exitCode = exitCode or -1
    -- Close all open handles
    for _, handle in pairs(p.handles) do
        if type(handle) == "table" and handle.close then
            pcall(handle.close, handle)
        end
    end
    return true
end

-- Register a file handle with current process
function process.registerHandle(handle)
    if not current then return end
    local p = processes[current]
    if p then
        table.insert(p.handles, handle)
    end
end

-- Close and unregister a file handle
function process.unregisterHandle(handle)
    if not current then return end
    local p = processes[current]
    if p then
        for i, h in ipairs(p.handles) do
            if h == handle then
                table.remove(p.handles, i)
                return
            end
        end
    end
end

-- Get environment of current process
function process.getenv(key)
    if not current then return nil end
    local p = processes[current]
    if not p then return nil end
    if key then
        return p.env[key]
    end
    return p.env
end

-- Set environment variable in current process
function process.setenv(key, value)
    if not current then return end
    local p = processes[current]
    if p then
        p.env[key] = value
    end
end

-- Thread support: add a thread to current process
function process.addThread(co)
    if not current then return end
    local p = processes[current]
    if p then
        table.insert(p.threads, co)
    end
end

-- Resume a specific process with a signal
local function resumeProcess(p, ...)
    if p.dead then return false end
    local prevCurrent = current
    current = p.pid
    local ok, result = coroutine.resume(p.coroutine, ...)
    current = prevCurrent
    
    if not ok then
        -- Process crashed
        p.dead = true
        p.exitCode = -1
        io.stderr:write("Process " .. p.name .. " crashed: " .. tostring(result) .. "\n")
        return false, result
    end
    
    if coroutine.status(p.coroutine) == "dead" then
        p.dead = true
        p.exitCode = result or 0
        return true, result
    end
    
    return true
end

-- Main scheduler tick — called from init.lua event loop
function process.tick(signal)
    -- Resume all living processes with signal
    local dead = {}
    for pid, p in pairs(processes) do
        if not p.dead then
            resumeProcess(p, table.unpack(signal or {}))
        end
        if p.dead then
            table.insert(dead, pid)
        end
    end
    -- Clean up dead processes (after a delay to allow exit code reading)
    -- Don't remove immediately in case parent wants exit code
end

-- Run a function synchronously, blocking until it returns
-- Used by init to run the main shell as one process
function process.run(fn, name, ...)
    local args = { ... }
    local pid = process.spawn(function() return fn(table.unpack(args)) end, name or "main")
    local p = processes[pid]
    
    while p and not p.dead do
        -- Pull a signal with timeout
        local sig = { computer.pullSignal(0.05) }
        resumeProcess(p, table.unpack(sig))
        
        -- Tick any threads
        if p.threads then
            for i = #p.threads, 1, -1 do
                local t = p.threads[i]
                if coroutine.status(t) ~= "dead" then
                    local ok, err = coroutine.resume(t, table.unpack(sig))
                    if not ok then
                        io.stderr:write("Thread error: " .. tostring(err) .. "\n")
                        table.remove(p.threads, i)
                    end
                else
                    table.remove(p.threads, i)
                end
            end
        end
    end
    
    local exitCode = p and p.exitCode or 0
    processes[pid] = nil
    return exitCode
end

-- Exit current process
function process.exit(code)
    if not current then return end
    local p = processes[current]
    if p then
        p.dead = true
        p.exitCode = code or 0
    end
    coroutine.yield()
end

return process
