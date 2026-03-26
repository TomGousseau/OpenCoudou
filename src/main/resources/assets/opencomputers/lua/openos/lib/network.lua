-- network.lua - Network utility library
-- Wraps modem components for easier networking

local network = {}

local serialization = require("serialization")
local event         = require("event")

-- Default network port
network.DEFAULT_PORT = 1

-- -----------------------------------------------------------------------
-- Low-level modem helpers
-- -----------------------------------------------------------------------

--- Get the primary modem address
function network.modem()
    return component.list("modem")()
end

--- Open a port on the primary modem
function network.open(port)
    local addr = network.modem()
    if not addr then return false, "no modem" end
    return pcall(component.invoke, addr, "open", port or network.DEFAULT_PORT)
end

--- Close a port
function network.close(port)
    local addr = network.modem()
    if not addr then return false, "no modem" end
    return pcall(component.invoke, addr, "close", port or network.DEFAULT_PORT)
end

--- Broadcast a raw message
function network.broadcast(port, msg)
    local addr = network.modem()
    if not addr then return false, "no modem" end
    return pcall(component.invoke, addr, "broadcast", port or network.DEFAULT_PORT, msg)
end

--- Send a message to a specific address
function network.send(dest, port, msg)
    local addr = network.modem()
    if not addr then return false, "no modem" end
    return pcall(component.invoke, addr, "send", dest, port or network.DEFAULT_PORT, msg)
end

--- Get this computer's modem address
function network.address()
    return network.modem()
end

--- Check if the modem is wireless
function network.isWireless()
    local addr = network.modem()
    if not addr then return false end
    local ok, result = pcall(component.invoke, addr, "isWireless")
    return ok and result
end

--- Get/set wireless strength
function network.strength(value)
    local addr = network.modem()
    if not addr then return nil end
    if value then
        pcall(component.invoke, addr, "setStrength", value)
    else
        local ok, result = pcall(component.invoke, addr, "getStrength")
        return ok and result or nil
    end
end

-- -----------------------------------------------------------------------
-- Serialized messaging
-- -----------------------------------------------------------------------

--- Send a serialized packet {type, data, from}
function network.sendPacket(dest, port, ptype, data)
    local pkt = serialization.serialize({
        type = ptype,
        data = data,
        from = computer.address(),
        time = computer.uptime(),
    })
    return network.send(dest, port, pkt)
end

--- Broadcast a serialized packet
function network.broadcastPacket(port, ptype, data)
    local pkt = serialization.serialize({
        type = ptype,
        data = data,
        from = computer.address(),
        time = computer.uptime(),
    })
    return network.broadcast(port, pkt)
end

--- Receive a packet and deserialize it
--- Returns packet table or nil, plus raw event components
function network.receive(port, timeout)
    local ev = { event.pull(timeout or math.huge, "modem_message") }
    if not ev[1] then return nil end
    
    -- ev: name, localAddr, remoteAddr, port, dist, msg
    local recv = {
        localAddr  = ev[2],
        remoteAddr = ev[3],
        port       = ev[4],
        distance   = ev[5],
        raw        = ev[6],
    }
    
    -- Try to deserialize
    if recv.raw then
        local ok, pkt = pcall(serialization.unserialize, recv.raw)
        if ok and type(pkt) == "table" then
            recv.packet = pkt
        end
    end
    
    return recv
end

--- Wait for a specific packet type
function network.waitFor(port, ptype, timeout)
    local deadline = timeout and (computer.uptime() + timeout)
    
    while true do
        local remaining = deadline and math.max(0, deadline - computer.uptime())
        if remaining == 0 then return nil, "timeout" end
        
        local recv = network.receive(port, remaining or math.huge)
        if not recv then return nil, "timeout" end
        
        if recv.packet and recv.packet.type == ptype then
            return recv
        end
    end
end

-- -----------------------------------------------------------------------
-- RPC (Remote Procedure Call)
-- -----------------------------------------------------------------------

local pendingCalls = {}
local rpcListeners = {}
local rpcPort = 42

--- Register a function callable remotely
function network.register(name, fn)
    rpcListeners[name] = fn
end

--- Unregister an RPC function
function network.unregister(name)
    rpcListeners[name] = nil
end

