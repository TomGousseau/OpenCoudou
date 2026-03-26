-- export.lua - Set or list environment variables
-- Usage: export                -> list all env vars
--        export NAME           -> export NAME (marks for child processes)
--        export NAME=VALUE     -> set and export NAME=VALUE
--        export -n NAME        -> unexport (remove) a variable

local shell = require("shell")
local process = require("process")
local args, opts = shell.parse(...)

local env = process.getenv()

-- List all
if #args == 0 then
    local names = {}
    for k in pairs(env) do
        table.insert(names, k)
    end
    table.sort(names)
    for _, k in ipairs(names) do
        local v = env[k]
        if type(v) == "string" then
            -- Quote if contains spaces or special chars
            if v:find("[%s\"'\\;&|<>]") then
                v = '"' .. v:gsub('"', '\\"') .. '"'
            end
            print("export " .. k .. "=" .. v)
        else
            print("export " .. k)
        end
    end
    return 0
end

-- Unexport
if opts.n then
    for _, name in ipairs(args) do
        env[name] = nil
    end
    return 0
end

-- Set/export each arg
for _, arg in ipairs(args) do
    local name, value = arg:match("^([A-Za-z_][A-Za-z0-9_]*)=(.*)$")
    if name then
        -- Expand $VAR references in value
        value = value:gsub("%$([A-Za-z_][A-Za-z0-9_]*)", function(vname)
            return env[vname] or ""
        end)
        value = value:gsub("%${([A-Za-z_][A-Za-z0-9_]*)}", function(vname)
            return env[vname] or ""
        end)
        env[name] = value
    elseif arg:match("^[A-Za-z_][A-Za-z0-9_]*$") then
        -- Just mark as exported (if already set, keep value; if not, set to empty)
        if env[arg] == nil then
            env[arg] = ""
        end
    else
        io.stderr:write("export: `" .. arg .. "': not a valid identifier\n")
        return 1
    end
end

return 0
