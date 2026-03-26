-- ls command: list directory contents

local args = {...}
local path = args[1] or shell.pwd()

-- Resolve relative paths
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()

-- Check if path exists
if not component.invoke(bootFs, "exists", path) then
  print("No such file or directory: " .. path)
  return
end

-- Check if it's a directory
if not component.invoke(bootFs, "isDirectory", path) then
  -- It's a file, just print its name
  print(path:match("([^/]+)$"))
  return
end

-- List directory contents
local files = component.invoke(bootFs, "list", path)
if not files then
  print("Cannot list directory")
  return
end

-- Sort entries
table.sort(files)

-- Print entries
for _, entry in ipairs(files) do
  local fullPath = path .. "/" .. entry
  local isDir = component.invoke(bootFs, "isDirectory", fullPath)
  
  if isDir then
    component.invoke(component.list("gpu")(), "setForeground", 0x00FFFF)
    print(entry .. "/")
    component.invoke(component.list("gpu")(), "setForeground", 0xFFFFFF)
  else
    local size = component.invoke(bootFs, "size", fullPath)
    print(entry .. " (" .. size .. " bytes)")
  end
end