--- Handle incoming RPC messages (call this from your event loop)
function network.handleRPC(recv)
    if not recv or not recv.packet then return end
    local pkt = recv.packet
    
    if pkt.type == "rpc_call" then
        local fn = rpcListeners[pkt.data.name]
        local result, err
        if fn then
            local ok, res = pcall(fn, table.unpack(pkt.data.args or {}))
            if ok then
                result = res
            else
                err = tostring(res)
            end
        else
            err = "no such function: " .. tostring(pkt.data.name)
        end
        
        -- Send response
        network.sendPacket(recv.remoteAddr, recv.port, "rpc_result", {
            id     = pkt.data.id,
            result = result,
            error  = err,
        })
        
    elseif pkt.type == "rpc_result" then
        local id = pkt.data and pkt.data.id
        if id and pendingCalls[id] then
            pendingCalls[id](pkt.data.result, pkt.data.error)
            pendingCalls[id] = nil
        end
    end
end

--- Call a remote function
--- cb(result, error) is called when the response arrives
function network.call(dest, name, args, cb, timeout)
    local id = tostring(math.random(1, 2^32))
    pendingCalls[id] = cb or function() end
    
    network.sendPacket(dest, rpcPort, "rpc_call", {
        id   = id,
        name = name,
        args = args or {},
    })
    
    -- Wait synchronously if no callback
    if not cb then
        local deadline = computer.uptime() + (timeout or 5)
        while pendingCalls[id] and computer.uptime() < deadline do
            local recv = network.receive(rpcPort, 0.1)
            if recv then network.handleRPC(recv) end
        end
        return not pendingCalls[id]
    end
    
    return true
end

-- -----------------------------------------------------------------------
-- Ping utility
-- -----------------------------------------------------------------------

local PING_PORT = 7
local pingCallbacks = {}

--- Ping a remote address
--- Returns round-trip time in seconds, or nil on timeout
function network.ping(dest, timeout)
    network.open(PING_PORT)
    local id = tostring(math.random(1, 2^32))
    local sent = computer.uptime()
    
    network.sendPacket(dest, PING_PORT, "ping", { id = id })
    
    local deadline = computer.uptime() + (timeout or 2)
    while computer.uptime() < deadline do
        local recv = network.receive(PING_PORT, deadline - computer.uptime())
        if recv and recv.packet then
            if recv.packet.type == "ping" and recv.remoteAddr == dest then
                -- Auto-reply to pings
                network.sendPacket(recv.remoteAddr, PING_PORT, "pong", { id = recv.packet.data.id })
            elseif recv.packet.type == "pong" and recv.remoteAddr == dest then
                if recv.packet.data.id == id then
                    local rtt = computer.uptime() - sent
                    network.close(PING_PORT)
                    return rtt
                end
            end
        end
    end
    
    network.close(PING_PORT)
    return nil, "timeout"
end

--- Start listening for pings and auto-responding
function network.startPingResponder()
    network.open(PING_PORT)
    event.listen("modem_message", function(_, localAddr, remoteAddr, port, dist, msg)
        if port ~= PING_PORT then return end
        local ok, pkt = pcall(serialization.unserialize, msg)
        if ok and type(pkt) == "table" and pkt.type == "ping" then
            network.sendPacket(remoteAddr, PING_PORT, "pong", { id = pkt.data.id })
        end
    end)
end

-- -----------------------------------------------------------------------
-- Network discovery
-- -----------------------------------------------------------------------

--- Discover nearby computers by broadcasting
--- Returns list of { addr, name, uptime } within timeout seconds
function network.discover(port, timeout)
    port = port or 255
    timeout = timeout or 2
    network.open(port)
    
    local found = {}
    local myAddr = computer.address()
    
    -- Broadcast discovery request
    network.broadcastPacket(port, "discover", {
        name   = require("computer").getHostname(),
        uptime = computer.uptime(),
    })
    
    -- Listen for responses
    local deadline = computer.uptime() + timeout
    while computer.uptime() < deadline do
        local recv = network.receive(port, deadline - computer.uptime())
        if recv and recv.packet then
            local pkt = recv.packet
            if pkt.type == "discover_reply" and recv.remoteAddr ~= myAddr then
                found[recv.remoteAddr] = {
                    addr     = recv.remoteAddr,
                    name     = pkt.data.name or recv.remoteAddr:sub(1, 8),
                    uptime   = pkt.data.uptime,
                    distance = recv.distance,
                }
            elseif pkt.type == "discover" and recv.remoteAddr ~= myAddr then
                -- Reply to their discovery
                network.sendPacket(recv.remoteAddr, port, "discover_reply", {
                    name   = require("computer").getHostname(),
                    uptime = computer.uptime(),
                })
            end
        end
    end
    
    network.close(port)
    
    local result = {}
    for _, v in pairs(found) do
        table.insert(result, v)
    end
    table.sort(result, function(a, b)
        return (a.distance or 999) < (b.distance or 999)
    end)
    
    return result
end

return network
