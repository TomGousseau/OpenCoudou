-- edit.lua - Simple text editor
local term = require("term")
local fs = require("filesystem")
local shell = require("shell")
local text = require("text")

local args = { ... }
local filename = args[1]
local filepath = filename and shell.resolve(filename)

-- Load file or create empty buffer
local lines = {}
if filepath and fs.exists(filepath) then
    local f, err = io.open(filepath, "r")
    if not f then
        io.stderr:write("edit: cannot open " .. filepath .. ": " .. tostring(err) .. "\n")
        os.exit(1)
    end
    for line in f:lines() do
        table.insert(lines, line)
    end
    f:close()
else
    -- New file
    lines = { "" }
end

if #lines == 0 then lines = { "" } end

local w, h = term.getSize()
local scrollY = 0       -- line offset
local cursorRow = 1     -- 1-based index into lines
local cursorCol = 1     -- 1-based index into current line
local modified = false
local running = true
local statusMsg = ""
local statusColor = 0xFFFFFF

local function clamp(v, lo, hi)
    return math.max(lo, math.min(hi, v))
end

local function statusBar()
    term.setCursor(1, h)
    term.setBackground(0x335577)
    term.setForeground(0xFFFFFF)
    local fname = filepath and fs.name(filepath) or "[new file]"
    local modFlag = modified and " [modified]" or ""
    local left = " " .. fname .. modFlag
    local right = " Ln " .. cursorRow .. " Col " .. cursorCol .. " "
    local mid = text.center("Ctrl+S: Save  Ctrl+Q: Quit  Ctrl+H: Help", w - #left - #right)
    local bar = left .. mid .. right
    -- Truncate if needed
    if #bar > w then bar = bar:sub(1, w) end
    -- Pad if needed
    bar = text.padRight(bar, w)
    io.write(bar)
    term.setBackground(0x000000)
    term.setForeground(0xFFFFFF)
end

local function redraw()
    local editH = h - 1
    
    -- Ensure scroll keeps cursor visible
    if cursorRow - 1 < scrollY then
        scrollY = cursorRow - 1
    end
    if cursorRow > scrollY + editH then
        scrollY = cursorRow - editH
    end
    
    term.setBackground(0x000000)
    term.setForeground(0xFFFFFF)
    
    for row = 1, editH do
        local lineIdx = scrollY + row
        term.setCursor(1, row)
        if lineIdx <= #lines then
            local line = lines[lineIdx]
            -- Truncate very long lines for display
            local display = unicode and unicode.sub(line, 1, w) or line:sub(1, w)
            io.write(text.padRight(display, w))
        else
            io.write(string.rep(" ", w))
        end
    end
    
    statusBar()
    
    -- Position cursor
    local displayCol = clamp(cursorCol, 1, w)
    local displayRow = cursorRow - scrollY
    term.setCursor(displayCol, displayRow)
end

-- Insert character at cursor
local function insertChar(ch)
    local line = lines[cursorRow]
    local before = line:sub(1, cursorCol - 1)
    local after = line:sub(cursorCol)
    lines[cursorRow] = before .. ch .. after
    cursorCol = cursorCol + 1
    modified = true
end

-- Handle backspace
local function backspace()
    if cursorCol > 1 then
        local line = lines[cursorRow]
        lines[cursorRow] = line:sub(1, cursorCol - 2) .. line:sub(cursorCol)
        cursorCol = cursorCol - 1
        modified = true
    elseif cursorRow > 1 then
        -- Join with previous line
        local prevLine = lines[cursorRow - 1]
        local thisLine = lines[cursorRow]
        cursorCol = #prevLine + 1
        lines[cursorRow - 1] = prevLine .. thisLine
        table.remove(lines, cursorRow)
        cursorRow = cursorRow - 1
        modified = true
    end
end

-- Handle enter
local function newline()
    local line = lines[cursorRow]
    local before = line:sub(1, cursorCol - 1)
    local after = line:sub(cursorCol)
    lines[cursorRow] = before
    table.insert(lines, cursorRow + 1, after)
    cursorRow = cursorRow + 1
    cursorCol = 1
    modified = true
end

-- Handle delete
local function deleteChar()
    local line = lines[cursorRow]
    if cursorCol <= #line then
        lines[cursorRow] = line:sub(1, cursorCol - 1) .. line:sub(cursorCol + 1)
        modified = true
    elseif cursorRow < #lines then
        lines[cursorRow] = line .. lines[cursorRow + 1]
        table.remove(lines, cursorRow + 1)
        modified = true
    end
end

-- Save file
local function save()
    if not filepath then
        -- Prompt for filename
        term.setCursor(1, h)
        term.setBackground(0x003300)
        term.setForeground(0xFFFFFF)
        io.write(" Save as: ")
        term.setBackground(0x000000)
        local name = term.read()
        if not name or #name == 0 then
            statusMsg = "Save cancelled"
            statusColor = 0xFFAA00
            return false
        end
        filepath = shell.resolve(name)
    end
    
    -- Ensure parent directory exists
    local parent = fs.path(filepath)
    if parent ~= "/" and not fs.exists(parent) then
        fs.makeDirectory(parent)
    end
    
    local f, err = io.open(filepath, "w")
    if not f then
        statusMsg = "Save failed: " .. tostring(err)
        statusColor = 0xFF4444
        return false
    end
    
    for i, line in ipairs(lines) do
        if i < #lines then
            f:write(line .. "\n")
        else
            f:write(line)
        end
    end
    f:close()
    
    modified = false
    statusMsg = "Saved: " .. filepath
    statusColor = 0x44FF44
    return true
end

-- Confirm discard dialog
local function confirmDiscard()
    term.setCursor(1, h)
    term.setBackground(0x770000)
    term.setForeground(0xFFFFFF)
    io.write(" Unsaved changes! Quit anyway? (y/N) ")
    term.setBackground(0x000000)
    local answer = term.read(nil, false)
    return answer and answer:lower() == "y"
end

-- Main editor loop
term.clear()
redraw()

while running do
    local sig, kb, charCode, code = computer.pullSignal()
    
    if sig == "key_down" then
        -- Check Ctrl combinations
        local ctrl = false -- TODO: track Ctrl state via key events
        
        if code == 200 then -- Up arrow
            cursorRow = math.max(1, cursorRow - 1)
            cursorCol = math.min(cursorCol, #lines[cursorRow] + 1)
        elseif code == 208 then -- Down arrow
            cursorRow = math.min(#lines, cursorRow + 1)
            cursorCol = math.min(cursorCol, #lines[cursorRow] + 1)
        elseif code == 203 then -- Left arrow
            if cursorCol > 1 then
                cursorCol = cursorCol - 1
            elseif cursorRow > 1 then
                cursorRow = cursorRow - 1
                cursorCol = #lines[cursorRow] + 1
            end
        elseif code == 205 then -- Right arrow
            if cursorCol <= #lines[cursorRow] then
                cursorCol = cursorCol + 1
            elseif cursorRow < #lines then
                cursorRow = cursorRow + 1
                cursorCol = 1
            end
        elseif code == 199 then -- Home
            cursorCol = 1
        elseif code == 207 then -- End
            cursorCol = #lines[cursorRow] + 1
        elseif code == 201 then -- Page Up
            cursorRow = math.max(1, cursorRow - (h - 2))
        elseif code == 209 then -- Page Down
            cursorRow = math.min(#lines, cursorRow + (h - 2))
        elseif code == 14 then -- Backspace
            backspace()
        elseif code == 211 then -- Delete
            deleteChar()
        elseif code == 28 then -- Enter
            newline()
        elseif code == 15 then -- Tab
            insertChar("\t")
        elseif charCode == 19 then -- Ctrl+S
            save()
        elseif charCode == 17 then -- Ctrl+Q
            if modified then
                if confirmDiscard() then
                    running = false
                end
            else
                running = false
            end
        elseif charCode == 8 then -- Ctrl+H = Help
            term.setCursor(1, h)
            term.setBackground(0x003355)
            term.setForeground(0xFFFFFF)
            io.write(" Ctrl+S=Save  Ctrl+Q=Quit  Arrows=Navigate  Enter=Newline  Backspace=Delete ")
            term.setBackground(0x000000)
            computer.pullSignal() -- Wait for keypress
        elseif charCode and charCode >= 32 then
            insertChar(string.char(charCode))
        end
        
        redraw()
    elseif sig == "clipboard" then
        -- Paste clipboard content
        local paste = charCode or kb
        for i = 1, #paste do
            local ch = paste:sub(i, i)
            if ch == "\n" then
                newline()
            else
                insertChar(ch)
            end
        end
        redraw()
    end
end

term.clear()
term.setCursor(1, 1)
