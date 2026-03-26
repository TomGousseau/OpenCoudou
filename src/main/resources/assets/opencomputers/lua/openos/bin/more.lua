-- more.lua - Pager (display file one screen at a time)
local shell = require("shell")
local fs = require("filesystem")
local term = require("term")
local text = require("text")

local args = { ... }
local files = {}

for _, a in ipairs(args) do
    if a:sub(1,1) ~= "-" then
        table.insert(files, shell.resolve(a))
    end
end

local w, h = term.getSize()
local pageSize = h - 1

local function showPage(lines, startLine)
    for i = startLine, math.min(#lines, startLine + pageSize - 1) do
        print(lines[i])
    end
    
    local atEnd = startLine + pageSize > #lines
    
    term.setBackground(0x333333)
    term.setForeground(0xFFFFFF)
    if atEnd then
        io.write(text.padRight(" (END)  q=Quit", w))
    else
        io.write(text.padRight(string.format(" --More-- (%d%%)", 
            math.floor(startLine / #lines * 100)), w))
    end
    term.setBackground(0x000000)
    term.setForeground(0xFFFFFF)
    
    return atEnd
end

local function paginate(content)
    local lines = {}
    for line in content:gmatch("[^\n]*") do
        -- Wrap long lines
        if #line > w then
            for _, wrapped in ipairs(text.wrap(line, w)) do
                table.insert(lines, wrapped)
            end
        else
            table.insert(lines, line)
        end
    end
    
    if #lines == 0 then return end
    
    local pos = 1
    
    while true do
        term.clear()
        local atEnd = showPage(lines, pos)
        
        local sig = { computer.pullSignal() }
        if sig[1] == "key_down" then
            local code = sig[4]
            if code == 16 or code == 1 then -- q or Esc
                break
            elseif code == 57 or code == 208 then -- Space or Down
                if atEnd then break end
                pos = pos + pageSize
            elseif code == 200 then -- Up
                pos = math.max(1, pos - pageSize)
            elseif code == 199 then -- Home
                pos = 1
            elseif code == 207 then -- End
                pos = math.max(1, #lines - pageSize + 1)
            elseif code == 28 then -- Enter
                if atEnd then break end
                pos = pos + 1
            end
        end
    end
    
    term.clear()
end

if #files == 0 then
    -- Pipe mode: read stdin
    local content = io.read("*a")
    if content and #content > 0 then
        paginate(content)
    end
else
    for _, path in ipairs(files) do
        if not fs.exists(path) then
            io.stderr:write("more: " .. path .. ": not found\n")
        elseif fs.isDirectory(path) then
            io.stderr:write("more: " .. path .. ": is a directory\n")
        else
            local data = fs.readAll(path)
            if data then
                paginate(data)
            end
        end
    end
end
