-- which.lua - Find a command in PATH
-- Usage: which [-a] command [command ...]
--   -a  show all matching files, not just the first

local shell = require("shell")
local args, opts = shell.parse(...)

if #args == 0 then
    io.stderr:write("Usage: which [-a] name...\n")
    return 1
end

local found = false

for _, name in ipairs(args) do
    -- Check if it's a shell alias first
    local aliases = shell.aliases()
    if aliases[name] then
        print(name .. ": aliased to " .. aliases[name])
        found = true
        if not opts.a then break end
    end
    
    -- Search through PATH directories
    local path = shell.getPath()
    local searched = {}
    
    for dir in path:gmatch("[^:]+") do
        if not searched[dir] then
            searched[dir] = true
            -- Try with .lua extension
            for _, ext in ipairs({ ".lua", "" }) do
                local fullPath = dir:gsub("/$", "") .. "/" .. name .. ext
                if require("filesystem").exists(fullPath) then
                    print(fullPath)
                    found = true
                    if not opts.a then
                        goto next_name
                    end
                    break
                end
            end
        end
    end
    
    ::next_name::
    if not found then
        io.stderr:write(name .. " not found\n")
    end
end

return found and 0 or 1
