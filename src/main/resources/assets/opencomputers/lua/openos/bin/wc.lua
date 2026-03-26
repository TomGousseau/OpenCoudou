-- bin/wc.lua - Word, line, and byte count

local shell = require("shell")

local args, opts = shell.parse(...)

local countLines = opts["l"]
local countWords = opts["w"]
local countBytes = opts["c"]

-- If no options given, show all
if not countLines and not countWords and not countBytes then
  countLines = true
  countWords = true
  countBytes = true
end

local function countStream(stream)
  local lines, words, bytes = 0, 0, 0
  for line in stream:lines() do
    lines = lines + 1
    bytes = bytes + #line + 1 -- +1 for newline
    for _ in line:gmatch("%S+") do
      words = words + 1
    end
  end
  return lines, words, bytes
end

local function printCounts(lines, words, bytes, fname)
  local parts = {}
  if countLines then table.insert(parts, string.format("%7d", lines)) end
  if countWords then table.insert(parts, string.format("%7d", words)) end
  if countBytes then table.insert(parts, string.format("%7d", bytes)) end
  if fname then table.insert(parts, " " .. fname) end
  io.write(table.concat(parts) .. "\n")
end

if #args == 0 then
  -- Read from stdin
  local lines, words, bytes = countStream(io.stdin)
  printCounts(lines, words, bytes)
else
  local totalL, totalW, totalB = 0, 0, 0
  for _, file in ipairs(args) do
    local path = require("shell").resolve(file)
    local f, err = io.open(path, "r")
    if not f then
      io.stderr:write("wc: " .. file .. ": " .. (err or "cannot open") .. "\n")
    else
      local l, w, b = countStream(f)
      f:close()
      printCounts(l, w, b, file)
      totalL = totalL + l
      totalW = totalW + w
      totalB = totalB + b
    end
  end
  if #args > 1 then
    printCounts(totalL, totalW, totalB, "total")
  end
end
