-- OpenComputers BIOS
-- This is the first code executed when a computer starts.
-- It is responsible for finding and loading the operating system.

local component = component
local computer = computer

-- Colors for output
local function setForeground(color)
  if component.gpu then
    component.invoke(component.list("gpu")(), "setForeground", color)
  end
end

local function setBackground(color)
  if component.gpu then
    component.invoke(component.list("gpu")(), "setBackground", color)
  end
end

-- Simple text output
local function print(text)
  if component.gpu then
    local gpu = component.list("gpu")()
    local screen = component.list("screen")()
    if gpu and screen then
      component.invoke(gpu, "bind", screen)
      local w, h = component.invoke(gpu, "getResolution")
      -- Simple scrolling terminal
      component.invoke(gpu, "copy", 1, 2, w, h - 1, 0, -1)
      component.invoke(gpu, "fill", 1, h, w, 1, " ")
      component.invoke(gpu, "set", 1, h, text)
    end
  end
end

-- Error display
local function displayError(message)
  setForeground(0xFF0000)
  print("BIOS Error: " .. tostring(message))
  setForeground(0xFFFFFF)
end

-- Main BIOS routine
local function main()
  -- Initialize display
  local gpu = component.list("gpu")()
  local screen = component.list("screen")()
  
  if gpu and screen then
    component.invoke(gpu, "bind", screen)
    local w, h = component.invoke(gpu, "getResolution")
    component.invoke(gpu, "setBackground", 0x000000)
    component.invoke(gpu, "setForeground", 0xFFFFFF)
    component.invoke(gpu, "fill", 1, 1, w, h, " ")
  end
  
  setForeground(0x00FF00)
  print("OpenComputers BIOS v1.0")
  setForeground(0xFFFFFF)
  print("Looking for bootable medium...")
  
  -- Search for bootable filesystems
  local eeprom = component.list("eeprom")()
  local bootAddress = nil
  
  -- Check EEPROM for preferred boot address
  if eeprom then
    local data = component.invoke(eeprom, "getData")
    if data and #data > 0 then
      bootAddress = data
    end
  end
  
  -- Look for filesystems with /init.lua
  local filesystems = {}
  for address in component.list("filesystem") do
    if component.invoke(address, "exists", "/init.lua") then
      table.insert(filesystems, address)
    end
  end
  
  if #filesystems == 0 then
    displayError("No bootable medium found!")
    print("Insert a disk with /init.lua and press any key to retry.")
    computer.pullSignal()
    return main()
  end
  
  -- Use preferred or first filesystem
  local bootFs = bootAddress
  if not bootFs or not component.invoke(bootFs, "exists", "/init.lua") then
    bootFs = filesystems[1]
  end
  
  print("Booting from: " .. bootFs:sub(1, 8) .. "...")
  
  -- Load and execute init.lua
  local handle = component.invoke(bootFs, "open", "/init.lua", "r")
  if not handle then
    displayError("Could not open /init.lua")
    return
  end
  
  local code = ""
  repeat
    local chunk = component.invoke(bootFs, "read", handle, 4096)
    if chunk then
      code = code .. chunk
    end
  until not chunk
  component.invoke(bootFs, "close", handle)
  
  -- Compile and run
  local init, err = load(code, "=init.lua", "t", _G)
  if not init then
    displayError("Syntax error: " .. tostring(err))
    return
  end
  
  -- Set boot filesystem globally
  computer.setBootAddress(bootFs)
  
  -- Execute
  local ok, err = pcall(init)
  if not ok then
    displayError("Runtime error: " .. tostring(err))
  end
end

-- Run BIOS
local ok, err = pcall(main)
if not ok then
  displayError("Critical error: " .. tostring(err))
end

-- Keep computer running if we get here
while true do
  computer.pullSignal()
end
