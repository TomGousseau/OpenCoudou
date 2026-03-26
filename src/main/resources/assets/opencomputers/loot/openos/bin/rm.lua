-- rm command: remove file or directory

local args = {...}
local path = args[1]

if not path then
  print("Usage: rm <file>")
  return
end

-- Resolve relative paths
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()

-- Check if exists
if not component.invoke(bootFs, "exists", path) then
  print("No such file: " .. path)
  return
end

-- Remove
local ok = component.invoke(bootFs, "remove", path)
if ok then
  print("Removed: " .. path)
else
  print("Failed to remove: " .. path)
end
