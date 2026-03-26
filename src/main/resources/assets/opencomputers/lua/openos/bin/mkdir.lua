-- mkdir.lua - Create directories
local shell = require("shell")
local fs = require("filesystem")

local args = { ... }
local parents = false
local verbose = false
local dirs = {}

for _, a in ipairs(args) do
    if a == "-p" or a == "--parents" then
        parents = true
    elseif a == "-v" or a == "--verbose" then
        verbose = true
    elseif a:sub(1,1) ~= "-" then
        table.insert(dirs, shell.resolve(a))
    end
end

if #dirs == 0 then
    io.stderr:write("usage: mkdir [-p] <directory> [dir2 ...]\n")
    os.exit(1)
end

local function mkdirParents(path)
    if fs.exists(path) then
        if not fs.isDirectory(path) then
            io.stderr:write("mkdir: " .. path .. ": exists but is not a directory\n")
            return false
        end
        return true
    end
    local parent = fs.path(path)
    if parent ~= path and parent ~= "/" then
        if not mkdirParents(parent) then return false end
    end
    if verbose then print("created directory: " .. path) end
    return fs.makeDirectory(path)
end

for _, dir in ipairs(dirs) do
    if parents then
        local ok = mkdirParents(dir)
        if not ok then
            io.stderr:write("mkdir: cannot create directory: " .. dir .. "\n")
        end
    else
        if fs.exists(dir) then
            io.stderr:write("mkdir: " .. dir .. ": already exists\n")
        else
            if verbose then print("created directory: " .. dir) end
            local ok, err = fs.makeDirectory(dir)
            if not ok then
                io.stderr:write("mkdir: cannot create " .. dir .. ": " .. tostring(err) .. "\n")
            end
        end
    end
end
