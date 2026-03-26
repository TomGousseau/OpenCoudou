-- wc command: word/line/character count

local args = {...}
local path = args[1]

if not path then
  print("Usage: wc <file>")
  return
end

-- Resolve path
if path:sub(1, 1) ~= "/" then
  path = shell.pwd() .. "/" .. path
end

local bootFs = computer.getBootAddress()

if not component.invoke(bootFs, "exists", path) then
  print("File not found: " .. path)
  return
end

local handle = component.invoke(bootFs, "open", path, "r")
if not handle then
  print("Cannot open file")
  return
end

local lines = 0
local words = 0
local chars = 0

repeat
  local chunk = component.invoke(bootFs, "read", handle, 4096)
  if chunk then
    chars = chars + #chunk
    for _ in chunk:gmatch("\n") do
      lines = lines + 1
    end
    for _ in chunk:gmatch("%S+") do
      words = words + 1
    end
  end
until not chunk

component.invoke(bootFs, "close", handle)

print(string.format("%d lines, %d words, %d characters", lines, words, chars))
