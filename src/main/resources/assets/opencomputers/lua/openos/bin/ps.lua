-- ps.lua - List processes
local process = require("process")
local term = require("term")
local text = require("text")

local args = { ... }
local wide = false

for _, a in ipairs(args) do
    if a == "-w" or a == "--wide" then wide = true end
end

local w = select(1, term.getSize())

term.setForeground(0x88AAFF)
print(text.padRight("PID", 6) .. text.padRight("STATUS", 12) .. "NAME")
term.setForeground(0xFFFFFF)
print(string.rep("-", w < 40 and 40 or w - 1))

local procs = process.list()
table.sort(procs, function(a, b) return a.pid < b.pid end)

for _, p in ipairs(procs) do
    local status = p.dead and "dead" or "running"
    term.setForeground(p.dead and 0x888888 or 0xFFFFFF)
    print(text.padRight(tostring(p.pid), 6) .. text.padRight(status, 12) .. (p.name or "?"))
end

term.setForeground(0xFFFFFF)
print(string.rep("-", w < 40 and 40 or w - 1))
print(#procs .. " process(es)")
