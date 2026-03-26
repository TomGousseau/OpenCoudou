-- grep.lua - Search for patterns in files or stdin
local shell = require("shell")
local fs = require("filesystem")
local term = require("term")

local args = { ... }
local pattern = nil
local files = {}
local ignoreCase = false
local invertMatch = false
local countOnly = false
local lineNumbers = false
local fileNames = false
local quiet = false
local recursive = false

local i = 1
while i <= #args do
    local a = args[i]
    if a == "-i" or a == "--ignore-case" then
        ignoreCase = true
    elseif a == "-v" or a == "--invert-match" then
        invertMatch = true
    elseif a == "-c" or a == "--count" then
        countOnly = true
    elseif a == "-n" or a == "--line-number" then
        lineNumbers = true
    elseif a == "-l" or a == "--files-with-matches" then
        fileNames = true
    elseif a == "-q" or a == "--quiet" then
        quiet = true
    elseif a == "-r" or a == "-R" or a == "--recursive" then
        recursive = true
    elseif a:sub(1,1) ~= "-" then
        if not pattern then
            pattern = a
        else
            table.insert(files, shell.resolve(a))
        end
    end
    i = i + 1
end

if not pattern then
    io.stderr:write("usage: grep [-ivcnlqr] <pattern> [file ...]\n")
    os.exit(1)
end

-- Compile pattern
if ignoreCase then
    -- Wrap each char in case-insensitive class
    pattern = pattern:lower()
end

local function matches(line)
    local l = ignoreCase and line:lower() or line
    local m = l:find(pattern)
    return invertMatch and not m or (not invertMatch and m)
end

local function highlightMatch(line)
    -- Simple: color matched portion
    local l = ignoreCase and line:lower() or line
    local s, e = l:find(pattern)
    if not s or not term.isAvailable() then return line end
    return line:sub(1, s-1) ..
           "\x1b[1;31m" .. line:sub(s, e) .. "\x1b[0m" ..
           line:sub(e+1)
end

local totalMatches = 0
local exitCode = 1 -- Assume no match

local function processFile(path, showFilename)
    local f, err = io.open(path, "r")
    if not f then
        io.stderr:write("grep: " .. path .. ": " .. tostring(err) .. "\n")
        return
    end
    
    local lineNum = 0
    local fileMatches = 0
    
    for line in f:lines() do
        lineNum = lineNum + 1
        if matches(line) then
            fileMatches = fileMatches + 1
            totalMatches = totalMatches + 1
            exitCode = 0
            
            if not quiet and not fileNames and not countOnly then
                local out = ""
                if showFilename then out = out .. path .. ":" end
                if lineNumbers then out = out .. lineNum .. ":" end
                out = out .. highlightMatch(line)
                print(out)
            end
            
            if fileNames and not quiet then
                print(path)
                f:close()
                return
            end
        end
    end
    
    f:close()
    
    if countOnly and not quiet then
        local prefix = showFilename and (path .. ":") or ""
        print(prefix .. fileMatches)
    end
end

local function processDir(dirPath)
    local files_list = fs.list(dirPath)
    if not files_list then return end
    for _, name in ipairs(files_list) do
        local full = fs.concat(dirPath, name)
        if fs.isDirectory(full) then
            if recursive then processDir(full) end
        else
            processFile(full, true)
        end
    end
end

if #files == 0 then
    -- Read from stdin
    for line in io.lines() do
        if matches(line) then
            totalMatches = totalMatches + 1
            exitCode = 0
            if not quiet and not countOnly then
                print(highlightMatch(line))
            end
        end
    end
    if countOnly then print(totalMatches) end
else
    local showFilename = #files > 1
    for _, path in ipairs(files) do
        if fs.isDirectory(path) then
            if recursive then
                processDir(path)
            else
                io.stderr:write("grep: " .. path .. ": is a directory\n")
            end
        else
            processFile(path, showFilename)
        end
    end
end

os.exit(exitCode)
