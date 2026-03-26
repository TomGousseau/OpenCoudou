-- mv.lua - Move/rename files
local shell = require("shell")
local fs = require("filesystem")

local args = { ... }
local sources = {}
local verbose = false

for _, a in ipairs(args) do
    if a == "-v" or a == "--verbose" then
        verbose = true
    elseif a:sub(1,1) ~= "-" then
        table.insert(sources, shell.resolve(a))
    end
end

if #sources < 2 then
    io.stderr:write("usage: mv <source> [source2 ...] <dest>\n")
    os.exit(1)
end

local dest = table.remove(sources)
local destIsDir = fs.isDirectory(dest)

if #sources > 1 and not destIsDir then
    io.stderr:write("mv: destination must be a directory for multiple sources\n")
    os.exit(1)
end

for _, src in ipairs(sources) do
    local dstPath = destIsDir and fs.concat(dest, fs.name(src)) or dest
    if verbose then print(src .. " -> " .. dstPath) end
    local ok, err = fs.rename(src, dstPath)
    if not ok then
        io.stderr:write("mv: cannot move " .. src .. " to " .. dstPath .. ": " .. tostring(err) .. "\n")
    end
end
