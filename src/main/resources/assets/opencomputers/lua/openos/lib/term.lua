-- term.lua - Terminal I/O library

local component = component
local computer = computer
local unicode = unicode

local term = {}

local gpu = nil
local screen = nil
local cursorX, cursorY = 1, 1
local width, height = 80, 25
local blink = true
local blinkTimer = nil
local cursorVisible = false
local focused = true

local function findGPU()
    for addr in component.list("gpu") do
        gpu = addr
        break
    end
    for addr in component.list("screen") do
        screen = addr
        break
    end
    if gpu and screen then
        component.invoke(gpu, "bind", screen)
        width, height = component.invoke(gpu, "getResolution")
    end
end

findGPU()

-- Bind to specific screen
function term.bind(screenAddr, gpuAddr)
    screen = screenAddr
    if gpuAddr then gpu = gpuAddr end
    if gpu and screen then
        component.invoke(gpu, "bind", screen)
        width, height = component.invoke(gpu, "getResolution")
    end
end

-- Get current cursor position
function term.getCursor()
    return cursorX, cursorY
end

-- Set cursor position
function term.setCursor(x, y)
    cursorX = math.max(1, math.min(x, width))
    cursorY = math.max(1, math.min(y, height))
end

-- Get terminal size
function term.getSize()
    if gpu then
        width, height = component.invoke(gpu, "getResolution")
    end
    return width, height
end

-- Clear screen
function term.clear()
    if not gpu then return end
    local w, h = term.getSize()
    component.invoke(gpu, "fill", 1, 1, w, h, " ")
    cursorX, cursorY = 1, 1
end

-- Clear current line
function term.clearLine()
    if not gpu then return end
    local w = term.getSize()
    component.invoke(gpu, "fill", 1, cursorY, w, 1, " ")
    cursorX = 1
end

-- Scroll terminal up
local function scroll(lines)
    if not gpu then return end
    lines = lines or 1
    local w, h = term.getSize()
    component.invoke(gpu, "copy", 1, 1 + lines, w, h - lines, 0, -lines)
    component.invoke(gpu, "fill", 1, h - lines + 1, w, lines, " ")
end

-- Write text at cursor
function term.write(str, wrap)
    if not gpu then return end
    if type(str) ~= "string" then str = tostring(str) end
    
    wrap = wrap ~= false -- default true
    
    local w, h = term.getSize()
    
    for i = 1, unicode.len(str) do
        local char = unicode.sub(str, i, i)
        local charW = unicode.charWidth(char)
        
        if char == "\n" then
            cursorX = 1
            cursorY = cursorY + 1
            if cursorY > h then
                scroll(1)
                cursorY = h
            end
        elseif char == "\r" then
            cursorX = 1
        elseif char == "\t" then
            local tabStop = math.floor((cursorX - 1) / 8) * 8 + 9
            while cursorX < tabStop do
                term.write(" ")
            end
        elseif char == "\b" then
            if cursorX > 1 then
                cursorX = cursorX - 1
                component.invoke(gpu, "set", cursorX, cursorY, " ")
            end
        else
            if cursorX + charW - 1 > w then
                if wrap then
                    cursorX = 1
                    cursorY = cursorY + 1
                    if cursorY > h then
                        scroll(1)
                        cursorY = h
                    end
                else
                    break
                end
            end
            component.invoke(gpu, "set", cursorX, cursorY, char)
            cursorX = cursorX + charW
        end
    end
end

