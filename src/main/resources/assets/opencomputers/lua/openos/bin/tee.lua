-- bin/tee.lua - Read stdin, write to stdout AND a file

local shell = require("shell")

local args, opts = shell.parse(...)
local append = opts["a"]

if #args == 0 then
  io.write("Usage: tee [-a] <file>\n")
  os.exit(1)
end

local path = require("shell").resolve(args[1])
local mode = append and "a" or "w"

local f, err = io.open(path, mode)
if not f then
  io.stderr:write("tee: " .. args[1] .. ": " .. (err or "cannot open") .. "\n")
  os.exit(1)
end

-- Read stdin line by line, write to both stdout and file
for line in io.stdin:lines() do
  io.write(line .. "\n")
  f:write(line .. "\n")
end

f:close()
