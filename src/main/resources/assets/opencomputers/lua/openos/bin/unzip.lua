-- bin/unzip.lua - Extract archives created by zip.lua

local shell = require("shell")
local fs    = require("filesystem")

local args = shell.parse(...)

if #args < 1 then
  io.write("Usage: unzip <archive.zip> [destdir]\n")
  os.exit(1)
end

local MAGIC = "OC\1Z"

local archivePath = shell.resolve(args[1])
local destDir     = args[2] and shell.resolve(args[2]) or shell.getWorkingDirectory()

local f, err = io.open(archivePath, "rb")
if not f then
  io.stderr:write("unzip: " .. args[1] .. ": " .. (err or "cannot open") .. "\n")
  os.exit(1)
end

local magic = f:read(4)
if magic ~= MAGIC then
  io.stderr:write("unzip: " .. args[1] .. ": not a valid zip archive\n")
  f:close()
  os.exit(1)
end

-- Ensure destination directory exists
if not fs.exists(destDir) then
  local ok, mkErr = fs.makeDirectory(destDir)
  if not ok then
    io.stderr:write("unzip: cannot create dest dir: " .. (mkErr or "") .. "\n")
    f:close()
    os.exit(1)
  end
end

local count = 0
while true do
  local header = f:read(2)
  if not header or #header < 2 then break end
  local nameLen = header:byte(1) * 256 + header:byte(2)
  local name = f:read(nameLen)
  if not name or #name < nameLen then break end

  local sizeB = f:read(4)
  if not sizeB or #sizeB < 4 then break end
  local size = sizeB:byte(1)*16777216 + sizeB:byte(2)*65536 + sizeB:byte(3)*256 + sizeB:byte(4)

  local data = f:read(size)
  if not data then break end

  -- Compute output path: strip leading path components relative to dest
  local outName = name:match("[^/\\]+$") or name
  local outPath = destDir:gsub("[/\\]$", "") .. "/" .. outName

  -- Create parent directories if needed
  local parentDir = outPath:match("(.+)/[^/]+$")
  if parentDir and not fs.exists(parentDir) then
    fs.makeDirectory(parentDir)
  end

  local outFile, outErr = io.open(outPath, "wb")
  if not outFile then
    io.stderr:write("unzip: cannot write " .. outName .. ": " .. (outErr or "") .. "\n")
  else
    outFile:write(data)
    outFile:close()
    io.write("  inflating: " .. outPath .. "\n")
    count = count + 1
  end
end

f:close()
io.write(string.format("Extracted %d files to %s\n", count, destDir))
