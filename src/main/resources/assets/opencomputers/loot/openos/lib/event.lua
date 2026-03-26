-- Event Library
-- Provides event handling and timer functionality

local event = {}

local computer = computer
local listeners = {}
local timers = {}
local nextTimerId = 1

-- ========================================
-- Event Listening
-- ========================================

function event.listen(eventType, callback)
  if type(eventType) ~= "string" then
    error("event type must be a string")
  end
  if type(callback) ~= "function" then
    error("callback must be a function")
  end
  
  listeners[eventType] = listeners[eventType] or {}
  
  -- Check if already registered
  for _, cb in ipairs(listeners[eventType]) do
    if cb == callback then
      return false
    end
  end
  
  table.insert(listeners[eventType], callback)
  return true
end

function event.ignore(eventType, callback)
  if not listeners[eventType] then
    return false
  end
  
  for i, cb in ipairs(listeners[eventType]) do
    if cb == callback then
      table.remove(listeners[eventType], i)
      return true
    end
  end
  return false
end

-- ========================================
-- Timers
-- ========================================

function event.timer(interval, callback, times)
  times = times or math.huge
  
  local id = nextTimerId
  nextTimerId = nextTimerId + 1
  
  timers[id] = {
    interval = interval,
    callback = callback,
    times = times,
    lastTick = computer.uptime()
  }
  
  return id
end

function event.cancel(id)
  if timers[id] then
    timers[id] = nil
    return true
  end
  return false
end

-- ========================================
-- Event Processing
-- ========================================

local function processEvent(eventType, ...)
  local handlers = listeners[eventType]
  if handlers then
    for _, callback in ipairs(handlers) do
      local ok, err = pcall(callback, eventType, ...)
      if not ok then
        -- Log error but continue
      end
    end
  end
end

local function processTimers()
  local now = computer.uptime()
  local toRemove = {}
  
  for id, timer in pairs(timers) do
    if now - timer.lastTick >= timer.interval then
      local ok, err = pcall(timer.callback)
      timer.lastTick = now
      timer.times = timer.times - 1
      
      if timer.times <= 0 then
        table.insert(toRemove, id)
      end
    end
  end
  
  for _, id in ipairs(toRemove) do
    timers[id] = nil
  end
end

function event.pull(timeout, ...)
  local filter = {...}
  local deadline = timeout and (computer.uptime() + timeout) or math.huge
  
  while true do
    -- Check timers
    processTimers()
    
    -- Calculate wait time
    local minWait = deadline - computer.uptime()
    for _, timer in pairs(timers) do
      local waitTime = timer.interval - (computer.uptime() - timer.lastTick)
      if waitTime < minWait then
        minWait = waitTime
      end
    end
    
    if minWait < 0 then
      minWait = 0
    end
    
    -- Wait for signal
    local signal = {computer.pullSignal(minWait)}
    
    if signal[1] then
      -- Process listeners
      processEvent(table.unpack(signal))
      
      -- Check filter
      if #filter == 0 then
        return table.unpack(signal)
      end
      
      for _, f in ipairs(filter) do
        if signal[1] == f then
          return table.unpack(signal)
        end
      end
    end
    
    -- Check timeout
    if computer.uptime() >= deadline then
      return nil
    end
  end
end

-- Pull without timeout
function event.pullFiltered(filter, timeout)
  return event.pull(timeout, filter)
end

-- Push a custom event
function event.push(eventType, ...)
  processEvent(eventType, ...)
end

-- One-shot listener
function event.onError(callback)
  -- Store error handler
  event._errorHandler = callback
end

return event
