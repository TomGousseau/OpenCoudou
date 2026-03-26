-- nano.lua - Simple nano-like text editor
-- More featureful than edit.lua, with search/replace, line numbers, 
-- multiple keyboard shortcuts, and a status bar

local term = require("term")
local fs   = require("filesystem")
local gpu  = require("gpu")

-- -----------------------------------------------------------------------
-- State
-- -----------------------------------------------------------------------

local filename = (...)
local lines    = {}       -- array of strings
local cursorX  = 1        -- column (1-based character position)
local cursorY  = 1        -- line in buffer (1-based)
local scrollY  = 0        -- lines scrolled from top
local scrollX  = 0        -- columns scrolled from left
local modified = false
local running  = true
local message  = ""       -- status message
local messageTimer = 0    -- uptime when message expires

local SEARCH_STR  = ""
local REPLACE_STR = ""

-- Screen size (updated on resize)
local sw, sh = term.getSize()
local CONTENT_H = sh - 2  -- header + footer
local CONTENT_W = sw - 5  -- line number gutter

-- -----------------------------------------------------------------------
-- Buffer operations
-- -----------------------------------------------------------------------

local function loadFile(path)
    if not path or not fs.exists(path) then
        lines = { "" }
        return
    end
    local content = fs.readAll(path) or ""
    lines = {}
    for line in (content .. "\n"):gmatch("([^\n]*)\n") do
        table.insert(lines, line)
    end
    if #lines == 0 then lines = { "" } end
end

local function saveFile(path)
    if not path then return false, "no filename" end
    local content = table.concat(lines, "\n")
    local ok, err = fs.writeAll(path, content)
    if ok then
        modified = false
        return true
    end
    return false, err
end

local function currentLine()
    return lines[cursorY] or ""
end

local function setMessage(msg, duration)
    message = msg
    messageTimer = computer.uptime() + (duration or 3)
end

-- -----------------------------------------------------------------------
-- Drawing
-- -----------------------------------------------------------------------

local function clampScroll()
    if cursorY <= scrollY then
        scrollY = cursorY - 1
    elseif cursorY > scrollY + CONTENT_H then
        scrollY = cursorY - CONTENT_H
    end
    local line = currentLine()
    if cursorX <= scrollX + 1 then
        scrollX = math.max(0, cursorX - 1)
    elseif cursorX > scrollX + CONTENT_W then
        scrollX = cursorX - CONTENT_W
    end
end

