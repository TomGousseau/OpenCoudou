-- mv command: move/rename files

local args = {...}
local src = args[1]
local dest = args[2]

if not src or not dest then
  print("Usage: mv <source> <destination>")
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

-- Rename/move
local ok = component.invoke(bootFs, "rename", src, dest)
if ok then
  print("Moved to: " .. dest)
else
  print("Failed to move")
end
