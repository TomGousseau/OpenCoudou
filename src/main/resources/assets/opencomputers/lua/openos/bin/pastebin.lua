-- pastebin.lua - Pastebin integration via internet card
-- Usage: pastebin get <paste-id> [filename]
--        pastebin put <filename>
--        pastebin run <paste-id> [args...]

local shell = require("shell")
local fs    = require("filesystem")
local term  = require("term")

local PASTEBIN_RAW = "https://pastebin.com/raw/"
local PASTEBIN_API = "https://pastebin.com/api/api_post.php"

local function getInternet()
    local addr = component.list("internet")()
    if not addr then
        io.stderr:write("pastebin: no internet card found\n")
        return nil
    end
    return addr
end

local function httpGet(addr, url)
    local reqId = component.invoke(addr, "request", url)
    if not reqId then
        return nil, "failed to start request"
    end
    
    local response = ""
    local deadline = computer.uptime() + 10
    
    while computer.uptime() < deadline do
        local ok, chunk = pcall(component.invoke, addr, "read", reqId)
        if ok and chunk then
            response = response .. chunk
        elseif ok and chunk == nil then
            -- EOF
            break
        else
            return nil, tostring(chunk)
        end
        os.sleep(0.05)
    end
    
    component.invoke(addr, "close", reqId)
    return response
end

local function httpPost(addr, url, data)
    local headers = { ["Content-Type"] = "application/x-www-form-urlencoded" }
    local reqId = component.invoke(addr, "request", url, data, headers, "POST")
    if not reqId then
        return nil, "failed to start request"
    end
    
    local response = ""
    local deadline = computer.uptime() + 15
    
    while computer.uptime() < deadline do
        local ok, chunk = pcall(component.invoke, addr, "read", reqId)
        if ok and chunk then
            response = response .. chunk
        elseif ok and chunk == nil then
            break
        else
            return nil, tostring(chunk)
        end
        os.sleep(0.05)
    end
    
    component.invoke(addr, "close", reqId)
    return response
end

-- URL encode a string
local function urlEncode(s)
    return s:gsub("([^%w%-%.%_%~ ])", function(c)
        return string.format("%%%02X", c:byte())
    end):gsub(" ", "+")
end

-- -------------------------------------------------------------------------

local function cmdGet(addr, pasteId, filename)
    if not pasteId then
        io.stderr:write("Usage: pastebin get <paste-id> [filename]\n")
        return 1
    end
    
    filename = filename or pasteId .. ".lua"
    
    -- Make absolute
    if not filename:match("^/") then
        filename = shell.getWorkingDirectory():gsub("/$","") .. "/" .. filename
    end
    
    if fs.exists(filename) then
        term.write("File already exists. Overwrite? [y/N] ")
        local ans = term.read()
        if not ans or ans:lower() ~= "y" then
            print("Cancelled.")
            return 0
        end
    end
    
    io.write("Downloading " .. pasteId .. "... ")
    local url = PASTEBIN_RAW .. pasteId
    local content, err = httpGet(addr, url)
    
    if not content then
        io.stderr:write("\nError: " .. (err or "unknown") .. "\n")
        return 1
    end
    
    if content:match("^<!DOCTYPE") or content:match("^<html") then
        io.stderr:write("\nPaste not found or access denied.\n")
        return 1
    end
    
    local ok, werr = fs.writeAll(filename, content)
    if not ok then
        io.stderr:write("\nFailed to write file: " .. tostring(werr) .. "\n")
        return 1
    end
    
    print("saved to " .. filename .. " (" .. #content .. " bytes)")
    return 0
end

local function cmdPut(addr, filename)
    if not filename then
        io.stderr:write("Usage: pastebin put <filename>\n")
        return 1
    end
    
    if not filename:match("^/") then
        filename = shell.getWorkingDirectory():gsub("/$","") .. "/" .. filename
    end
    
    if not fs.exists(filename) then
        io.stderr:write("File not found: " .. filename .. "\n")
        return 1
    end
    
    local content, err = fs.readAll(filename)
    if not content then
        io.stderr:write("Could not read file: " .. tostring(err) .. "\n")
        return 1
    end
    
    -- Read API key if available
    local apiKey = ""
    if fs.exists("/etc/pastebin.key") then
        apiKey = (fs.readAll("/etc/pastebin.key") or ""):gsub("%s+", "")
    end
    
    local basename = filename:match("[^/]+$") or filename
    
    io.write("Uploading " .. basename .. " (" .. #content .. " bytes)... ")
    
    local postData = table.concat({
        "api_dev_key="         .. urlEncode(apiKey ~= "" and apiKey or "guest"),
        "api_option=paste",
        "api_paste_code="      .. urlEncode(content),
        "api_paste_name="      .. urlEncode(basename),
        "api_paste_format=lua",
        "api_paste_expire_date=1W",
    }, "&")
    
    local response, rerr = httpPost(addr, PASTEBIN_API, postData)
    
    if not response then
        io.stderr:write("\nError: " .. tostring(rerr) .. "\n")
        return 1
    end
    
    -- Pastebin returns the URL on success, or "Bad API request, ..." on error
    if response:match("^Bad API") then
        -- Fall back to anonymous paste URL format
        io.stderr:write("\nAPI error: " .. response .. "\n")
        io.stderr:write("(Set your API key in /etc/pastebin.key for authenticated pastes)\n")
        return 1
    end
    
    print("uploaded: " .. response)
    return 0
end

local function cmdRun(addr, pasteId, ...)
    if not pasteId then
        io.stderr:write("Usage: pastebin run <paste-id> [args...]\n")
        return 1
    end
    
    io.write("Downloading " .. pasteId .. "... ")
    local url = PASTEBIN_RAW .. pasteId
    local content, err = httpGet(addr, url)
    
    if not content then
        io.stderr:write("\nError: " .. (err or "unknown") .. "\n")
        return 1
    end
    
    if content:match("^<!DOCTYPE") or content:match("^<html") then
        io.stderr:write("\nPaste not found.\n")
        return 1
    end
    
    print("ok (" .. #content .. " bytes)")
    
    -- Load and run in a sandbox
    local fn, loadErr = load(content, "=" .. pasteId, "t", setmetatable({}, { __index = _G }))
    if not fn then
        io.stderr:write("Syntax error: " .. tostring(loadErr) .. "\n")
        return 1
    end
    
    local ok2, runErr = pcall(fn, ...)
    if not ok2 then
        io.stderr:write("Runtime error: " .. tostring(runErr) .. "\n")
        return 1
    end
    
    return 0
end

-- -------------------------------------------------------------------------
-- Main
-- -------------------------------------------------------------------------
local args = { ... }
local command = args[1]

if not command then
    io.stderr:write("Usage: pastebin <get|put|run> ...\n")
    return 1
end

local addr = getInternet()
if not addr then return 1 end

if command == "get" then
    return cmdGet(addr, args[2], args[3])
elseif command == "put" then
    return cmdPut(addr, args[2])
elseif command == "run" then
    local runArgs = {}
    for i = 3, #args do runArgs[#runArgs+1] = args[i] end
    return cmdRun(addr, args[2], table.unpack(runArgs))
else
    io.stderr:write("Unknown command: " .. command .. "\n")
    io.stderr:write("Usage: pastebin <get|put|run> ...\n")
    return 1
end
