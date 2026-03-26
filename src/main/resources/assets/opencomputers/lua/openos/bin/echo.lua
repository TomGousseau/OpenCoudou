-- echo.lua - Print arguments
local args = { ... }
local noNewline = false

if args[1] == "-n" then
    noNewline = true
    table.remove(args, 1)
end

local output = table.concat(args, " ")
if noNewline then
    io.write(output)
else
    print(output)
end
