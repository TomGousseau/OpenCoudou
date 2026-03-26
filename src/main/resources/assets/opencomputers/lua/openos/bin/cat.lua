-- cat.lua - Concatenate and print files
local shell = require("shell")
local fs = require("filesystem")

local args = { ... }
local showLineNumbers = false
local files = {}

for _, a in ipairs(args) do
    if a == "-n" or a == "--number" then
        showLineNumbers = true
    elseif a ~= "--" then
        table.insert(files, shell.resolve(a))
    end
end

if #files == 0 then
    -- Read from stdin
    while true do
        local line = io.read("*l")
        if not line then break end
        print(line)
    end
    return
end

for _, path in ipairs(files) do
    if not fs.exists(path) then
        io.stderr:write("cat: " .. path .. ": no such file\n")
    elseif fs.isDirectory(path) then
        io.stderr:write("cat: " .. path .. ": is a directory\n")
    else
        local f, err = io.open(path, "r")
        if not f then
            io.stderr:write("cat: " .. path .. ": " .. tostring(err) .. "\n")
        else
            local lineNum = 1
            for line in f:lines() do
                if showLineNumbers then
                    io.write(string.format("%6d\t", lineNum))
                    lineNum = lineNum + 1
                end
                print(line)
            end
            f:close()
        end
    end
end
