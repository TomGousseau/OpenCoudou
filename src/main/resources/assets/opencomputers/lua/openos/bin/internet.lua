-- internet.lua - Internet component tools (download, pastebin, HTTP)
local component = component
local term = require("term")
local fs = require("filesystem")
local shell = require("shell")
local text = require("text")

local args = { ... }

local function getInternet()
    local addr = component.list("internet")()
    if not addr then
        io.stderr:write("internet: no internet card found\n")
        os.exit(1)
    end
    return addr
end

local function usage()
    print("Usage: internet <command> [args]")
    print("Commands:")
    print("  get <url> [file]     - download URL to file or stdout")
    print("  post <url> <data>    - POST data to URL")
    print("  head <url>           - show response headers")
end

local cmd = args[1]

if not cmd or cmd == "help" or cmd == "--help" then
    usage()
    return
end

local addr = getInternet()

if cmd == "get" then
    local url = args[2]
    local outFile = args[3]
    
    if not url then
        io.stderr:write("internet get: URL required\n")
        os.exit(1)
    end
    
    term.setForeground(0x88BBFF)
    io.write("Downloading: " .. url .. "\n")
    term.setForeground(0xFFFFFF)
    
    local reqId = component.invoke(addr, "request", url)
    if not reqId then
        io.stderr:write("internet: request failed\n")
        os.exit(1)
    end
    
    -- Wait for response
    local timeout = computer.uptime() + 10
    local responseData = ""
    
    while computer.uptime() < timeout do
        local sig, id, data, err = computer.pullSignal(0.1)
        if sig == "internet_response" and id == reqId then
            if err then
                io.stderr:write("internet: " .. tostring(err) .. "\n")
                os.exit(1)
            end
            if data then
                responseData = responseData .. data
            else
                -- nil data = end of response
                break
            end
        end
    end
    
    if outFile then
        local ok, err = fs.writeAll(shell.resolve(outFile), responseData)
        if ok then
            print("Saved to: " .. shell.resolve(outFile) .. " (" .. #responseData .. " bytes)")
        else
            io.stderr:write("internet: cannot write to " .. outFile .. ": " .. tostring(err) .. "\n")
        end
    else
        io.write(responseData)
    end
    
elseif cmd == "post" then
    local url = args[2]
    local data = args[3]
    
    if not url or not data then
        io.stderr:write("internet post: URL and data required\n")
        os.exit(1)
    end
    
    local reqId = component.invoke(addr, "request", url, data, {["Content-Type"] = "application/x-www-form-urlencoded"})
    
    local timeout = computer.uptime() + 10
    local responseData = ""
    
    while computer.uptime() < timeout do
        local sig, id, chunk, err = computer.pullSignal(0.1)
        if sig == "internet_response" and id == reqId then
            if err then
                io.stderr:write("internet: " .. tostring(err) .. "\n")
                os.exit(1)
            end
            if chunk then
                responseData = responseData .. chunk
            else
                break
            end
        end
    end
    
    io.write(responseData)
    
elseif cmd == "head" then
    local url = args[2]
    if not url then
        io.stderr:write("internet head: URL required\n")
        os.exit(1)
    end
    
    local reqId = component.invoke(addr, "request", url, nil, {}, "HEAD")
    
    local timeout = computer.uptime() + 5
    while computer.uptime() < timeout do
        local sig, id, data, err = computer.pullSignal(0.1)
        if sig == "internet_response" and id == reqId then
            if err then
                io.stderr:write("internet: " .. tostring(err) .. "\n")
            else
                if data then print(data) end
            end
            if not data then break end
        end
    end
    
else
    io.stderr:write("internet: unknown command '" .. cmd .. "'\n")
    usage()
    os.exit(1)
end
