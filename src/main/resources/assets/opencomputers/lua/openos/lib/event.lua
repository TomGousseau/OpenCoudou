-- event.lua - Event system for OpenOS
-- Handles signals, timers, and event listeners

local computer = computer
local running = true

local event = {}

local listeners = {}
local timers = {}
local nextTimerId = 1
local timerCallbacks = {}

-- Pull a signal with optional timeout
function event.pull(timeout, ...)
    local filters = {...}
    local deadline = timeout and (computer.uptime() + timeout) or math.huge
    
    while true do
        local remaining = deadline - computer.uptime()
        if remaining < 0 and timeout then
            return nil
        end
        
        local sig = table.pack(computer.pullSignal(remaining))
        if sig.n > 0 then
            local name = sig[1]
            
            -- Check timers
            for id, timer in pairs(timers) do
                if computer.uptime() >= timer.deadline then
                    local cb = timerCallbacks[id]
                    timers[id] = nil
                    timerCallbacks[id] = nil
                    if cb then
                        local ok, err = pcall(cb)
                        if not ok then
                            event.onError(err)
                        end
                    end
                end
            end
            
            -- Dispatch to listeners
            local eventListeners = listeners[name]
            if eventListeners then
                for i = #eventListeners, 1, -1 do
                    local listener = eventListeners[i]
                    if listener then
                        local ok, result = pcall(listener, table.unpack(sig, 1, sig.n))
                        if not ok then
                            event.onError(result)
                        elseif result == false then
                            table.remove(eventListeners, i)
                        end
                    end
                end
            end
            
            -- Check internal listeners
            local internalListeners = listeners["*"]
            if internalListeners then
                for i = #internalListeners, 1, -1 do
                    local listener = internalListeners[i]
                    if listener then
                        local ok, result = pcall(listener, table.unpack(sig, 1, sig.n))
                        if not ok then
                            event.onError(result)
                        elseif result == false then
                            table.remove(internalListeners, i)
                        end
                    end
                end
            end
            
            -- Check filters
            if #filters == 0 then
                return table.unpack(sig, 1, sig.n)
            else
                for _, filter in ipairs(filters) do
                    if name == filter then
                        return table.unpack(sig, 1, sig.n)
                    end
                end
            end
        end
    end
end

-- Pull a signal, ignoring errors
function event.pullFiltered(timeout, predicate)
    local deadline = timeout and (computer.uptime() + timeout) or math.huge
    
    while true do
        local remaining = deadline - computer.uptime()
        if remaining < 0 and timeout then
            return nil
        end
        
        local sig = table.pack(computer.pullSignal(remaining))
        if sig.n > 0 then
            if not predicate or predicate(table.unpack(sig, 1, sig.n)) then
                return table.unpack(sig, 1, sig.n)
            end
        end
    end
end

-- Listen for events
function event.listen(name, callback)
    if not listeners[name] then
        listeners[name] = {}
    end
    -- Check not already registered
    for _, cb in ipairs(listeners[name]) do
        if cb == callback then return false end
    end
    table.insert(listeners[name], callback)
    return true
end

-- Stop listening
function event.ignore(name, callback)
    if not listeners[name] then return false end
    for i, cb in ipairs(listeners[name]) do
        if cb == callback then
            table.remove(listeners[name], i)
            return true
        end
    end
    return false
end

-- One-shot listener
function event.once(name, callback)
    local function wrapper(...)
        event.ignore(name, wrapper)
        return callback(...)
    end
    return event.listen(name, wrapper)
end

-- Timer functions
function event.timer(interval, callback, times)
    times = times or 1
    local id = nextTimerId
    nextTimerId = nextTimerId + 1
    
    local remaining = times
    
    local function schedule()
        timers[id] = {
            deadline = computer.uptime() + interval
        }
        timerCallbacks[id] = function()
            local ok, err = pcall(callback)
            if not ok then
                event.onError(err)
            end
            remaining = remaining - 1
            if remaining > 0 or times == math.huge then
                schedule()
            end
        end
    end
    
    schedule()
    return id
end

-- Cancel a timer
function event.cancel(id)
    if timers[id] then
        timers[id] = nil
        timerCallbacks[id] = nil
        return true
    end
    return false
end

-- Error handler
function event.onError(err)
    io.stderr:write("event error: " .. tostring(err) .. "\n")
end

-- Push a signal into the queue
function event.push(name, ...)
    computer.pushSignal(name, ...)
end

-- Check if event system should keep running
function event.shouldContinue()
    return running
end

function event.stop()
    running = false
end

return event
