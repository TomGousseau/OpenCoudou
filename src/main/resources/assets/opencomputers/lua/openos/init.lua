-- OpenOS init.lua - Main OS entry point
-- Boots the full operating system

local computer = computer
local component = component

-- Package system
local package = {}
package.loaded = {}
package.path = "/lib/?.lua;/lib/?/init.lua;/usr/lib/?.lua"
package.preload = {}

local function splits(str, sep)
    local parts = {}
    for part in str:gmatch("([^" .. sep .. "]+)") do
        parts[#parts + 1] = part
    end
    return parts
end

-- Boot filesystem
local bootfs = ...

-- Basic I/O before full init
local gpu = component.list("gpu")()
local screen = component.list("screen")()
if gpu and screen then
    component.invoke(gpu, "bind", screen)
end

local function rawprint(msg)
    if gpu then
        local w, h = component.invoke(gpu, "getResolution")
        component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
        component.invoke(gpu, "fill", 1, h, w, 1, " ")
        component.invoke(gpu, "set", 1, h, tostring(msg):sub(1, w))
    end
end

rawprint("OpenOS booting...")

-- Filesystem driver
local fs = {}

local mounts = {} -- path -> address

local function normalizePath(path)
    local parts = {}
    for part in path:gmatch("[^/\\]+") do
        if part == ".." then
            table.remove(parts)
        elseif part ~= "." then
            table.insert(parts, part)
        end
    end
    return "/" .. table.concat(parts, "/")
end

local function findMount(path)
    local normalized = normalizePath(path)
    local bestLen = -1
    local bestAddr = nil
    local bestMount = nil
    
    for mountPath, addr in pairs(mounts) do
        if normalized:sub(1, #mountPath) == mountPath or normalized == mountPath:gsub("/$","") then
            if #mountPath > bestLen then
                bestLen = #mountPath
                bestAddr = addr
                bestMount = mountPath
            end
        end
    end
    
    if not bestAddr then return nil, nil end
    
    local relative = normalized:sub(#bestMount + 1)
    if relative:sub(1,1) == "/" then relative = relative:sub(2) end
    return bestAddr, relative == "" and "/" or relative
end

function fs.mount(address, path)
    local normalized = normalizePath(path)
    if not normalized:find("/$") then normalized = normalized .. "/" end
    mounts[normalized] = address
    return true
end

function fs.unmount(path)
    local normalized = normalizePath(path)
    if not normalized:find("/$") then normalized = normalized .. "/" end
    mounts[normalized] = nil
end

function fs.exists(path)
    local addr, rel = findMount(path)
    if not addr then return false end
    return component.invoke(addr, "exists", rel)
end

function fs.isDirectory(path)
    local addr, rel = findMount(path)
    if not addr then return false end
    return component.invoke(addr, "isDirectory", rel)
end

function fs.isFile(path)
    return fs.exists(path) and not fs.isDirectory(path)
end

function fs.list(path)
    local addr, rel = findMount(path)
    if not addr then return nil, "no such directory" end
    return component.invoke(addr, "list", rel)
end

function fs.size(path)
    local addr, rel = findMount(path)
    if not addr then return 0 end
    return component.invoke(addr, "size", rel)
end

function fs.open(path, mode)
    local addr, rel = findMount(path)
    if not addr then return nil, "no such file or directory" end
    local handle, err = component.invoke(addr, "open", rel, mode or "r")
    if not handle then return nil, err end
    return {
        _addr = addr,
        _handle = handle,
        read = function(self, n)
            return component.invoke(self._addr, "read", self._handle, n or math.huge)
        end,
        write = function(self, data)
            return component.invoke(self._addr, "write", self._handle, data)
        end,
        seek = function(self, whence, offset)
            return component.invoke(self._addr, "seek", self._handle, whence, offset or 0)
        end,
        close = function(self)
            return component.invoke(self._addr, "close", self._handle)
        end
    }
end

function fs.read(path)
    local f, err = fs.open(path, "r")
    if not f then return nil, err end
    local chunks = {}
    repeat
        local chunk = f:read(4096)
        if chunk then chunks[#chunks+1] = chunk end
    until not chunk
    f:close()
    return table.concat(chunks)
end

function fs.write(path, data)
    local f, err = fs.open(path, "w")
    if not f then return false, err end
    local ok, werr = f:write(data)
    f:close()
    return ok, werr
end

function fs.makeDirectory(path)
    local addr, rel = findMount(path)
    if not addr then return false, "no mount" end
    return component.invoke(addr, "makeDirectory", rel)
end

function fs.remove(path)
    local addr, rel = findMount(path)
    if not addr then return false, "no mount" end
    return component.invoke(addr, "remove", rel)
end

function fs.rename(from, to)
    local fromAddr, fromRel = findMount(from)
    local toAddr, toRel = findMount(to)
    if not fromAddr then return false, "source not found" end
    if fromAddr == toAddr then
        return component.invoke(fromAddr, "rename", fromRel, toRel)
    end
    -- Cross-filesystem rename = copy + delete
    local data, err = fs.read(from)
    if not data then return false, err end
    local ok, werr = fs.write(to, data)
    if not ok then return false, werr end
    return fs.remove(from)
end

function fs.copy(from, to)
    local data, err = fs.read(from)
    if not data then return false, err end
    return fs.write(to, data)
end

function fs.concat(...)
    local parts = {...}
    local result = ""
    for _, part in ipairs(parts) do
        if part:sub(1,1) == "/" then
            result = part
        else
            if result:sub(-1) ~= "/" then
                result = result .. "/"
            end
            result = result .. part
        end
    end
    return normalizePath(result)
end

function fs.path(path)
    local normalized = normalizePath(path)
    return normalized:match("^(.*)/[^/]*$") or "/"
end

function fs.name(path)
    return path:match("[^/\\]+$") or ""
end

function fs.extension(path)
    local name = fs.name(path)
    return name:match("%.([^%.]+)$")
end

function fs.getMounts()
    local result = {}
    for path, addr in pairs(mounts) do
        result[path] = addr
    end
    return result
end

-- Mount boot filesystem  
fs.mount(bootfs, "/")

-- Try to mount tmpfs
local tmpAddr = computer.tmpAddress()
if tmpAddr then
    fs.mount(tmpAddr, "/tmp")
end

rawprint("Filesystems mounted")

-- Load require system
local function loadfile(path, env)
    local code, err = fs.read(path)
    if not code then return nil, "failed to read " .. path .. ": " .. tostring(err) end
    local fn, err2 = load(code, "=" .. path, "bt", env or _G)
    if not fn then return nil, "failed to compile " .. path .. ": " .. tostring(err2) end
    return fn
end

function package.require(name)
    if package.loaded[name] ~= nil then
        return package.loaded[name]
    end
    
    if package.preload[name] then
        local result = package.preload[name](name)
        package.loaded[name] = result
        return result
    end
    
    -- Search package.path
    local errors = {}
    for pathTemplate in package.path:gmatch("[^;]+") do
        local filepath = pathTemplate:gsub("%?", name:gsub("%.", "/"))
        if fs.exists(filepath) then
            local fn, err = loadfile(filepath)
            if fn then
                local result = fn(name)
                if result == nil then result = true end
                package.loaded[name] = result
                return result
            else
                errors[#errors+1] = err
            end
        else
            errors[#errors+1] = "no file '" .. filepath .. "'"
        end
    end
    
    error("module '" .. name .. "' not found:\n" .. table.concat(errors, "\n"), 2)
end

_G.require = package.require
_G.package = package
_G.fs = fs

rawprint("Package system ready")

-- Load core libraries
local ok, err = pcall(function()
    -- Load event system first
    _G.event = require("event")
    
    -- Load process manager
    _G.process = require("process")
    
    -- Load io library
    _G.io = require("io")
    
    -- Load shell
    _G.shell = require("shell")
    
    -- Load term
    _G.term = require("term")
    
    rawprint("Core libraries loaded")
end)

if not ok then
    rawprint("Warning: " .. tostring(err))
end

-- Start shell
rawprint("Starting shell...")

local shellOk, shellErr = pcall(function()
    local sh = loadfile("/bin/sh.lua")
    if sh then
        sh()
    else
        -- Minimal shell fallback
        local running = true
        local cwd = "/"
        
        while running do
            local w = term and term.getCursor and term.getCursor() or 1
            if term then
                term.write(cwd .. "$ ")
            end
            
            local input = ""
            while true do
                local sig, _, char, code = computer.pullSignal()
                if sig == "key_down" then
                    if code == 28 then -- Enter
                        break
                    elseif code == 14 then -- Backspace
                        if #input > 0 then
                            input = input:sub(1, -2)
                            if term then term.write("\b \b") end
                        end
                    elseif char >= 32 then
                        input = input .. string.char(char)
                        if term then term.write(string.char(char)) end
                    end
                end
            end
            
            if term then term.write("\n") end
            
            -- Parse and execute command
            local args = {}
            for arg in input:gmatch("%S+") do
                args[#args+1] = arg
            end
            
            if #args > 0 then
                local cmd = args[1]
                if cmd == "exit" then
                    running = false
                elseif cmd == "ls" then
                    local path = args[2] or cwd
                    local list = fs.list(path)
                    if list then
                        for _, entry in ipairs(list) do
                            if term then term.write(entry .. "  ") end
                        end
                        if term then term.write("\n") end
                    end
                elseif cmd == "cd" then
                    local target = args[2] or "/"
                    if not target:find("^/") then
                        target = cwd .. "/" .. target
                    end
                    target = normalizePath(target)
                    if fs.isDirectory(target) then
                        cwd = target
                    else
                        if term then term.write("Not a directory: " .. target .. "\n") end
                    end
                elseif cmd == "cat" then
                    if args[2] then
                        local data, err = fs.read(args[2])
                        if data then
                            if term then term.write(data .. "\n") end
                        else
                            if term then term.write("Error: " .. tostring(err) .. "\n") end
                        end
                    end
                elseif cmd == "reboot" then
                    computer.shutdown(true)
                elseif cmd == "shutdown" then
                    computer.shutdown(false)
                else
                    -- Try to run as script
                    local scriptPath = cwd .. "/" .. cmd
                    if not fs.exists(scriptPath) then
                        scriptPath = "/bin/" .. cmd .. ".lua"
                    end
                    if fs.exists(scriptPath) then
                        local fn, err = loadfile(scriptPath)
                        if fn then
                            local ok, err = pcall(fn, table.unpack(args, 2))
                            if not ok and term then
                                term.write("Error: " .. tostring(err) .. "\n")
                            end
                        else
                            if term then term.write("Load error: " .. tostring(err) .. "\n") end
                        end
                    else
                        if term then term.write("Command not found: " .. cmd .. "\n") end
                    end
                end
            end
        end
    end
end)

if not shellOk then
    rawprint("Shell error: " .. tostring(shellErr))
end

computer.shutdown(false)
