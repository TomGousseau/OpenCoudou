-- bin/zip.lua - Simple file archive creator
-- Uses a custom format: 4-byte magic + for each file: 2-byte name len + name + 4-byte data len + data

local shell  = require("shell")
local fs     = require("filesystem")
local serial = require("serialization")

local args, opts = shell.parse(...)

local MAGIC = "OC\1Z"

local function usage()
  io.write("Usage: zip <archive.zip> <file...>\n")
  io.write("       zip -l <archive.zip>\n")
  os.exit(1)
end

if #args < 1 then usage() end

local archivePath = shell.resolve(args[1])

-- List mode
if opts["l"] then
  local f, err = io.open(archivePath, "rb")
  if not f then
    io.stderr:write("zip: " .. args[1] .. ": " .. (err or "cannot open") .. "\n")
    os.exit(1)
  end
  local magic = f:read(4)
  if magic ~= MAGIC then
    io.stderr:write("zip: " .. args[1] .. ": not a valid zip archive\n")
    f:close()
    os.exit(1)
  end
  io.write(string.format("%-40s  %10s\n", "Name", "Size"))
  io.write(string.rep("-", 52) .. "\n")
  local total = 0
  while true do
    local header = f:read(2)
    if not header or #header < 2 then break end
    local nameLen = header:byte(1) * 256 + header:byte(2)
    local name = f:read(nameLen)
    local sizeB = f:read(4)
    if not sizeB or #sizeB < 4 then break end
    local size = sizeB:byte(1)*16777216 + sizeB:byte(2)*65536 + sizeB:byte(3)*256 + sizeB:byte(4)
    f:read(size) -- skip data
    io.write(string.format("%-40s  %10d\n", name, size))
    total = total + size
  end
  io.write(string.rep("-", 52) .. "\n")
  io.write(string.format("Total: %d bytes\n", total))
  f:close()
  return
end

-- Create mode
if #args < 2 then usage() end

local out, err = io.open(archivePath, "wb")
if not out then
  io.stderr:write("zip: " .. args[1] .. ": " .. (err or "cannot create") .. "\n")
  os.exit(1)
end

out:write(MAGIC)
local count = 0

for i = 2, #args do
  local filePath = shell.resolve(args[i])
  if not fs.exists(filePath) then
    io.stderr:write("zip: " .. args[i] .. ": No such file\n")
  elseif fs.isDirectory(filePath) then
    io.stderr:write("zip: " .. args[i] .. ": is a directory (skipped)\n")
  else
    local f, ferr = io.open(filePath, "rb")
    if not f then
      io.stderr:write("zip: " .. args[i] .. ": " .. (ferr or "cannot read") .. "\n")
    else
      local data = f:read("*a")
      f:close()
      local name = args[i] -- store original name
      local namelen = #name
      -- name length as 2 bytes big-endian
      out:write(string.char(math.floor(namelen/256), namelen % 256))
      out:write(name)
      -- data length as 4 bytes big-endian
      local dlen = #data
      out:write(string.char(
        math.floor(dlen/16777216) % 256,
        math.floor(dlen/65536) % 256,
        math.floor(dlen/256) % 256,
        dlen % 256
      ))
      out:write(data)
      io.write("  adding: " .. name .. " (" .. dlen .. " bytes)\n")
      count = count + 1
    end
  end
end

out:close()
io.write(string.format("Created %s (%d files)\n", args[1], count))
