-- lua.lua - Interactive Lua REPL / run Lua files
local term = require("term")
local shell = require("shell")
local text = require("text")
local serialization = require("serialization")

local args = { ... }

-- Run a file if specified
if args[1] and args[1]:sub(1,1) ~= "-" then
    local path = shell.resolve(args[1], "lua")
    local fileArgs = {}
    for i = 2, #args do fileArgs[i-1] = args[i] end
    return shell.runFile(path, table.unpack(fileArgs))
end

-- Interactive REPL
local history = {}
local chunk_lines = {}

term.setForeground(0x00AAFF)
print("Lua 5.4  OpenOS Interactive REPL")
print("Type 'exit' or press Ctrl+D to quit")
term.setForeground(0xFFFFFF)

local function tryLoad(code)
    local chunk, err = load("return " .. code, "=stdin", "t")
    if not chunk then
        chunk, err = load(code, "=stdin", "t")
    end
    return chunk, err
end

local function formatResult(...)
    local results = { ... }
    if #results == 0 then return nil end
    local parts = {}
    for _, v in ipairs(results) do
        table.insert(parts, serialization.serialize(v))
    end
    return table.concat(parts, "\t")
end

local running = true
local multiline = false

while running do
    local prompt = multiline and ">> " or "lua> "
    term.setForeground(0xAAFF00)
    io.write(prompt)
    term.setForeground(0xFFFFFF)
    
    local line = term.read(history)
    if not line then
        print("")
        running = false
        break
    end
    
    if line == "exit" or line == "quit" then
        running = false
        break
    end
    
    if line == "" and multiline then
        -- End multiline
        multiline = false
        local code = table.concat(chunk_lines, "\n")
        chunk_lines = {}
        
        local chunk, err = tryLoad(code)
        if not chunk then
            term.setForeground(0xFF4444)
            print("Error: " .. tostring(err))
            term.setForeground(0xFFFFFF)
        else
            local ok, result = pcall(chunk)
            if ok then
                if result ~= nil then
                    term.setForeground(0x88FFFF)
                    print(tostring(result))
                    term.setForeground(0xFFFFFF)
                end
            else
                term.setForeground(0xFF4444)
                print("Error: " .. tostring(result))
                term.setForeground(0xFFFFFF)
            end
        end
    elseif line ~= "" then
        if multiline then
            table.insert(chunk_lines, line)
        else
            -- Try to execute single line
            local chunk, err = tryLoad(line)
            if chunk then
                -- Single line works
                local ok, result = pcall(chunk)
                if ok then
                    if result ~= nil then
                        term.setForeground(0x88FFFF)
                        print(tostring(result))
                        term.setForeground(0xFFFFFF)
                    end
                else
                    term.setForeground(0xFF4444)
                    print("Error: " .. tostring(result))
                    term.setForeground(0xFFFFFF)
                end
            else
                -- Check if it looks like an incomplete block
                if err and (err:find("<eof>") or err:find("'end'") or line:sub(-2) == "do" or
                   line:sub(-4) == "then" or line:sub(-8) == "function") then
                    multiline = true
                    chunk_lines = { line }
                else
                    term.setForeground(0xFF4444)
                    print("Error: " .. tostring(err))
                    term.setForeground(0xFFFFFF)
                end
            end
        end
    end
end
