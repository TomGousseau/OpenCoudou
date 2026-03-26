-- gpu.lua - GPU drawing utility library
-- Provides higher-level drawing primitives on top of the raw GPU component

local gpu = {}

-- Get the primary GPU component proxy
local function G()
    local addr = component.list("gpu")()
    if not addr then error("no GPU found") end
    return component.proxy(addr)
end

-- -----------------------------------------------------------------------
-- Screen management
-- -----------------------------------------------------------------------

--- Bind the GPU to a screen
function gpu.bind(screenAddr)
    G().bind(screenAddr)
end

--- Get screen dimensions
function gpu.getSize()
    return G().getResolution()
end

--- Set screen resolution
function gpu.setResolution(w, h)
    return G().setResolution(w, h)
end

--- Get the maximum screen resolution
function gpu.maxResolution()
    return G().maxResolution()
end

--- Get the current depth (bits per color)
function gpu.getDepth()
    return G().getDepth()
end

--- Set render depth
function gpu.setDepth(depth)
    return G().setDepth(depth)
end

-- -----------------------------------------------------------------------
-- Color management
-- -----------------------------------------------------------------------

--- Set foreground color
function gpu.setFG(color, isPaletteIndex)
    return G().setForeground(color, isPaletteIndex or false)
end

--- Set background color
function gpu.setBG(color, isPaletteIndex)
    return G().setBackground(color, isPaletteIndex or false)
end

--- Get foreground color
function gpu.getFG()
    return G().getForeground()
end

--- Get background color
function gpu.getBG()
    return G().getBackground()
end

--- Set palette color at index (0-15)
function gpu.setPalette(index, color)
    return G().setPaletteColor(index, color)
end

--- Get palette color
function gpu.getPalette(index)
    return G().getPaletteColor(index)
end

-- -----------------------------------------------------------------------
-- Drawing primitives
-- -----------------------------------------------------------------------

--- Fill a rectangle with a character
function gpu.fill(x, y, w, h, char)
    return G().fill(x, y, w, h, char or " ")
end

--- Clear the entire screen
function gpu.clear(fg, bg)
    local g = G()
    if bg then g.setBackground(bg) end
    if fg then g.setForeground(fg) end
    local w, h = g.getResolution()
    g.fill(1, 1, w, h, " ")
end

--- Write text at position
function gpu.set(x, y, text, vertical)
    return G().set(x, y, text, vertical or false)
end

--- Get character and colors at position
function gpu.get(x, y)
    return G().get(x, y)
end

--- Copy a region to another position
function gpu.copy(x, y, w, h, tx, ty)
    return G().copy(x, y, w, h, tx, ty)
end

-- -----------------------------------------------------------------------
-- Higher-level drawing
-- -----------------------------------------------------------------------

--- Draw a horizontal line at y from x1 to x2
function gpu.hline(y, x1, x2, char)
    local w = math.abs(x2 - x1) + 1
    local sx = math.min(x1, x2)
    G().fill(sx, y, w, 1, char or "─")
end

--- Draw a vertical line at x from y1 to y2
function gpu.vline(x, y1, y2, char)
    local h = math.abs(y2 - y1) + 1
    local sy = math.min(y1, y2)
    G().fill(x, sy, 1, h, char or "│")
end

--- Draw a box (border only) at x, y with width w and height h
--- style: "single" (default), "double", "heavy", "rounded", "ascii"
function gpu.box(x, y, w, h, style)
    style = style or "single"
    local g = G()
    
    local chars = {
        single  = { tl="┌", tr="┐", bl="└", br="┘", h="─", v="│" },
        double  = { tl="╔", tr="╗", bl="╚", br="╝", h="═", v="║" },
        heavy   = { tl="┏", tr="┓", bl="┗", br="┛", h="━", v="┃" },
        rounded = { tl="╭", tr="╮", bl="╰", br="╯", h="─", v="│" },
        ascii   = { tl="+", tr="+", bl="+", br="+", h="-", v="|" },
    }
    local c = chars[style] or chars.single
    
    -- Top/bottom edges
    if w > 2 then
        g.fill(x + 1, y,         w - 2, 1, c.h)  -- top
        g.fill(x + 1, y + h - 1, w - 2, 1, c.h)  -- bottom
    end
    -- Left/right edges
    if h > 2 then
        g.fill(x,         y + 1, 1, h - 2, c.v)  -- left
        g.fill(x + w - 1, y + 1, 1, h - 2, c.v)  -- right
    end
    -- Corners
    g.set(x,             y,         c.tl)
    g.set(x + w - 1,     y,         c.tr)
    g.set(x,             y + h - 1, c.bl)
    g.set(x + w - 1,     y + h - 1, c.br)
