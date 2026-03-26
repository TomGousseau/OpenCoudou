-- /etc/profile.lua - Default shell profile
-- This file is sourced by sh.lua when starting an interactive shell.
-- Edit to customize your environment.

local shell = require("shell")
local term  = require("term")

-- -----------------------------------------------------------------------
-- PATH - where to find executable scripts
-- -----------------------------------------------------------------------
shell.setPath("/bin:/usr/bin:/home/root/bin:/usr/local/bin")

-- -----------------------------------------------------------------------
-- Environment variables
-- -----------------------------------------------------------------------
local env = require("process").getenv()

env.HOME     = env.HOME     or "/home/root"
env.TERM     = env.TERM     or "oc-term"
env.LANG     = env.LANG     or "en_US.UTF-8"
env.EDITOR   = env.EDITOR   or "edit"
env.PAGER    = env.PAGER    or "more"
env.TMPDIR   = env.TMPDIR   or "/tmp"
env.HISTFILE = env.HISTFILE or "/home/root/.sh_history"
env.HISTSIZE = env.HISTSIZE or "200"

-- Set hostname
local fs = require("filesystem")
if fs.exists("/etc/hostname") then
    local h = fs.readAll("/etc/hostname")
    if h then env.HOSTNAME = h:gsub("%s+", "") end
else
    env.HOSTNAME = "oc-" .. computer.address():sub(1, 8)
end

-- PS1: coloured prompt  user@host:cwd$
env.PS1 = "\x1b[32m\\u@\\h\x1b[0m:\x1b[36m\\w\x1b[0m\\$ "

-- -----------------------------------------------------------------------
-- Aliases
-- -----------------------------------------------------------------------
shell.alias("ll",    "ls -l")
shell.alias("la",    "ls -la")
shell.alias("l",     "ls -lA")
shell.alias("..",    "cd ..")
shell.alias("...",   "cd ../..")
shell.alias("cls",   "term.clear and term.clear() or shell.run('echo')")
shell.alias("clear", "lua -e 'term=require(\"term\") term.clear() term.setCursor(1,1)'")
shell.alias("q",     "exit")
shell.alias("bye",   "exit")
shell.alias("reboot","reboot")
shell.alias("halt",  "reboot -p")
shell.alias("h",     "help")
shell.alias("md",    "mkdir -p")
shell.alias("rd",    "rm -r")

-- -----------------------------------------------------------------------
-- Ensure important directories exist
-- -----------------------------------------------------------------------
for _, dir in ipairs({
    "/tmp",
    "/var/log",
    "/home/root",
    "/home/root/bin",
    "/usr/bin",
    "/usr/lib",
    "/usr/local/bin",
}) do
    if not fs.exists(dir) then
        fs.makeDirectory(dir)
    end
end

-- -----------------------------------------------------------------------
-- Welcome / MOTD
-- -----------------------------------------------------------------------
if fs.exists("/etc/motd") then
    local motd = fs.readAll("/etc/motd")
    if motd then
        io.write(motd)
    end
end
