-- filesync.lua - File synchronization utility over network
-- Syncs files/directories between two computers using modems
-- Usage: filesync server <dir> [port]       -> start a sync server
--        filesync push   <dir> <addr> [port] -> push local dir to remote
--        filesync pull   <dir> <addr> [port] -> pull remote dir to local

local shell    = require("shell")
local fs       = require("filesystem")
local network  = require("network")
local serialization = require("serialization")
local event    = require("event")

local DEFAULT_PORT = 4444
local BLOCK_SIZE   = 4096
local TIMEOUT      = 10

local args = { ... }
local subcmd = args[1]

-- -----------------------------------------------------------------------
-- Shared helpers
-- -----------------------------------------------------------------------

local function walkDir(dir)
    local files = {}
    local function walk(path)
        if fs.isDirectory(path) then
            for _, name in ipairs(fs.list(path) or {}) do
                if name ~= "." and name ~= ".." then
                    walk(path:gsub("/$","") .. "/" .. name)
                end
            end
        else
            local size = fs.size and fs.size(path) or 0
            local mtime = fs.lastModified and fs.lastModified(path) or 0
            table.insert(files, {
                path  = path,
                size  = size,
                mtime = mtime,
            })
        end
    end
    walk(dir)
    return files
end

local function fileChecksum(path)
    local content = fs.readAll(path)
    if not content then return nil end
    -- Simple checksum: sum of all byte values mod 2^32
    local sum = 0
    for i = 1, #content do
        sum = (sum + content:byte(i)) % (2^32)
    end
    return sum
end

-- -----------------------------------------------------------------------
-- Protocol
-- -----------------------------------------------------------------------
-- All packets: { cmd, data, seq }
-- Commands:
--   LIST_REQ  -> request file listing of a path
--   LIST_RESP -> response: { files: [{path, size, mtime, csum}] }
--   FETCH_REQ -> request file chunks: { path, offset, length }
--   FETCH_RESP-> response: { path, offset, data, eof }
--   PUSH_START-> start sending a file: { path, size }
--   PUSH_CHUNK-> file chunk: { path, offset, data, eof }
--   PUSH_ACK  -> acknowledgement: { path }
--   ERROR     -> { msg }

local function makePacket(cmd, data, seq)
    return serialization.serialize({ cmd = cmd, data = data, seq = seq or 0 })
end

local function sendTo(modemAddr, destAddr, port, cmd, data, seq)
    component.invoke(modemAddr, "send", destAddr, port, makePacket(cmd, data, seq))
end

local function waitPkt(port, expectedCmd, timeout)
    local deadline = computer.uptime() + (timeout or TIMEOUT)
    while computer.uptime() < deadline do
        local ev = { event.pull(deadline - computer.uptime(), "modem_message") }
        if ev[1] == "modem_message" then
            local pkt = serialization.unserialize(ev[6] or "")
            if type(pkt) == "table" and (not expectedCmd or pkt.cmd == expectedCmd) then
                return pkt, ev[3] -- pkt, senderAddr
            end
        end
    end
    return nil, nil
end

-- -----------------------------------------------------------------------
-- Server mode
-- -----------------------------------------------------------------------

