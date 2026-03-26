-- ping.lua - Network ping utility
-- Usage: ping <address_or_hostname> [-c count] [-t timeout] [-a]
--   -c  number of pings to send (default: 4)
--   -t  timeout per ping in seconds (default: 2)
--   -a  broadcast ping (discover all nearby)

local shell   = require("shell")
local network = require("network")
local term    = require("term")

local args, opts = shell.parse(...)

local count   = tonumber(opts.c) or 4
local timeout = tonumber(opts.t) or 2

-- Broadcast discover mode
if opts.a then
    print("Discovering nearby computers...")
    local found = network.discover(255, timeout)
    if #found == 0 then
        print("No computers found.")
        return 1
    end
    print(string.format("Found %d computer(s):", #found))
    print(string.format("%-40s  %-6s  %s", "Address", "Dist", "Name"))
    print(string.rep("-", 60))
    for _, c in ipairs(found) do
        local dist = c.distance and string.format("%.1f", c.distance) or "?"
        print(string.format("%-40s  %-6s  %s", c.addr, dist, c.name or "?"))
    end
    return 0
end

-- Direct ping mode
local target = args[1]
if not target then
    io.stderr:write("Usage: ping <address> [-c count] [-t timeout]\n")
    io.stderr:write("       ping -a (discover all nearby computers)\n")
    return 1
end

-- Validate address format
if not target:match("^%x%x%x%x%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%x%x%x%x%x%x%x%x$") then
    -- Try short address prefix matching
    for addr, _ in component.list() do
        if addr:sub(1, #target) == target then
            target = addr
            break
        end
    end
end

print(string.format("PING %s:", target:sub(1, 8) .. "..."))

local sent     = 0
local received = 0
local minRtt   = math.huge
local maxRtt   = 0
local totalRtt = 0
local running  = true

-- Ctrl+C handler
event.listen("interrupted", function()
    running = false
end)

for i = 1, count do
    if not running then break end
    
    sent = sent + 1
    local rtt, err = network.ping(target, timeout)
    
    if rtt then
        received = received + 1
        local ms = math.floor(rtt * 1000)
        totalRtt = totalRtt + rtt
        if rtt < minRtt then minRtt = rtt end
        if rtt > maxRtt then maxRtt = rtt end
        print(string.format("Reply from %s: time=%dms", target:sub(1, 8), ms))
    else
        print(string.format("Request timeout for seq %d", i))
    end
    
    if i < count and running then
        os.sleep(1)
    end
end

-- Statistics
print("")
print(string.format("--- %s ping statistics ---", target:sub(1, 8)))
local loss = math.floor((sent - received) / sent * 100)
print(string.format("%d packets transmitted, %d received, %d%% packet loss", sent, received, loss))

if received > 0 then
    local avg = totalRtt / received
    print(string.format("rtt min/avg/max = %d/%d/%dms",
        math.floor(minRtt * 1000),
        math.floor(avg * 1000),
        math.floor(maxRtt * 1000)))
end

return received > 0 and 0 or 1
