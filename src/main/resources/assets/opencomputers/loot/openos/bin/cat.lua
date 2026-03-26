-- cat command: display file contents

local args = {...}
local path = args[1]

if not path then
  print("Usage: cat <file>")
  return
end

-- Resolve relative paths
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()

-- Check if file exists
if not component.invoke(bootFs, "exists", path) then
  print("No such file: " .. path)
  return
end

-- Check if it's a directory
if component.invoke(bootFs, "isDirectory", path) then
  print("Is a directory: " .. path)
  return
end

-- Read and display file
local handle = component.invoke(bootFs, "open", path, "r")
if not handle then
  print("Cannot open file")
  return
end

repeat
  local chunk = component.invoke(bootFs, "read", handle, 4096)
  if chunk then
    term.write(chunk)
  end
until not chunk

component.invoke(bootFs, "close", handle)
term.write("\n")