local function runServer(rootDir, port)
    port = tonumber(port) or DEFAULT_PORT
    
    local modemAddr = network.modem()
    if not modemAddr then
        io.stderr:write("filesync: no modem found\n")
        return 1
    end
    
    component.invoke(modemAddr, "open", port)
    print(string.format("filesync server running on port %d, serving %s", port, rootDir))
    print("Press Ctrl+C to stop.")
    
    local running = true
    event.listen("interrupted", function() running = false end)
    
    while running do
        local pkt, senderAddr = waitPkt(port, nil, 60)
        if not pkt then goto continue end
        
        if pkt.cmd == "LIST_REQ" then
            local dir   = rootDir .. (pkt.data.path or "")
            local files = walkDir(dir)
            -- Add checksums
            for _, f in ipairs(files) do
                f.csum = fileChecksum(f.path)
                -- Make paths relative to rootDir
                f.relpath = f.path:sub(#rootDir + 1)
            end
            sendTo(modemAddr, senderAddr, port, "LIST_RESP", { files = files })
            
        elseif pkt.cmd == "FETCH_REQ" then
            local absPath = rootDir .. pkt.data.path
            if not fs.exists(absPath) then
                sendTo(modemAddr, senderAddr, port, "ERROR", { msg = "file not found: "..pkt.data.path })
            else
                local content = fs.readAll(absPath) or ""
                local offset  = pkt.data.offset or 0
                local length  = pkt.data.length or BLOCK_SIZE
                local chunk   = content:sub(offset + 1, offset + length)
                local eof     = (offset + length) >= #content
                sendTo(modemAddr, senderAddr, port, "FETCH_RESP", {
                    path   = pkt.data.path,
                    offset = offset,
                    data   = chunk,
                    eof    = eof,
                })
            end
            
        elseif pkt.cmd == "PUSH_START" then
            -- Client wants to push a file
            local absPath = rootDir .. pkt.data.path
            -- Ensure directory exists
            local dir = absPath:match("(.*)/[^/]*$")
            if dir then fs.makeDirectory(dir) end
            -- Acknowledge
            sendTo(modemAddr, senderAddr, port, "PUSH_ACK", { path = pkt.data.path })
            
        elseif pkt.cmd == "PUSH_CHUNK" then
            local absPath = rootDir .. pkt.data.path
            local existing = fs.exists(absPath) and fs.readAll(absPath) or ""
            local newContent
            if pkt.data.offset == 0 then
                newContent = pkt.data.data
            else
                newContent = existing .. pkt.data.data
            end
            fs.writeAll(absPath, newContent)
            
            if pkt.data.eof then
                sendTo(modemAddr, senderAddr, port, "PUSH_ACK", { path = pkt.data.path, done = true })
                print("  received: " .. pkt.data.path)
            end
        end
        
        ::continue::
    end
    
    component.invoke(modemAddr, "close", port)
    print("filesync server stopped")
    return 0
end

-- -----------------------------------------------------------------------
-- Push mode
-- -----------------------------------------------------------------------

local function runPush(localDir, remoteAddr, port)
    port = tonumber(port) or DEFAULT_PORT
    
    local modemAddr = network.modem()
    if not modemAddr then
        io.stderr:write("filesync: no modem found\n")
        return 1
    end
    
    component.invoke(modemAddr, "open", port)
    
    print("Scanning local directory: " .. localDir)
    local files = walkDir(localDir)
    print(string.format("Found %d files to send", #files))
    
    for i, f in ipairs(files) do
        local relpath = f.path:sub(#localDir + 1)
        io.write(string.format("[%d/%d] %s... ", i, #files, relpath))
        
        local content = fs.readAll(f.path) or ""
        
        -- Send start
        component.invoke(modemAddr, "send", remoteAddr, port,
            makePacket("PUSH_START", { path = relpath, size = #content }))
        
        -- Wait for ack
        local ack = waitPkt(port, "PUSH_ACK", TIMEOUT)
        if not ack then
            print("TIMEOUT")
            goto nextfile
        end
        
        -- Send chunks
        local offset = 0
        while offset < #content do
            local chunk   = content:sub(offset + 1, offset + BLOCK_SIZE)
            local eof     = (offset + BLOCK_SIZE) >= #content
            component.invoke(modemAddr, "send", remoteAddr, port,
                makePacket("PUSH_CHUNK", {
                    path   = relpath,
                    offset = offset,
                    data   = chunk,
                    eof    = eof,
                }))
            offset = offset + #chunk
            
            if eof then
                local done = waitPkt(port, "PUSH_ACK", TIMEOUT)
                if not done then io.write("(no final ack) ") end
            end
        end
        
        print("ok")
        ::nextfile::
    end
    
    component.invoke(modemAddr, "close", port)
    print("Push complete.")
    return 0
end

-- -----------------------------------------------------------------------
-- Pull mode
-- -----------------------------------------------------------------------

local function runPull(localDir, remoteAddr, port)
    port = tonumber(port) or DEFAULT_PORT
    
    local modemAddr = network.modem()
    if not modemAddr then
        io.stderr:write("filesync: no modem found\n")
        return 1
    end
    
    component.invoke(modemAddr, "open", port)
    
    print("Querying remote file list...")
    component.invoke(modemAddr, "send", remoteAddr, port,
        makePacket("LIST_REQ", { path = "" }))
    
    local resp = waitPkt(port, "LIST_RESP", TIMEOUT)
    if not resp then
        io.stderr:write("filesync: no response from server\n")
        component.invoke(modemAddr, "close", port)
        return 1
    end
    
    local remoteFiles = resp.data.files or {}
    print(string.format("Remote has %d files", #remoteFiles))
    
    local toFetch = {}
    for _, rf in ipairs(remoteFiles) do
        local localPath = localDir .. rf.relpath
        if not fs.exists(localPath) then
            table.insert(toFetch, rf)
        else
            -- Compare checksum
            local localCsum = fileChecksum(localPath)
            if localCsum ~= rf.csum then
                table.insert(toFetch, rf)
            end
        end
    end
    
    print(string.format("Need to fetch %d files", #toFetch))
    
    for i, rf in ipairs(toFetch) do
        io.write(string.format("[%d/%d] %s... ", i, #toFetch, rf.relpath))
        
        local content = ""
        local offset  = 0
        
        while true do
            component.invoke(modemAddr, "send", remoteAddr, port,
                makePacket("FETCH_REQ", {
                    path   = rf.relpath,
                    offset = offset,
                    length = BLOCK_SIZE,
                }))
            
            local chunk = waitPkt(port, "FETCH_RESP", TIMEOUT)
            if not chunk then
                print("TIMEOUT")
                break
            end
            
            content = content .. (chunk.data.data or "")
            
            if chunk.data.eof then
                -- Write file
                local absPath = localDir .. rf.relpath
                local dir = absPath:match("(.*)/[^/]*$")
                if dir then fs.makeDirectory(dir) end
                fs.writeAll(absPath, content)
                print("ok (" .. #content .. " bytes)")
                break
            end
            
            offset = offset + BLOCK_SIZE
        end
    end
    
    component.invoke(modemAddr, "close", port)
    print("Pull complete.")
    return 0
end

-- -----------------------------------------------------------------------
-- Main
-- -----------------------------------------------------------------------

if subcmd == "server" then
    return runServer(args[2] or "/", args[3])
elseif subcmd == "push" then
    if not args[2] or not args[3] then
        io.stderr:write("Usage: filesync push <localdir> <remoteaddr> [port]\n")
        return 1
    end
    return runPush(args[2], args[3], args[4])
elseif subcmd == "pull" then
    if not args[2] or not args[3] then
        io.stderr:write("Usage: filesync pull <localdir> <remoteaddr> [port]\n")
        return 1
    end
    return runPull(args[2], args[3], args[4])
else
    io.stderr:write("Usage: filesync <server|push|pull> ...\n")
    return 1
end
