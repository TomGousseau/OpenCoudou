-- OpenComputers Kernel
-- Provides basic OS functionality including multitasking and event handling

local kernel = {}

-- ========================================
-- Event System
-- ========================================

local listeners = {}
local timers = {}
local nextTimerId = 1

function kernel.listen(event, handler)
  listeners[event] = listeners[event] or {}
  table.insert(listeners[event], handler)
  return #listeners[event]
end

function kernel.ignore(event, handlerId)
  if listeners[event] and listeners[event][handlerId] then
    table.remove(listeners[event], handlerId)
    return true
  end
  return false
end

function kernel.fire(event, ...)
  local handlers = listeners[event]
  if handlers then
    for _, handler in ipairs(handlers) do
      local ok, err = pcall(handler, ...)
      if not ok then
        kernel.log("Event handler error: " .. tostring(err))
      end
    end
  end
end

function kernel.timer(interval, callback, times)
  local id = nextTimerId
  nextTimerId = nextTimerId + 1
  timers[id] = {
    interval = interval,
    callback = callback,
    times = times or 1,
    lastTick = computer.uptime()
  }
  return id
end

function kernel.cancelTimer(id)
  timers[id] = nil
end

-- ========================================
-- Process Management
-- ========================================

local processes = {}
local currentPid = 0
local nextPid = 1

function kernel.spawn(func, name, ...)
  local args = {...}
  local pid = nextPid
  nextPid = nextPid + 1
  
  local co = coroutine.create(function()
    return func(table.unpack(args))
  end)
  
  processes[pid] = {
    pid = pid,
    name = name or "unnamed",
    coroutine = co,
    status = "ready",
    parent = currentPid
  }
  
  return pid
end

function kernel.kill(pid)
  if processes[pid] then
    processes[pid].status = "dead"
    processes[pid] = nil
    return true
  end
  return false
end

function kernel.running()
  return currentPid
end

function kernel.processes()
  local result = {}
  for pid, proc in pairs(processes) do
    result[pid] = {
      pid = proc.pid,
      name = proc.name,
      status = proc.status
    }
  end
  return result
end

-- ========================================
-- Scheduler
-- ========================================

local function schedule()
  local deadline = computer.uptime() + 0.05 -- 50ms max per tick
  
  -- Update timers
  local now = computer.uptime()
  for id, timer in pairs(timers) do
    if now - timer.lastTick >= timer.interval then
      local ok, err = pcall(timer.callback)
      if not ok then
        kernel.log("Timer error: " .. tostring(err))
      end
      timer.lastTick = now
      timer.times = timer.times - 1
      if timer.times <= 0 then
        timers[id] = nil
      end
    end
  end
  
  -- Run processes
  for pid, proc in pairs(processes) do
    if proc.status == "ready" and computer.uptime() < deadline then
      currentPid = pid
      proc.status = "running"
      
      local ok, result = coroutine.resume(proc.coroutine)
      
      if not ok then
        kernel.log("Process " .. proc.name .. " error: " .. tostring(result))
        proc.status = "dead"
        processes[pid] = nil
      elseif coroutine.status(proc.coroutine) == "dead" then
        proc.status = "dead"
        processes[pid] = nil
      else
        proc.status = "ready"
      end
      
      currentPid = 0
    end
  end
end

-- ========================================
-- Logging
-- ========================================

local logBuffer = {}
local maxLogSize = 100

function kernel.log(message)
  table.insert(logBuffer, {
    time = computer.uptime(),
    message = tostring(message)
  })
  if #logBuffer > maxLogSize then
    table.remove(logBuffer, 1)
  end
end

function kernel.getLogs()
  return logBuffer
end

-- ========================================
-- Main Loop
-- ========================================

function kernel.run()
  kernel.log("Kernel started")
  
  while true do
    -- Pull events with timeout for scheduler
    local signal = {computer.pullSignal(0.05)}
    
    if signal[1] then
      -- Fire event to listeners
      kernel.fire(table.unpack(signal))
    end
    
    -- Run scheduler
    schedule()
    
    -- Yield to prevent timeout
    coroutine.yield()
  end
end

return kernel
