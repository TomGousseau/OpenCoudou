-- edit command: simple text editor

local args = {...}
local path = args[1]

if not path then
  print("Usage: edit <file>")
  return
end

-- Resolve relative paths
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()
local gpu = component.list("gpu")()
local w, h = component.invoke(gpu, "getResolution")

-- Editor state
local lines = {""}
local cursorX = 1
local cursorY = 1
local scrollX = 0
local scrollY = 0
local dirty = false
local running = true

-- Status messages
local statusMsg = ""
local statusTime = 0

-- Load file if exists
if component.invoke(bootFs, "exists", path) and not component.invoke(bootFs, "isDirectory", path) then
  lines = {}
  local handle = component.invoke(bootFs, "open", path, "r")
  if handle then
    local content = ""
    repeat
      local chunk = component.invoke(bootFs, "read", handle, 4096)
      if chunk then
        content = content .. chunk
      end
    until not chunk
    component.invoke(bootFs, "close", handle)
    
    for line in (content .. "\n"):gmatch("([^\n]*)\n") do
      table.insert(lines, line)
    end
    
    if #lines == 0 then
      lines = {""}
    end
  end
end

-- Helper functions
local function setStatus(msg)
  statusMsg = msg
  statusTime = computer.uptime()
end

local function draw()
  component.invoke(gpu, "setBackground", 0x000000)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  component.invoke(gpu, "fill", 1, 1, w, h, " ")
  
  -- Draw lines
  for y = 1, h - 1 do
    local lineNum = y + scrollY
    if lineNum <= #lines then
      local line = lines[lineNum] or ""
      local displayLine = line:sub(scrollX + 1, scrollX + w)
      component.invoke(gpu, "set", 1, y, displayLine)
    end
  end
  
  -- Draw status bar
  component.invoke(gpu, "setBackground", 0x0000FF)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  component.invoke(gpu, "fill", 1, h, w, 1, " ")
  
  local filename = path:match("([^/]+)$") or path
  if dirty then filename = filename .. " *" end
  local status = " " .. filename .. " | Ln " .. cursorY .. ", Col " .. cursorX
  component.invoke(gpu, "set", 1, h, status)
  
  -- Show status message
  if statusMsg ~= "" and computer.uptime() - statusTime < 3 then
    local msgStart = w - #statusMsg - 1
    component.invoke(gpu, "set", msgStart, h, statusMsg .. " ")
  end
  
  -- Show help hints
  component.invoke(gpu, "set", w - 20, h, "Ctrl+S:Save Ctrl+Q:Quit")
  
  component.invoke(gpu, "setBackground", 0x000000)
  
  -- Position cursor
  local screenX = cursorX - scrollX
  local screenY = cursorY - scrollY
  if screenX >= 1 and screenX <= w and screenY >= 1 and screenY <= h - 1 then
    component.invoke(gpu, "setBackground", 0xFFFFFF)
    component.invoke(gpu, "setForeground", 0x000000)
    local char = (lines[cursorY] or ""):sub(cursorX, cursorX)
    if char == "" then char = " " end
    component.invoke(gpu, "set", screenX, screenY, char)
    component.invoke(gpu, "setBackground", 0x000000)
    component.invoke(gpu, "setForeground", 0xFFFFFF)
  end
end

local function save()
  local content = table.concat(lines, "\n")
  local handle = component.invoke(bootFs, "open", path, "w")
  if handle then
    component.invoke(bootFs, "write", handle, content)
    component.invoke(bootFs, "close", handle)
    dirty = false
    setStatus("Saved!")
    return true
  else
    setStatus("Save failed!")
    return false
  end
end

local function insertChar(char)
  local line = lines[cursorY] or ""
  lines[cursorY] = line:sub(1, cursorX - 1) .. char .. line:sub(cursorX)
  cursorX = cursorX + 1
  dirty = true
end

