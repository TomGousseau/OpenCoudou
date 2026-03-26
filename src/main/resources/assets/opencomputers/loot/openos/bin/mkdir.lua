-- mkdir command: create directory

local args = {...}
local path = args[1]

if not path then
  print("Usage: mkdir <directory>")
  return
end

-- Resolve relative paths
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()

-- Check if already exists
if component.invoke(bootFs, "exists", path) then
  print("Already exists: " .. path)
  return
end

-- Create directory
local ok = component.invoke(bootFs, "makeDirectory", path)
if ok then
  print("Created: " .. path)
else
  print("Failed to create: " .. path)
end
