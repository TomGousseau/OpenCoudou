-- cp.lua - Copy files
local shell = require("shell")
local fs = require("filesystem")

local args = { ... }
local recursive = false
local sources = {}
local dest = nil
local verbose = false

for _, a in ipairs(args) do
    if a == "-r" or a == "-R" or a == "--recursive" then
        recursive = true
    elseif a == "-v" or a == "--verbose" then
        verbose = true
    elseif a:sub(1,1) ~= "-" then
        table.insert(sources, shell.resolve(a))
    end
end

if #sources < 2 then
    io.stderr:write("usage: cp [-r] <source> [source2 ...] <dest>\n")
    os.exit(1)
end

dest = table.remove(sources)

local function copyFile(src, dst)
    if verbose then print(src .. " -> " .. dst) end
    local ok, err = fs.copy(src, dst)
    if not ok then
        io.stderr:write("cp: cannot copy " .. src .. " to " .. dst .. ": " .. tostring(err) .. "\n")
        return false
    end
    return true
end

local function copyRecursive(src, dst)
    if fs.isDirectory(src) then
        if not recursive then
            io.stderr:write("cp: " .. src .. " is a directory (use -r)\n")
            return false
        end
        fs.makeDirectory(dst)
        local files = fs.list(src)
        if files then
            for _, f in ipairs(files) do
                copyRecursive(fs.concat(src, f), fs.concat(dst, f))
            end
        end
    else
        copyFile(src, dst)
    end
end

local destIsDir = fs.isDirectory(dest)

if #sources > 1 and not destIsDir then
    io.stderr:write("cp: destination must be a directory when copying multiple files\n")
    os.exit(1)
end

for _, src in ipairs(sources) do
    local dstPath = destIsDir and fs.concat(dest, fs.name(src)) or dest
    copyRecursive(src, dstPath)
end