local function deleteChar()
  if cursorX > 1 then
    local line = lines[cursorY] or ""
    lines[cursorY] = line:sub(1, cursorX - 2) .. line:sub(cursorX)
    cursorX = cursorX - 1
    dirty = true
  elseif cursorY > 1 then
    local prevLine = lines[cursorY - 1]
    cursorX = #prevLine + 1
    lines[cursorY - 1] = prevLine .. (lines[cursorY] or "")
    table.remove(lines, cursorY)
    cursorY = cursorY - 1
    dirty = true
  end
end

local function newLine()
  local line = lines[cursorY] or ""
  local before = line:sub(1, cursorX - 1)
  local after = line:sub(cursorX)
  lines[cursorY] = before
  table.insert(lines, cursorY + 1, after)
  cursorY = cursorY + 1
  cursorX = 1
  dirty = true
end

local function updateScroll()
  -- Horizontal scroll
  if cursorX <= scrollX then
    scrollX = math.max(0, cursorX - 10)
  elseif cursorX > scrollX + w - 1 then
    scrollX = cursorX - w + 10
  end
  
  -- Vertical scroll
  if cursorY <= scrollY then
    scrollY = math.max(0, cursorY - 1)
  elseif cursorY > scrollY + h - 2 then
    scrollY = cursorY - h + 2
  end
end

-- Key code constants
local KEY_UP = 200
local KEY_DOWN = 208
local KEY_LEFT = 203
local KEY_RIGHT = 205
local KEY_HOME = 199
local KEY_END = 207
local KEY_ENTER = 28
local KEY_BACKSPACE = 14
local KEY_DELETE = 211
local KEY_S = 31
local KEY_Q = 16

-- Main loop
draw()

while running do
  local event, _, char, code = computer.pullSignal()
  
  if event == "key_down" then
    -- Ctrl+S = Save
    if code == KEY_S and char == 19 then
      save()
    -- Ctrl+Q = Quit
    elseif code == KEY_Q and char == 17 then
      if dirty then
        setStatus("Unsaved changes! Press Ctrl+Q again")
        draw()
        local ev2, _, ch2, co2 = computer.pullSignal()
        if ev2 == "key_down" and co2 == KEY_Q and ch2 == 17 then
          running = false
        end
      else
        running = false
      end
    -- Arrow keys
    elseif code == KEY_UP then
      if cursorY > 1 then
        cursorY = cursorY - 1
        cursorX = math.min(cursorX, #(lines[cursorY] or "") + 1)
      end
    elseif code == KEY_DOWN then
      if cursorY < #lines then
        cursorY = cursorY + 1
        cursorX = math.min(cursorX, #(lines[cursorY] or "") + 1)
      end
    elseif code == KEY_LEFT then
      if cursorX > 1 then
        cursorX = cursorX - 1
      elseif cursorY > 1 then
        cursorY = cursorY - 1
        cursorX = #(lines[cursorY] or "") + 1
      end
    elseif code == KEY_RIGHT then
      if cursorX <= #(lines[cursorY] or "") then
        cursorX = cursorX + 1
      elseif cursorY < #lines then
        cursorY = cursorY + 1
        cursorX = 1
      end
    elseif code == KEY_HOME then
      cursorX = 1
    elseif code == KEY_END then
      cursorX = #(lines[cursorY] or "") + 1
    -- Typing
    elseif code == KEY_ENTER then
      newLine()
    elseif code == KEY_BACKSPACE then
      deleteChar()
    elseif code == KEY_DELETE then
      local line = lines[cursorY] or ""
      if cursorX <= #line then
        lines[cursorY] = line:sub(1, cursorX - 1) .. line:sub(cursorX + 1)
        dirty = true
      elseif cursorY < #lines then
        lines[cursorY] = line .. (lines[cursorY + 1] or "")
        table.remove(lines, cursorY + 1)
        dirty = true
      end
    elseif char >= 32 and char < 127 then
      insertChar(string.char(char))
    end
    
    updateScroll()
    draw()
  end
end

-- Restore screen
term.clear()
