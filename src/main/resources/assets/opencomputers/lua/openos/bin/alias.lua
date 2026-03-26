-- alias.lua - Shell alias management command
-- Usage: alias              -> list all aliases
--        alias name         -> show value of alias
--        alias name=value   -> set alias
--        alias -d name      -> delete alias
--        unalias name       -> delete alias (same as alias -d)

local shell = require("shell")
local args, opts = shell.parse(...)

local function usage()
    io.write("Usage: alias [name[=value]]\n")
    io.write("       alias -d name\n")
end

-- Delete mode
if opts.d then
    if #args == 0 then
        io.stderr:write("alias: -d requires a name\n")
        return 1
    end
    for _, name in ipairs(args) do
        shell.unalias(name)
    end
    return 0
end

-- No arguments: list all aliases
if #args == 0 then
    local aliases = shell.aliases()
    local names = {}
    for name in pairs(aliases) do
        table.insert(names, name)
    end
    table.sort(names)
    if #names == 0 then
        print("No aliases defined.")
    else
        for _, name in ipairs(names) do
            print(string.format("alias %s='%s'", name, aliases[name]))
        end
    end
    return 0
end

-- Process each argument
for _, arg in ipairs(args) do
    local name, value = arg:match("^([^=]+)=(.*)$")
    if name then
        -- Set alias
        shell.alias(name, value)
    else
        -- Show alias
        local aliases = shell.aliases()
        if aliases[arg] then
            print(string.format("alias %s='%s'", arg, aliases[arg]))
        else
            io.stderr:write("alias: " .. arg .. ": not found\n")
            return 1
        end
    end
end

return 0
