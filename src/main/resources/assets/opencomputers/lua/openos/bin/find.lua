-- bin/find.lua - Find files matching criteria

local shell = require("shell")
local fs    = require("filesystem")

local args, opts = shell.parse(...)

if #args == 0 then
  io.write("Usage: find <dir> [-name <pat>] [-type <f|d>] [-size <+/-n>]\n")
  os.exit(1)
end

local root     = shell.resolve(args[1])
local namePat  = opts["name"]
local typeOpt  = opts["type"]   -- "f" or "d"
local sizeOpt  = opts["size"]   -- "+n", "-n", or "n"

-- Convert shell glob pattern to Lua pattern
local function globToPattern(g)
  local p = g:gsub("([%.%+%-%^%$%(%)%[%]{}])", "%%%1")
              :gsub("%*", ".*")
              :gsub("%?", ".")
  return "^" .. p .. "$"
end

-- Parse size spec: "+1024" = >1024, "-1024" = <1024, "1024" = ==1024
local function parseSizeSpec(spec)
  if not spec then return nil end
  local sign, num = spec:match("^([%+%-]?)(%d+)$")
  if not num then return nil end
  return sign or "=", tonumber(num)
end

local namePatt  = namePat and globToPattern(namePat) or nil
local sizeSign, sizeNum = parseSizeSpec(sizeOpt)

-- Recursive walk
local function walk(path)
  local isDir = fs.isDirectory(path)
  local name  = fs.name(path)

  -- Apply filters
  local matches = true

  if namePatt and not name:match(namePatt) then
    matches = false
  end

  if typeOpt then
    if typeOpt == "f" and isDir then matches = false end
    if typeOpt == "d" and not isDir then matches = false end
  end

  if sizeNum and not isDir then
    local size = fs.size(path) or 0
    if sizeSign == "+" and size <= sizeNum then matches = false end
    if sizeSign == "-" and size >= sizeNum then matches = false end
    if sizeSign == "=" and size ~= sizeNum then matches = false end
  end

  if matches then
    io.write(path .. "\n")
  end

  if isDir then
    local ok, iter, state = pcall(fs.list, path)
    if ok and iter then
      for entry in iter do
        walk(path .. "/" .. entry)
      end
    end
  end
end

if not fs.exists(root) then
  io.write("find: " .. root .. ": No such file or directory\n")
  os.exit(1)
end

walk(root)
