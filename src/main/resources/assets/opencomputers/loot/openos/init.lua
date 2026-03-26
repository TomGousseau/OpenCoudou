-- OpenOS Init Script
-- This is the entry point for the OpenOS operating system.

local component = component
local computer = computer

-- ========================================
-- Basic Output
-- ========================================

local gpu = component.list("gpu")()
local screen = component.list("screen")()

if gpu and screen then
  component.invoke(gpu, "bind", screen)
end

local function write(text)
  if gpu then
    local w, h = component.invoke(gpu, "getResolution")
    component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
    component.invoke(gpu, "fill", 1, h, w, 1, " ")
    component.invoke(gpu, "set", 1, h, tostring(text))
  end
end

local function print(...)
  local args = {...}
  local text = ""
  for i, v in ipairs(args) do
    if i > 1 then text = text .. "\t" end
    text = text .. tostring(v)
  end
  write(text)
end

-- ========================================
-- Filesystem Mounting
-- ========================================

local bootFs = computer.getBootAddress()
local filesystem = setmetatable({}, {
  __index = function(_, method)
    return function(...)
      return component.invoke(bootFs, method, ...)
    end
  end
})

-- ========================================
-- Package System
-- ========================================

local loaded = {}
local loading = {}

local function loadfile(path)
  local handle = filesystem.open(path, "r")
  if not handle then
    return nil, "file not found: " .. path
  end
  
  local code = ""
  repeat
    local chunk = filesystem.read(handle, 4096)
    if chunk then
      code = code .. chunk
    end
  until not chunk
  filesystem.close(handle)
  
  return load(code, "=" .. path, "t", _G)
end

local function require(module)
  if loaded[module] then
    return loaded[module]
  end
  
  if loading[module] then
    error("circular dependency: " .. module)
  end
  
  loading[module] = true
  
  -- Search paths
  local paths = {
    "/lib/" .. module .. ".lua",
    "/lib/" .. module .. "/init.lua",
    "/usr/lib/" .. module .. ".lua"
  }
  
  local loader, path
  for _, p in ipairs(paths) do
    if filesystem.exists(p) then
      local err
      loader, err = loadfile(p)
      if loader then
        path = p
        break
      end
    end
  end
  
  if not loader then
    loading[module] = nil
    error("module not found: " .. module)
  end
  
  local result = loader(module, path)
  if result == nil then
    result = true
  end
  
  loaded[module] = result
  loading[module] = nil
  
  return result
end

_G.require = require
_G.print = print

-- ========================================
-- Terminal Interface
-- ========================================

local term = {}

term.cursorX = 1
term.cursorY = 1

function term.clear()
  if gpu then
    local w, h = component.invoke(gpu, "getResolution")
    component.invoke(gpu, "fill", 1, 1, w, h, " ")
    term.cursorX = 1
    term.cursorY = 1
  end
end

function term.setCursor(x, y)
  term.cursorX = x
  term.cursorY = y
end

function term.getCursor()
  return term.cursorX, term.cursorY
end

function term.write(text)
  if not gpu then return end
  
  local w, h = component.invoke(gpu, "getResolution")
  text = tostring(text)
  
  for i = 1, #text do
    local char = text:sub(i, i)
    
    if char == "\n" then
      term.cursorX = 1
      term.cursorY = term.cursorY + 1
    elseif char == "\r" then
      term.cursorX = 1
    elseif char == "\t" then
      term.cursorX = ((term.cursorX - 1) // 8 + 1) * 8 + 1
    else
      component.invoke(gpu, "set", term.cursorX, term.cursorY, char)
      term.cursorX = term.cursorX + 1
    end
    
    if term.cursorX > w then
      term.cursorX = 1
      term.cursorY = term.cursorY + 1
    end
    
    if term.cursorY > h then
      component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
      component.invoke(gpu, "fill", 1, h, w, 1, " ")
      term.cursorY = h
    end
  end
end

function term.read(prompt)
  if prompt then
    term.write(prompt)
  end
  
  local line = ""
  local startX = term.cursorX
  
  while true do
    local event, _, char, code = computer.pullSignal()
    
    if event == "key_down" then
      if code == 28 then -- Enter
        term.write("\n")
        return line
      elseif code == 14 then -- Backspace
        if #line > 0 then
          line = line:sub(1, -2)
          term.cursorX = term.cursorX - 1
          component.invoke(gpu, "set", term.cursorX, term.cursorY, " ")
        end
      elseif char >= 32 and char < 127 then
        line = line .. string.char(char)
        term.write(string.char(char))
      end
    end
  end
end

_G.term = term

-- ========================================
-- Shell
-- ========================================

local shell = {}
shell.aliases = {
  dir = "ls",
  cls = "clear",
  cp = "copy",
  mv = "move",
  rm = "del"
}

shell.path = "/bin:/usr/bin"

function shell.resolve(program)
  -- Check aliases
  local resolved = shell.aliases[program] or program
  
  -- Check if already a path
  if resolved:find("/") then
    if filesystem.exists(resolved) then
      return resolved
    end
    return nil
  end
  
  -- Search path
  for dir in shell.path:gmatch("[^:]+") do
    local path = dir .. "/" .. resolved .. ".lua"
    if filesystem.exists(path) then
      return path
    end
    path = dir .. "/" .. resolved
    if filesystem.exists(path) then
      return path
    end
  end
  
  return nil
end

function shell.execute(command)
  -- Parse command
  local parts = {}
  for part in command:gmatch("%S+") do
    table.insert(parts, part)
  end
  
  if #parts == 0 then
    return true
  end
  
  local program = parts[1]
  local args = {table.unpack(parts, 2)}
  
  -- Built-in commands
  if program == "cd" then
    shell.cd(args[1] or "/")
    return true
  elseif program == "exit" then
    return false
  end
  
  -- Find program
  local path = shell.resolve(program)
  if not path then
    print("Command not found: " .. program)
    return true
  end
  
  -- Load and execute
  local func, err = loadfile(path)
  if not func then
    print("Error loading: " .. tostring(err))
    return true
  end
  
  local ok, result = pcall(func, table.unpack(args))
  if not ok then
    print("Error: " .. tostring(result))
  end
  
  return true
end

shell.workingDir = "/"

function shell.cd(path)
  if path:sub(1, 1) ~= "/" then
    path = shell.workingDir .. "/" .. path
  end
  
  -- Normalize path
  local parts = {}
  for part in path:gmatch("[^/]+") do
    if part == ".." then
      table.remove(parts)
    elseif part ~= "." then
      table.insert(parts, part)
    end
  end
  
  path = "/" .. table.concat(parts, "/")
  
  if filesystem.isDirectory(path) then
    shell.workingDir = path
  else
    print("Not a directory: " .. path)
  end
end

function shell.pwd()
  return shell.workingDir
end

_G.shell = shell

-- ========================================
-- Main
-- ========================================

term.clear()
component.invoke(gpu, "setForeground", 0x00FF00)
term.write("OpenOS 1.0\n")
component.invoke(gpu, "setForeground", 0xFFFFFF)
term.write("Type 'help' for available commands.\n\n")

-- Main shell loop
while true do
  component.invoke(gpu, "setForeground", 0x00FFFF)
  term.write(shell.workingDir)
  component.invoke(gpu, "setForeground", 0xFFFFFF)
  term.write("> ")
  
  local command = term.read()
  if command and #command > 0 then
    if not shell.execute(command) then
      break
    end
  end
end

print("Goodbye!")
