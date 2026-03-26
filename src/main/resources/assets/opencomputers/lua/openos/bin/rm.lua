-- rm.lua - Remove files
local shell = require("shell")
local fs = require("filesystem")

local args = { ... }
local recursive = false
local force = false
local verbose = false
local targets = {}

for _, a in ipairs(args) do
    if a == "-r" or a == "-R" or a == "--recursive" then
        recursive = true
    elseif a == "-f" or a == "--force" then
        force = true
    elseif a == "-v" or a == "--verbose" then
        verbose = true
    elseif a == "-rf" or a == "-fr" then
        recursive = true
        force = true
    elseif a:sub(1,1) ~= "-" then
        table.insert(targets, shell.resolve(a))
    end
end

if #targets == 0 then
    io.stderr:write("usage: rm [-rf] <file> [file2 ...]\n")
    os.exit(1)
end

local function remove(path)
    if not fs.exists(path) then
        if not force then
            io.stderr:write("rm: " .. path .. ": no such file or directory\n")
        end
        return
    end
    
    if fs.isDirectory(path) then
        if not recursive then
            io.stderr:write("rm: " .. path .. ": is a directory (use -r)\n")
            return
        end
        local files = fs.list(path)
        if files then
            for _, f in ipairs(files) do
                remove(fs.concat(path, f))
            end
        end
    end
    
    if verbose then print("removed: " .. path) end
    local ok, err = fs.remove(path)
    if not ok and not force then
        io.stderr:write("rm: cannot remove " .. path .. ": " .. tostring(err) .. "\n")
    end
end

for _, target in ipairs(targets) do
    remove(target)
end
