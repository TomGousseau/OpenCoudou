-- shell.lua - Shell library

local fs = require("filesystem")
local process = require("process")
local text = require("text")

local shell = {}

-- Environment
local _aliases = {}
local _path = "/bin:/usr/bin:/home/bin"

-- Get/set PATH
function shell.getPath()
    return process.getenv("PATH") or _path
end

function shell.setPath(path)
    process.setenv("PATH", path)
    _path = path
end

-- Get/set working directory
function shell.getWorkingDirectory()
    return process.getenv("PWD") or "/"
end

function shell.setWorkingDirectory(dir)
    dir = shell.resolve(dir)
    if not fs.isDirectory(dir) then
        return nil, "not a directory: " .. dir
    end
    process.setenv("PWD", dir)
    return dir
end

-- Resolve a path relative to cwd
function shell.resolve(path, ext)
    if path:sub(1, 1) == "/" then
        -- Absolute
        if ext and not fs.exists(path) then
            return shell.resolve(path .. "." .. ext)
        end
        return fs.canonical(path)
    end
    
    -- Relative
    local cwd = shell.getWorkingDirectory()
    local full = fs.concat(cwd, path)
    if ext and not fs.exists(full) then
        return fs.canonical(full .. "." .. ext)
    end
    return fs.canonical(full)
end

-- Find a command in PATH
function shell.which(name)
    if name:find("/") then
        return shell.resolve(name, "lua")
    end
    
    -- Check aliases first
    if _aliases[name] then
        return shell.which(_aliases[name])
    end
    
    local pathList = text.split(shell.getPath(), ":", true)
    for _, dir in ipairs(pathList) do
        local full = fs.concat(dir, name)
        if fs.exists(full) then
            return full
        end
        local withExt = full .. ".lua"
        if fs.exists(withExt) then
            return withExt
        end
    end
    
    return nil
end

-- Alias management
function shell.alias(name, command)
    if command then
        _aliases[name] = command
    end
    return _aliases[name]
end

function shell.unalias(name)
    _aliases[name] = nil
end

function shell.aliases()
    local result = {}
    for k, v in pairs(_aliases) do
        result[k] = v
    end
    return result
end

-- Parse a command string into command + args
function shell.parse(str)
    local tokens = text.tokenize(str)
    if #tokens == 0 then return nil, {} end
    
    -- Handle variable expansion
    local env = process.getenv() or {}
    for i, token in ipairs(tokens) do
        tokens[i] = token:gsub("%$([%w_]+)", function(var)
            return tostring(env[var] or "")
        end)
    end
    
    local command = tokens[1]
    table.remove(tokens, 1)
    return command, tokens
end

-- Execute a command
function shell.execute(command, ...)
    local args = { ... }
    
    -- Expand alias
    if _aliases[command] then
        local aliasTokens = text.tokenize(_aliases[command])
        command = aliasTokens[1]
        local aliasArgs = {}
        for i = 2, #aliasTokens do
            table.insert(aliasArgs, aliasTokens[i])
        end
        for _, a in ipairs(args) do
            table.insert(aliasArgs, a)
        end
        args = aliasArgs
    end
    
    local path = shell.which(command)
    if not path then
        io.stderr:write("command not found: " .. command .. "\n")
        return false, "command not found: " .. command
    end
    
    return shell.runFile(path, table.unpack(args))
end

-- Run a file with arguments
function shell.runFile(path, ...)
    local args = { ... }
    
    -- Read and compile the file
    local f, err = io.open(path, "r")
    if not f then
        return false, err or "cannot open: " .. path
    end
    
    local code = f:read("*a")
    f:close()
    
    local chunk, loadErr = load(code, "=" .. path, "t")
    if not chunk then
        return false, "syntax error in " .. path .. ": " .. tostring(loadErr)
    end
    
    -- Run in a new environment inheriting _ENV
    local env = setmetatable({}, { __index = _ENV })
    env.arg = { [0] = path, table.unpack(args) }
    
    local ok, result = xpcall(function()
        return chunk(table.unpack(args))
    end, function(err)
        return debug and debug.traceback and debug.traceback(err, 2) or err
    end)
    
    if not ok then
        io.stderr:write("error: " .. tostring(result) .. "\n")
        return false, result
    end
    
    return true, result
end

-- Run a command string (full parsing)
function shell.run(str)
    local command, args = shell.parse(str)
    if not command then return true end
    
    -- Handle shell redirections (basic: > and >>)
    local outFile = nil
    local append = false
    local filteredArgs = {}
    
    local i = 1
    while i <= #args do
        if args[i] == ">" or args[i] == ">>" then
            append = args[i] == ">>"
            outFile = args[i + 1]
            i = i + 2
        else
            table.insert(filteredArgs, args[i])
            i = i + 1
        end
    end
    
    if outFile then
        local mode = append and "a" or "w"
        local file, err = io.open(outFile, mode)
        if not file then
            io.stderr:write("cannot open " .. outFile .. ": " .. tostring(err) .. "\n")
            return false
        end
        local oldOutput = io.output()
        io.output(file)
        local ok, result = shell.execute(command, table.unpack(filteredArgs))
        io.output(oldOutput)
        file:close()
        return ok, result
    end
    
    return shell.execute(command, table.unpack(filteredArgs))
end

-- Tab completion
function shell.complete(partial)
    local results = {}
    
    -- If it has a slash, complete paths
    if partial:find("/") then
        local dir, base = partial:match("^(.*/)(.*)$")
        dir = dir or "/"
        base = base or ""
        local resolvedDir = shell.resolve(dir)
        
        if fs.isDirectory(resolvedDir) then
            local files, err = fs.list(resolvedDir)
            if files then
                for _, f in ipairs(files) do
                    if f:sub(1, #base) == base then
                        local full = partial .. f:sub(#base + 1)
                        if fs.isDirectory(fs.concat(resolvedDir, f)) then
                            full = full .. "/"
                        end
                        table.insert(results, full)
                    end
                end
            end
        end
    else
        -- Complete commands from PATH
        local pathList = text.split(shell.getPath(), ":", true)
        local seen = {}
        
        -- Check aliases
        for name, _ in pairs(_aliases) do
            if name:sub(1, #partial) == partial and not seen[name] then
                table.insert(results, name)
                seen[name] = true
            end
        end
        
        for _, dir in ipairs(pathList) do
            local files = fs.list(shell.resolve(dir))
            if files then
                for _, f in ipairs(files) do
                    local name = f:gsub("%.lua$", "")
                    if name:sub(1, #partial) == partial and not seen[name] then
                        table.insert(results, name)
                        seen[name] = true
                    end
                end
            end
        end
    end
    
    table.sort(results)
    return results
end

return shell
