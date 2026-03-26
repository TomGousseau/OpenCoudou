-- bin/sort.lua - Sort lines of text

local shell = require("shell")

local args, opts = shell.parse(...)

local reverse = opts["r"]
local numeric = opts["n"]
local unique  = opts["u"]

-- Read all lines from a file or stdin
local function readLines(source)
  local lines = {}
  for line in source:lines() do
    table.insert(lines, line)
  end
  return lines
end

local lines = {}

if #args == 0 then
  lines = readLines(io.stdin)
else
  for _, file in ipairs(args) do
    local path = require("shell").resolve(file)
    local f, err = io.open(path, "r")
    if not f then
      io.stderr:write("sort: " .. file .. ": " .. (err or "cannot open") .. "\n")
      os.exit(1)
    end
    local flines = readLines(f)
    f:close()
    for _, l in ipairs(flines) do
      table.insert(lines, l)
    end
  end
end

-- Sort comparator
local function cmp(a, b)
  if numeric then
    local na = tonumber(a:match("^%s*(%-?%d+%.?%d*)")) or 0
    local nb = tonumber(b:match("^%s*(%-?%d+%.?%d*)")) or 0
    return reverse and (na > nb) or (na < nb)
  else
    if reverse then
      return a > b
    else
      return a < b
    end
  end
end

table.sort(lines, cmp)

-- Unique filter
if unique then
  local seen = {}
  local uniq = {}
  for _, l in ipairs(lines) do
    if not seen[l] then
      seen[l] = true
      table.insert(uniq, l)
    end
  end
  lines = uniq
end

for _, l in ipairs(lines) do
  io.write(l .. "\n")
end