local function drawHeader()
    local g = component.proxy(component.list("gpu")())
    g.setBackground(0x222266)
    g.setForeground(0xFFFFFF)
    g.fill(1, 1, sw, 1, " ")
    local title = (modified and "* " or "  ") .. (filename or "[new file]")
    local pos = string.format("  Ln %d/%d, Col %d", cursorY, #lines, cursorX)
    g.set(2, 1, title)
    g.set(sw - #pos, 1, pos)
end

local function drawFooter()
    local g = component.proxy(component.list("gpu")())
    g.setBackground(0x222222)
    g.setForeground(0xAAAAAA)
    g.fill(1, sh, sw, 1, " ")
    
    local msg = computer.uptime() < messageTimer and message or
        "^S Save  ^Q Quit  ^F Find  ^H Replace  ^G Go  ^C Copy  ^V Paste"
    if #msg > sw then msg = msg:sub(1, sw) end
    g.set(1, sh, msg)
end

local function drawContent()
    local g = component.proxy(component.list("gpu")())
    
    for row = 1, CONTENT_H do
        local lineNum = scrollY + row
        local y = row + 1  -- +1 for header
        
        -- Line number gutter (4 chars + separator)
        g.setBackground(0x111122)
        g.setForeground(0x555588)
        if lineNum <= #lines then
            g.set(1, y, string.format("%4d│", lineNum))
        else
            g.set(1, y, "    │")
        end
        
        -- Line content
        g.setBackground(0x000000)
        g.setForeground(0xFFFFFF)
        g.fill(6, y, CONTENT_W, 1, " ")
        
        if lineNum <= #lines then
            local line = lines[lineNum]
            if scrollX < #line then
                local visible = line:sub(scrollX + 1, scrollX + CONTENT_W)
                g.set(6, y, visible)
            end
        end
    end
end

local function redraw()
    clampScroll()
    drawHeader()
    drawContent()
    drawFooter()
    
    -- Position cursor
    local screenX = cursorX - scrollX + 5  -- 5 = gutter width
    local screenY = cursorY - scrollY + 1  -- +1 for header
    term.setCursor(math.max(1, math.min(sw, screenX)),
                   math.max(2, math.min(sh - 1, screenY)))
end

-- -----------------------------------------------------------------------
-- Editing operations
-- -----------------------------------------------------------------------

local clipboard = ""

local function insertChar(ch)
    local line = currentLine()
    local left  = line:sub(1, cursorX - 1)
    local right = line:sub(cursorX)
    lines[cursorY] = left .. ch .. right
    cursorX = cursorX + 1
    modified = true
end

local function insertNewline()
    local line  = currentLine()
    local left  = line:sub(1, cursorX - 1)
    local right = line:sub(cursorX)
    
    -- Auto-indent: copy leading whitespace from current line
    local indent = left:match("^(%s*)")
    
    lines[cursorY] = left
    table.insert(lines, cursorY + 1, indent .. right)
    cursorY = cursorY + 1
    cursorX = #indent + 1
    modified = true
end

local function backspace()
    if cursorX > 1 then
        local line  = currentLine()
        lines[cursorY] = line:sub(1, cursorX - 2) .. line:sub(cursorX)
        cursorX = cursorX - 1
        modified = true
    elseif cursorY > 1 then
        -- Join with previous line
        local prevLine = lines[cursorY - 1]
        local thisLine = lines[cursorY]
        cursorX = #prevLine + 1
        lines[cursorY - 1] = prevLine .. thisLine
        table.remove(lines, cursorY)
        cursorY = cursorY - 1
        modified = true
    end
end

local function deleteFwd()
    local line = currentLine()
    if cursorX <= #line then
        lines[cursorY] = line:sub(1, cursorX - 1) .. line:sub(cursorX + 1)
        modified = true
    elseif cursorY < #lines then
        lines[cursorY] = line .. lines[cursorY + 1]
        table.remove(lines, cursorY + 1)
        modified = true
    end
end

local function gotoLine(n)
    cursorY = math.max(1, math.min(#lines, n))
    cursorX = 1
end

-- -----------------------------------------------------------------------
-- Search
-- -----------------------------------------------------------------------

local function findNext(pattern, fromY, fromX)
    if pattern == "" then return nil end
    -- Search from current position forward, wrapping
    for i = 0, #lines - 1 do
        local lineNum = ((fromY - 1 + i) % #lines) + 1
        local line    = lines[lineNum]
        local startX  = (i == 0) and fromX or 1
        local col = line:find(pattern, startX, true)
        if col then
            return lineNum, col
        end
    end
    return nil
end

-- -----------------------------------------------------------------------
-- Input prompt (inline in footer)
-- -----------------------------------------------------------------------

local function prompt(promptStr)
    local g = component.proxy(component.list("gpu")())
    g.setBackground(0x222222)
    g.setForeground(0xFFFFFF)
    g.fill(1, sh, sw, 1, " ")
    g.set(1, sh, promptStr)
    local inputX = #promptStr + 1
    
    local buf = ""
    while true do
        g.set(inputX, sh, buf .. " ")
        term.setCursor(inputX + #buf, sh)
        
        local ev, _, char, key = require("event").pull(10)
        if ev == "key_down" then
            if key == 28 then return buf       -- Enter
            elseif key == 1 then return nil    -- Escape
            elseif key == 14 and #buf > 0 then
                buf = buf:sub(1, -2)
            elseif char and char >= 32 and char < 256 then
                buf = buf .. string.char(char)
            end
        end
    end
end

-- -----------------------------------------------------------------------
-- Main loop
-- -----------------------------------------------------------------------

loadFile(filename)

-- Initial screen setup
term.clear()
sw, sh = term.getSize()
CONTENT_H = sh - 2
CONTENT_W = sw - 5
redraw()

while running do
    local ev, _, char, key = require("event").pull(1)
    
    if computer.uptime() >= messageTimer and message ~= "" then
        message = ""
        drawFooter()
    end
    
    if ev ~= "key_down" then goto continue end
    
    -- Navigation
    if key == 200 then        -- Up
        if cursorY > 1 then cursorY = cursorY - 1 end
        cursorX = math.min(cursorX, #currentLine() + 1)
        
    elseif key == 208 then    -- Down
        if cursorY < #lines then cursorY = cursorY + 1 end
        cursorX = math.min(cursorX, #currentLine() + 1)
        
    elseif key == 203 then    -- Left
        if cursorX > 1 then
            cursorX = cursorX - 1
        elseif cursorY > 1 then
            cursorY = cursorY - 1
            cursorX = #currentLine() + 1
        end
        
    elseif key == 205 then    -- Right
        local line = currentLine()
        if cursorX <= #line then
            cursorX = cursorX + 1
        elseif cursorY < #lines then
            cursorY = cursorY + 1
            cursorX = 1
        end
        
    elseif key == 199 then    -- Home
        cursorX = 1
        
    elseif key == 207 then    -- End
        cursorX = #currentLine() + 1
        
    elseif key == 201 then    -- Page Up
        cursorY = math.max(1, cursorY - CONTENT_H)
        cursorX = math.min(cursorX, #currentLine() + 1)
        
    elseif key == 209 then    -- Page Down
        cursorY = math.min(#lines, cursorY + CONTENT_H)
        cursorX = math.min(cursorX, #currentLine() + 1)
        
    elseif key == 28 or key == 156 then  -- Enter
        insertNewline()
        
    elseif key == 14 then    -- Backspace
        backspace()
        
    elseif key == 211 then   -- Delete
        deleteFwd()
        
    elseif key == 15 then    -- Tab
        insertChar("\t")
        
    -- Control keys (char codes below 32)
    elseif char == 19 then   -- Ctrl+S
        if not filename then
            filename = prompt("Save as: ")
        end
        if filename then
            local ok, err = saveFile(filename)
            if ok then
                setMessage("Saved: " .. filename)
            else
                setMessage("Error saving: " .. tostring(err))
            end
        end
        
    elseif char == 17 then   -- Ctrl+Q
        if modified then
            setMessage("Unsaved changes! Press Ctrl+Q again to quit.")
            modified = false  -- Second Ctrl+Q will exit
        else
            running = false
        end
        
    elseif char == 6 then    -- Ctrl+F (Find)
        local pat = prompt("Search: ")
        if pat and pat ~= "" then
            SEARCH_STR = pat
            local ly, lx = findNext(pat, cursorY, cursorX + 1)
            if ly then
                cursorY = ly; cursorX = lx
                setMessage("Found at line " .. ly)
            else
                setMessage("Not found: " .. pat)
            end
        end
        
    elseif char == 14 then   -- Ctrl+N (Find Next) — note: also backspace? key 14 vs char 14
        if SEARCH_STR ~= "" then
            local ly, lx = findNext(SEARCH_STR, cursorY, cursorX + 1)
            if ly then cursorY = ly; cursorX = lx
            else setMessage("No more matches") end
        end
        
    elseif char == 8 then    -- Ctrl+H (Replace)
        local pat = prompt("Find: ")
        if pat and pat ~= "" then
            local rep = prompt("Replace with: ")
            if rep then
                local count = 0
                for i, line in ipairs(lines) do
                    local newLine = line:gsub(pat, rep, nil)
                    if newLine ~= line then
                        lines[i] = newLine
                        count = count + 1
                        modified = true
                    end
                end
                setMessage(string.format("Replaced %d occurrence(s)", count))
            end
        end
        
    elseif char == 7 then    -- Ctrl+G (Go to line)
        local input = prompt("Go to line: ")
        if input then
            local n = tonumber(input)
            if n then gotoLine(n)
            else setMessage("Invalid line number") end
        end
        
    elseif char == 3 then    -- Ctrl+C (Copy line)
        clipboard = currentLine()
        setMessage("Copied line " .. cursorY)
        
    elseif char == 22 then   -- Ctrl+V (Paste)
        if clipboard ~= "" then
            -- Insert clipboard as new line below cursor
            table.insert(lines, cursorY + 1, clipboard)
            cursorY = cursorY + 1
            cursorX = 1
            modified = true
            setMessage("Pasted")
        end
        
    elseif char == 11 then   -- Ctrl+K (Delete line)
        if #lines > 1 then
            table.remove(lines, cursorY)
            if cursorY > #lines then cursorY = #lines end
            cursorX = 1
            modified = true
        else
            lines[1] = ""
            cursorX  = 1
            modified = true
        end
        setMessage("Line deleted")
        
    elseif char and char >= 32 and char < 256 then
        insertChar(string.char(char))
    end
    
    redraw()
    ::continue::
end

-- Restore terminal
term.clear()
term.setCursor(1, 1)
term.setForeground(0xFFFFFF)
term.setBackground(0x000000)
if filename then
    print(modified and ("nano: left modified file: " .. filename) or ("nano: closed " .. filename))
end
