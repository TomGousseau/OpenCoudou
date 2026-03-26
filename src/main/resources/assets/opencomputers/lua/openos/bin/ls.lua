-- ls.lua - List directory contents

local shell = require("shell")
local fs = require("filesystem")
local text = require("text")
local term = require("term")

local args = { ... }
local showAll = false
local longFormat = false
local target = nil

for _, a in ipairs(args) do
    if a == "-a" or a == "--all" then
        showAll = true
    elseif a == "-l" or a == "--long" then
        longFormat = true
    elseif a == "-la" or a == "-al" then
        showAll = true
        longFormat = true
    elseif a:sub(1, 1) ~= "-" then
        target = a
    end
end

target = target and shell.resolve(target) or shell.getWorkingDirectory()

if not fs.exists(target) then
    io.stderr:write("ls: no such file or directory: " .. target .. "\n")
    os.exit(1)
end

local files
if fs.isDirectory(target) then
    files, err = fs.list(target)
    if not files then
        io.stderr:write("ls: cannot list " .. target .. ": " .. tostring(err) .. "\n")
        os.exit(1)
    end
else
    files = { fs.name(target) }
    target = fs.path(target)
end

-- Sort: directories first, then alphabetically
table.sort(files, function(a, b)
    local aDir = fs.isDirectory(fs.concat(target, a))
    local bDir = fs.isDirectory(fs.concat(target, b))
    if aDir ~= bDir then return aDir end
    return a:lower() < b:lower()
end)

-- Filter hidden files
if not showAll then
    local visible = {}
    for _, f in ipairs(files) do
        if f:sub(1, 1) ~= "." then
            table.insert(visible, f)
        end
    end
    files = visible
end

if longFormat then
    -- Long format: type, size, date, name
    for _, f in ipairs(files) do
        local path = fs.concat(target, f)
        local isDir = fs.isDirectory(path)
        local size = isDir and "-" or tostring(fs.size(path))
        local modified = fs.lastModified(path)
        local dateStr = os.date("%Y-%m-%d %H:%M", math.floor(modified / 1000))
        
        local typeChar = isDir and "d" or "-"
        local colorStart = ""
        local colorEnd = ""
        if term.isAvailable() then
            if isDir then
                colorStart = "\x1b[1;34m"
                colorEnd = "\x1b[0m"
            elseif f:match("%.lua$") then
                colorStart = "\x1b[1;32m"
                colorEnd = "\x1b[0m"
            end
        end
        
        print(string.format("%s  %8s  %s  %s%s%s",
            typeChar, size, dateStr,
            colorStart, f .. (isDir and "/" or ""), colorEnd))
    end
else
    -- Normal format: columns
    if #files == 0 then return end
    
    local w = select(1, term.getSize())
    local maxLen = 0
    for _, f in ipairs(files) do
        local len = #f + (fs.isDirectory(fs.concat(target, f)) and 1 or 0)
        if len > maxLen then maxLen = len end
    end
    
    local colWidth = maxLen + 2
    local cols = math.max(1, math.floor(w / colWidth))
    
    for i, f in ipairs(files) do
        local path = fs.concat(target, f)
        local isDir = fs.isDirectory(path)
        local display = f .. (isDir and "/" or "")
        
        if term.isAvailable() then
            if isDir then
                term.setForeground(0x4488FF)
            elseif f:match("%.lua$") then
                term.setForeground(0x44FF44)
            else
                term.setForeground(0xFFFFFF)
            end
        end
        
        io.write(text.padRight(display, colWidth))
        
        if term.isAvailable() then
            term.setForeground(0xFFFFFF)
        end
        
        if i % cols == 0 then
            io.write("\n")
        end
    end
    
    if #files % cols ~= 0 then
        io.write("\n")
    end
end
