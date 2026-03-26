-- cp command: copy files

local args = {...}
local src = args[1]
local dest = args[2]

if not src or not dest then
  print("Usage: cp <source> <destination>")
  return
end

-- Resolve paths
if src:sub(1, 1) ~= "/" then
  src = shell.pwd() .. "/" .. src
end
if dest:sub(1, 1) ~= "/" then
  dest = shell.pwd() .. "/" .. dest
end

local bootFs = computer.getBootAddress()

-- Check source exists
if not component.invoke(bootFs, "exists", src) then
  print("Source not found: " .. src)
  return
end

-- If dest is directory, append filename
if component.invoke(bootFs, "isDirectory", dest) then
  local filename = src:match("([^/]+)$")
  dest = dest .. "/" .. filename
end

-- Copy file
local srcHandle = component.invoke(bootFs, "open", src, "r")
if not srcHandle then
  print("Cannot read source")
  return
end

local destHandle = component.invoke(bootFs, "open", dest, "w")
if not destHandle then
  component.invoke(bootFs, "close", srcHandle)
  print("Cannot write destination")
  return
end

repeat
  local chunk = component.invoke(bootFs, "read", srcHandle, 4096)
  if chunk then
    component.invoke(bootFs, "write", destHandle, chunk)
  end
until not chunk

component.invoke(bootFs, "close", srcHandle)
component.invoke(bootFs, "close", destHandle)

print("Copied to: " .. dest)