-- Read a line of input
function term.read(history, dobreak, hint)
    local event = require("event")
    
    if not gpu then
        -- No display, just read a signal
        while true do
            local sig, _, char, code = computer.pullSignal()
            if sig == "key_down" and code == 28 then
                return ""
            end
        end
    end
    
    history = history or {}
    local historyIndex = #history + 1
    local input = ""
    local cursorPos = 0 -- position in input string (0 = before first char)
    local scrollOffset = 0
    local w = select(1, term.getSize())
    local startX, startY = cursorX, cursorY
    
    -- Add empty entry at end for current input
    table.insert(history, "")
    
    local function redraw()
        local displayInput = input
        local maxDisplayLen = w - startX
        
        -- Calculate visible window
        local actualCursorPos = unicode.len(unicode.sub(input, 1, cursorPos))
        if actualCursorPos - scrollOffset > maxDisplayLen - 1 then
            scrollOffset = actualCursorPos - maxDisplayLen + 1
        elseif actualCursorPos < scrollOffset then
            scrollOffset = actualCursorPos
        end
        
        local visible = unicode.wtrunc(unicode.sub(displayInput, scrollOffset + 1), maxDisplayLen)
        component.invoke(gpu, "fill", startX, startY, maxDisplayLen, 1, " ")
        component.invoke(gpu, "set", startX, startY, visible)
        
        -- Position cursor
        local visibleCursorX = actualCursorPos - scrollOffset
        cursorX = startX + visibleCursorX
        cursorY = startY
    end
    
    redraw()
    
    while true do
        local sig, kb, char, code = computer.pullSignal()
        
        if sig == "key_down" then
            if code == 28 then -- Enter
                if dobreak ~= false then
                    term.write("\n")
                end
                history[#history] = input
                if input == "" or input == history[#history - 1] then
                    table.remove(history, #history)
                end
                return input
                
            elseif code == 14 then -- Backspace
                if cursorPos > 0 then
                    local before = unicode.sub(input, 1, cursorPos - 1)
                    local after = unicode.sub(input, cursorPos + 1)
                    input = before .. after
                    cursorPos = cursorPos - 1
                    redraw()
                end
                
            elseif code == 211 then -- Delete
                if cursorPos < unicode.len(input) then
                    local before = unicode.sub(input, 1, cursorPos)
                    local after = unicode.sub(input, cursorPos + 2)
                    input = before .. after
                    redraw()
                end
                
            elseif code == 203 then -- Left arrow
                if cursorPos > 0 then
                    cursorPos = cursorPos - 1
                    redraw()
                end
                
            elseif code == 205 then -- Right arrow
                if cursorPos < unicode.len(input) then
                    cursorPos = cursorPos + 1
                    redraw()
                end
                
            elseif code == 199 then -- Home
                cursorPos = 0
                redraw()
                
            elseif code == 207 then -- End
                cursorPos = unicode.len(input)
                redraw()
                
            elseif code == 200 then -- Up arrow (history)
                if historyIndex > 1 then
                    history[historyIndex] = input
                    historyIndex = historyIndex - 1
                    input = history[historyIndex]
                    cursorPos = unicode.len(input)
                    redraw()
                end
                
            elseif code == 208 then -- Down arrow (history)
                if historyIndex < #history then
                    history[historyIndex] = input
                    historyIndex = historyIndex + 1
                    input = history[historyIndex]
                    cursorPos = unicode.len(input)
                    redraw()
                end
                
            elseif code == 15 then -- Tab (autocomplete)
                if hint then
                    local completion = hint(input)
                    if completion then
                        input = input .. completion
                        cursorPos = unicode.len(input)
                        redraw()
                    end
                end
                
            elseif char and char >= 32 then
                local ch = unicode.char(char)
                local before = unicode.sub(input, 1, cursorPos)
                local after = unicode.sub(input, cursorPos + 1)
                input = before .. ch .. after
                cursorPos = cursorPos + 1
                redraw()
            end
            
        elseif sig == "clipboard" then
            local paste = char
            local before = unicode.sub(input, 1, cursorPos)
            local after = unicode.sub(input, cursorPos + 1)
            input = before .. paste .. after
            cursorPos = cursorPos + unicode.len(paste)
            redraw()
        end
    end
end

-- Set foreground color
function term.setForeground(color)
    if gpu then
        component.invoke(gpu, "setForeground", color)
    end
end

-- Set background color
function term.setBackground(color)
    if gpu then
        component.invoke(gpu, "setBackground", color)
    end
end

-- Get foreground color
function term.getForeground()
    if gpu then
        return component.invoke(gpu, "getForeground")
    end
    return 0xFFFFFF
end

-- Get background color
function term.getBackground()
    if gpu then
        return component.invoke(gpu, "getBackground")
    end
    return 0x000000
end

-- Move cursor without writing
function term.up(n) cursorY = math.max(1, cursorY - (n or 1)) end
function term.down(n) cursorY = math.min(height, cursorY + (n or 1)) end
function term.left(n) cursorX = math.max(1, cursorX - (n or 1)) end
function term.right(n) cursorX = math.min(width, cursorX + (n or 1)) end

-- Is there a GPU/screen connected?
function term.isAvailable()
    return gpu ~= nil and screen ~= nil
end

-- Print with newline
function term.println(str)
    term.write(tostring(str) .. "\n")
end

return term