end

--- Draw a filled rectangle with optional border
function gpu.rect(x, y, w, h, fg, bg, char, borderStyle)
    local g = G()
    local ofg = g.getForeground()
    local obg = g.getBackground()
    
    if bg then g.setBackground(bg) end
    if fg then g.setForeground(fg) end
    
    g.fill(x, y, w, h, char or " ")
    
    if borderStyle then
        gpu.box(x, y, w, h, borderStyle)
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
end

--- Print text centred in a screen-width row at y
function gpu.centredText(y, text, fg)
    local g = G()
    local w, _ = g.getResolution()
    local ofg = g.getForeground()
    if fg then g.setForeground(fg) end
    local x = math.floor((w - #text) / 2) + 1
    g.set(x, y, text)
    if fg then g.setForeground(ofg) end
end

--- Draw a progress bar at x, y with width w
--- value: 0.0–1.0
function gpu.progressBar(x, y, w, value, fgFilled, fgEmpty, bgFilled, bgEmpty)
    local g     = G()
    value       = math.max(0, math.min(1, value))
    local filled = math.floor(w * value)
    local empty  = w - filled
    
    local ofg = g.getForeground()
    local obg = g.getBackground()
    
    if filled > 0 then
        if bgFilled then g.setBackground(bgFilled) end
        if fgFilled then g.setForeground(fgFilled) end
        g.fill(x, y, filled, 1, "█")
    end
    if empty > 0 then
        if bgEmpty then g.setBackground(bgEmpty) end
        if fgEmpty then g.setForeground(fgEmpty) end
        g.fill(x + filled, y, empty, 1, "░")
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
end

--- Draw a button (highlighted rectangle with label)
function gpu.button(x, y, label, fg, bg, active)
    local g   = G()
    local w   = #label + 2
    local ofg = g.getForeground()
    local obg = g.getBackground()
    
    fg = fg or (active and 0x000000 or 0xFFFFFF)
    bg = bg or (active and 0xFFFFFF or 0x444444)
    
    g.setForeground(fg)
    g.setBackground(bg)
    g.fill(x, y, w, 1, " ")
    g.set(x + 1, y, label)
    
    g.setForeground(ofg)
    g.setBackground(obg)
    
    return x, y, w, 1 -- hit box
end

--- Scroll (move) content of a region up/down
function gpu.scroll(x, y, w, h, amount)
    local g = G()
    if amount > 0 then
        -- Scroll up: copy region up
        g.copy(x, y + amount, w, h - amount, 0, -amount)
        g.fill(x, y + h - amount, w, amount, " ")
    elseif amount < 0 then
        -- Scroll down
        local a = -amount
        g.copy(x, y, w, h - a, 0, a)
        g.fill(x, y, w, a, " ")
    end
end

--- Draw a list of strings with an optional highlighted line
function gpu.list(x, y, w, h, items, selected, fg, bg, selFg, selBg)
    local g   = G()
    local ofg = g.getForeground()
    local obg = g.getBackground()
    
    fg    = fg    or 0xFFFFFF
    bg    = bg    or 0x000000
    selFg = selFg or 0x000000
    selBg = selBg or 0x44AAFF
    
    for row = 1, h do
        local item = items[row]
        local isSel = (selected == row)
        
        g.setForeground(isSel and selFg or fg)
        g.setBackground(isSel and selBg or bg)
        
        if item then
            local text = tostring(item)
            if #text > w then text = text:sub(1, w - 1) .. "…" end
            g.fill(x, y + row - 1, w, 1, " ")
            g.set(x, y + row - 1, text)
        else
            g.fill(x, y + row - 1, w, 1, " ")
        end
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
end

--- Draw a simple text input field showing text with cursor
function gpu.input(x, y, w, text, cursorPos, fg, bg, cursorFg, cursorBg)
    local g   = G()
    local ofg = g.getForeground()
    local obg = g.getBackground()
    
    fg       = fg       or 0xFFFFFF
    bg       = bg       or 0x222222
    cursorFg = cursorFg or 0x000000
    cursorBg = cursorBg or 0xFFFFFF
    cursorPos = cursorPos or (#text + 1)
    
    -- Scroll text if too long
    local display = text
    local offset  = 0
    if #display >= w then
        offset  = math.max(0, cursorPos - w + 1)
        display = display:sub(offset + 1, offset + w - 1)
    end
    
    g.setForeground(fg)
    g.setBackground(bg)
    g.fill(x, y, w, 1, " ")
    g.set(x, y, display)
    
    -- Draw cursor
    local cx = cursorPos - offset
    if cx >= 1 and cx <= w then
        local charUnder = text:sub(cursorPos, cursorPos)
        if charUnder == "" then charUnder = " " end
        g.setForeground(cursorFg)
        g.setBackground(cursorBg)
        g.set(x + cx - 1, y, charUnder)
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
end

--- Draw a horizontal menu bar
function gpu.menuBar(y, items, selected, fg, bg, selFg, selBg, separatorChar)
    local g   = G()
    local ofg = g.getForeground()
    local obg = g.getBackground()
    local w, _ = g.getResolution()
    
    fg          = fg          or 0xFFFFFF
    bg          = bg          or 0x333333
    selFg       = selFg       or 0x000000
    selBg       = selBg       or 0xFFFFFF
    separatorChar = separatorChar or " │ "
    
    g.setForeground(fg)
    g.setBackground(bg)
    g.fill(1, y, w, 1, " ")
    
    local cx = 2
    for i, item in ipairs(items) do
        local isSel = (selected == i)
        g.setForeground(isSel and selFg or fg)
        g.setBackground(isSel and selBg or bg)
        
        local label = " " .. tostring(item) .. " "
        g.set(cx, y, label)
        cx = cx + #label
        
        if i < #items then
            g.setForeground(0x666666)
            g.setBackground(bg)
            g.set(cx, y, separatorChar)
            cx = cx + #separatorChar
        end
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
end

--- Display a modal dialog
function gpu.dialog(title, message, buttons, fg, bg, titleFg)
    local g    = G()
    local sw, sh = g.getResolution()
    buttons    = buttons  or { "OK" }
    fg         = fg       or 0xFFFFFF
    bg         = bg       or 0x222266
    titleFg    = titleFg  or 0xFFFF44
    
    -- Calculate dialog size
    local lines = {}
    for line in (message .. "\n"):gmatch("([^\n]*)\n") do
        table.insert(lines, line)
    end
    local dw    = math.max(#title + 4, 30)
    for _, l in ipairs(lines) do dw = math.max(dw, #l + 4) end
    dw = math.min(dw, sw - 4)
    local dh = #lines + 4
    local dx = math.floor((sw - dw) / 2) + 1
    local dy = math.floor((sh - dh) / 2) + 1
    
    -- Draw dialog background
    local ofg = g.getForeground()
    local obg = g.getBackground()
    g.setBackground(bg)
    g.setForeground(fg)
    g.fill(dx, dy, dw, dh, " ")
    gpu.box(dx, dy, dw, dh, "double")
    
    -- Title
    g.setForeground(titleFg)
    g.set(dx + 2, dy, " " .. title .. " ")
    
    -- Message lines
    g.setForeground(fg)
    for i, line in ipairs(lines) do
        g.set(dx + 2, dy + 1 + i, line)
    end
    
    -- Buttons
    local btnY   = dy + dh - 2
    local btnTot = 0
    for _, b in ipairs(buttons) do btnTot = btnTot + #b + 4 end
    local bx = dx + math.floor((dw - btnTot) / 2)
    for bi, label in ipairs(buttons) do
        local isFirst = (bi == 1)
        gpu.button(bx, btnY, label, isFirst and 0x000000 or fg, isFirst and 0xFFFFFF or 0x555555)
        bx = bx + #label + 4
    end
    
    g.setForeground(ofg)
    g.setBackground(obg)
    
    -- Wait for keyboard input
    local selected = 1
    while true do
        local ev, _, char, key = require("event").pull("key_down")
        if ev then
            if key == 28 or key == 156 then  -- Enter
                return selected
            elseif key == 15 or key == 205 then  -- Tab / Right
                selected = (selected % #buttons) + 1
            elseif key == 203 then  -- Left
                selected = ((selected - 2) % #buttons) + 1
            elseif key == 1 then  -- Escape
                return #buttons  -- Return last button (usually Cancel)
            end
            
            -- Redraw buttons with new selection
            g.setBackground(bg)
            g.fill(dx + 1, btnY, dw - 2, 1, " ")
            bx = dx + math.floor((dw - btnTot) / 2)
            for bi, label in ipairs(buttons) do
                local isActive = (bi == selected)
                gpu.button(bx, btnY, label, isActive and 0x000000 or fg, isActive and 0xFFFFFF or 0x555555)
                bx = bx + #label + 4
            end
        end
    end
end

return gpu
