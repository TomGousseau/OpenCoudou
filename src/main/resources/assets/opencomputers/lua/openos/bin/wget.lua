-- wget.lua - Download files from the internet
local shell = require("shell")
local fs = require("filesystem")
local term = require("term")
local text = require("text")

local args = { ... }
local quiet = false
local output = nil
local urls = {}

for i, a in ipairs(args) do
    if a == "-q" or a == "--quiet" then
        quiet = true
    elseif a == "-O" and args[i+1] then
        output = args[i+1]
    elseif a:sub(1,2) == "-O" and #a > 2 then
        output = a:sub(3)
    elseif a:sub(1,1) ~= "-" and a ~= output then
        table.insert(urls, a)
    end
end

if #urls == 0 then
    io.stderr:write("usage: wget [-q] [-O outfile] <url> [url2 ...]\n")
    os.exit(1)
end

local internetAddr = component.list("internet")()
if not internetAddr then
    io.stderr:write("wget: no internet card\n")
    os.exit(1)
end

for _, url in ipairs(urls) do
    local outPath = output or fs.name(url:match("[^/]+$") or "index.html")
    outPath = shell.resolve(outPath)
    
    if not quiet then
        term.setForeground(0x88BBFF)
        io.write("=> " .. url .. "\n")
        term.setForeground(0xFFFFFF)
    end
    
    local reqId = component.invoke(internetAddr, "request", url)
    if not reqId then
        io.stderr:write("wget: request failed for " .. url .. "\n")
        goto continue
    end
    
    local timeout = computer.uptime() + 15
    local data = ""
    local headersDone = false
    local statusCode = 0
    local totalBytes = 0
    
    while computer.uptime() < timeout do
        local sig, id, chunk, err = computer.pullSignal(0.1)
        
        if sig == "internet_response" and id == reqId then
            if err then
                io.stderr:write("wget: error: " .. tostring(err) .. "\n")
                break
            end
            if chunk == nil then
                -- Done
                break
            end
            
            -- Parse status line from first chunk
            if not headersDone then
                local statusLine = chunk:match("^HTTP/%d%.%d (%d+)")
                if statusLine then
                    statusCode = tonumber(statusLine) or 0
                end
                -- Find end of headers
                local headerEnd = chunk:find("\r\n\r\n")
                if headerEnd then
                    data = chunk:sub(headerEnd + 4)
                    headersDone = true
                end
            else
                data = data .. chunk
            end
            
            totalBytes = #data
        end
    end
    
    if #data > 0 then
        local ok, err2 = fs.writeAll(outPath, data)
        if ok then
            if not quiet then
                print(string.format("Saved: %s (%d bytes)", outPath, totalBytes))
            end
        else
            io.stderr:write("wget: cannot write " .. outPath .. ": " .. tostring(err2) .. "\n")
        end
    elseif statusCode ~= 0 and statusCode >= 400 then
        io.stderr:write(string.format("wget: HTTP %d for %s\n", statusCode, url))
    else
        io.stderr:write("wget: no data received for " .. url .. "\n")
    end
    
    ::continue::
end
