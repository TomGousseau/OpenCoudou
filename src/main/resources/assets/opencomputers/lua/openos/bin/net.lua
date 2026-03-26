-- net.lua - Network configuration and diagnostics
-- Usage: net                    -> show current network status
--        net list               -> list modems
--        net open <port>        -> open a port
--        net close <port>       -> close a port
--        net send <addr> <port> <msg>  -> send raw message

local shell   = require("shell")
local network = require("network")

local args = { ... }
local subcmd = args[1] or "status"

local function showStatus()
    print("=== Network Status ===")
    print("")
    
    local modems = {}
    for addr, t in component.list("modem") do
        table.insert(modems, { addr = addr, type = t })
    end
    
    if #modems == 0 then
        print("No modems installed.")
        return
    end
    
    for i, m in ipairs(modems) do
        local isWireless = false
        pcall(function()
            isWireless = component.invoke(m.addr, "isWireless")
        end)
        
        print(string.format("Modem %d: %s", i, m.addr:sub(1, 8) .. "..."))
        print(string.format("  Type    : %s", isWireless and "Wireless" or "Wired"))
        
        if isWireless then
            local ok, str = pcall(component.invoke, m.addr, "getStrength")
            if ok then
                print(string.format("  Strength: %.1f", str))
            end
        end
        
        -- Max packet size
        local ok, ps = pcall(component.invoke, m.addr, "maxPacketSize")
        if ok then
            print(string.format("  Max Pkt : %d bytes", ps))
        end
        
        print("")
    end
    
    print("Computer address: " .. computer.address())
    print("Uptime          : " .. string.format("%.1f s", computer.uptime()))
end

if subcmd == "status" or subcmd == "" then
    showStatus()

elseif subcmd == "list" then
    for addr, _ in component.list("modem") do
        local isW = false
        pcall(function() isW = component.invoke(addr, "isWireless") end)
        print(string.format("%s  [%s]", addr, isW and "wireless" or "wired"))
    end

elseif subcmd == "open" then
    local port = tonumber(args[2])
    if not port then
        io.stderr:write("Usage: net open <port>\n")
        return 1
    end
    local ok, err = network.open(port)
    if ok then
        print("Opened port " .. port)
    else
        io.stderr:write("Failed to open port " .. port .. ": " .. tostring(err) .. "\n")
        return 1
    end

elseif subcmd == "close" then
    local port = tonumber(args[2])
    if not port then
        io.stderr:write("Usage: net close <port>\n")
        return 1
    end
    local ok, err = network.close(port)
    if ok then
        print("Closed port " .. port)
    else
        io.stderr:write("Failed: " .. tostring(err) .. "\n")
        return 1
    end

elseif subcmd == "send" then
    local dest = args[2]
    local port = tonumber(args[3])
    local msg  = table.concat(args, " ", 4)
    if not dest or not port or msg == "" then
        io.stderr:write("Usage: net send <address> <port> <message>\n")
        return 1
    end
    network.open(port)
    network.send(dest, port, msg)
    print("Sent to " .. dest:sub(1, 8) .. " on port " .. port)

else
    io.stderr:write("Unknown subcommand: " .. subcmd .. "\n")
    io.stderr:write("Usage: net [status|list|open|close|send]\n")
    return 1
end

return 0
