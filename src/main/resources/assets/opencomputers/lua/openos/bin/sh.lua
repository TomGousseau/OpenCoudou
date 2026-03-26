#!/usr/bin/env lua
-- sh.lua - Interactive shell for OpenOS

local shell = require("shell")
local term = require("term")
local text = require("text")
local fs = require("filesystem")
local event = require("event")
local process = require("process")

local sh = {}

local history = {}
local running = true

-- Get the prompt string
local function getPrompt()
    local cwd = shell.getWorkingDirectory()
    local user = process.getenv("USER") or "root"
    local hostname = process.getenv("HOSTNAME") or computer.address():sub(1, 8)
    local color = (user == "root") and "\x1b[1;31m" or "\x1b[1;32m"
    local reset = "\x1b[0m"
    
    -- Shorten home path
    local home = process.getenv("HOME") or "/home/" .. user
    if cwd:sub(1, #home) == home then
        cwd = "~" .. cwd:sub(#home + 1)
    end
    
    return string.format("%s%s@%s%s:%s$ ", color, user, hostname, reset, cwd)
end

-- Tab completion hint
local function hint(partial)
    -- Get last token
    local tokens = text.tokenize(partial)
    local lastToken = tokens[#tokens] or ""
    
    -- Check if completing a command or argument
    if #tokens <= 1 and not partial:match("%s$") then
        -- Completing command name
        local completions = shell.complete(lastToken)
        if #completions == 1 then
            return completions[1]:sub(#lastToken + 1)
        elseif #completions > 1 then
            -- Show options
            term.write("\n")
            for i, c in ipairs(completions) do
                if i > 20 then
                    term.write("... and " .. (#completions - 20) .. " more\n")
                    break
                end
                term.write(c .. "  ")
            end
            term.write("\n")
            -- Redraw prompt
            term.write(getPrompt())
        end
    else
        -- Completing path/argument
        local completions = shell.complete(lastToken)
        if #completions == 1 then
            return completions[1]:sub(#lastToken + 1)
        elseif #completions > 1 then
            term.write("\n")
            for i, c in ipairs(completions) do
                if i > 20 then
                    term.write("... and " .. (#completions - 20) .. " more\n")
                    break
                end
                term.write(c .. "  ")
            end
            term.write("\n")
            term.write(getPrompt())
        end
    end
    
    return nil
end

-- Handle a line of input
local function handleLine(line)
    line = text.trim(line)
    if line == "" then return end
    
    -- Add to history (avoid duplicates)
    if history[#history] ~= line then
        table.insert(history, line)
        if #history > 100 then
            table.remove(history, 1)
        end
    end
    
    -- Handle semicolons (multiple commands)
    local commands = text.split(line, ";", true)
    for _, cmd in ipairs(commands) do
        cmd = text.trim(cmd)
        if cmd ~= "" then
            local ok, err = shell.run(cmd)
            if not ok and err then
                -- Error already printed by shell.execute
            end
        end
    end
end

-- Main REPL loop
function sh.run()
    -- Setup default environment
    process.setenv("USER", "root")
    process.setenv("HOME", "/home/root")
    process.setenv("HOSTNAME", computer.address():sub(1, 8))
    process.setenv("TERM", "opencomputers")
    process.setenv("PATH", "/bin:/usr/bin:/home/root/bin")
    process.setenv("PWD", "/home/root")
    
    -- Ensure home directory exists
    if not fs.exists("/home/root") then
        fs.makeDirectory("/home/root")
    end
    
    -- Load shell profile if it exists
    if fs.exists("/etc/profile.lua") then
        pcall(shell.runFile, "/etc/profile.lua")
    end
    if fs.exists("/home/root/.profile.lua") then
        pcall(shell.runFile, "/home/root/.profile.lua")
    end
    
    -- Load history
    if fs.exists("/home/root/.sh_history") then
        local data = fs.readAll("/home/root/.sh_history")
        if data then
            for line in data:gmatch("[^\n]+") do
                table.insert(history, line)
            end
        end
    end
    
    -- Display welcome message
    term.setForeground(0x00AAFF)
    term.write("OpenOS " .. (_G.OPENOS_VERSION or "1.0") .. " - OpenComputers Shell\n")
    term.setForeground(0xFFFFFF)
    term.write("Type 'help' to get started.\n\n")
    
    while running do
        -- Write prompt
        local promptStr = getPrompt()
        -- Strip ANSI if no color support
        if not term.isAvailable() then
            promptStr = promptStr:gsub("\x1b%[[^m]*m", "")
        end
        term.write(promptStr)
        
        -- Read input
        local ok, line = pcall(term.read, history, true, hint)
        if not ok then
            if line == "interrupted" then
                term.write("^C\n")
            else
                term.write("\nError reading input: " .. tostring(line) .. "\n")
            end
        elseif line then
            handleLine(line)
        else
            -- EOF
            term.write("\nexit\n")
            running = false
        end
    end
    
    -- Save history
    local histData = table.concat(history, "\n") .. "\n"
    fs.writeAll("/home/root/.sh_history", histData)
end

-- Entrypoint
sh.run()
