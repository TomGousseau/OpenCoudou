-- cd.lua - Change directory
local shell = require("shell")
local args = { ... }
local target = args[1]

if not target or target == "~" then
    local home = os.getenv and os.getenv("HOME") or "/home/root"
    target = home
end

local ok, err = shell.setWorkingDirectory(shell.resolve(target))
if not ok then
    io.stderr:write("cd: " .. tostring(err) .. "\n")
    os.exit(1)
end
