-- top.lua - Real-time process/system monitor
local process = require("process")
local term = require("term")
local text = require("text")

local function header()
    local uptime = computer.uptime()
    local energy = computer.energy()
    local maxEnergy = computer.maxEnergy and computer.maxEnergy() or 10000
    local w, h = term.getSize()
    
    term.setBackground(0x003366)
    term.setForeground(0xFFFFFF)
    
    local pct = math.floor((energy / maxEnergy) * 100)
    local line1 = string.format(" OpenOS top  Uptime: %s  Energy: %d/%d (%d%%)",
        formatUptime(uptime), math.floor(energy), math.floor(maxEnergy), pct)
    io.write(text.padRight(line1, w) .. "\n")
    
    term.setBackground(0x000000)
end

local function formatUptime(s)
    local h = math.floor(s / 3600)
    local m = math.floor((s % 3600) / 60)
    local sec = math.floor(s % 60)
    return string.format("%02d:%02d:%02d", h, m, sec)
end

local w, h = term.getSize()
local running = true

term.clear()

while running do
    local sig = { computer.pullSignal(1.0) }
    if sig[1] == "key_down" and (sig[4] == 16 or sig[4] == 1) then -- q or Esc
        running = false
        break
    end
    
    -- Redraw
    term.setCursor(1, 1)
    
    local uptime = computer.uptime()
    local energy = computer.energy()
    local maxEnergy = 10000
    local pct = math.floor((energy / maxEnergy) * 100)
    
    -- Header
    term.setBackground(0x003366)
    term.setForeground(0xFFFFFF)
    local line = string.format(" OpenOS  Uptime: %s  Energy: %d (%d%%)",
        formatUptime(uptime), math.floor(energy), pct)
    io.write(text.padRight(line, w) .. "\n")
    
    -- Column headers
    term.setBackground(0x001122)
    term.setForeground(0xAABBFF)
    io.write(text.padRight(
        text.padRight("PID", 5) .. text.padRight("STATUS", 10) .. "NAME", w) .. "\n")
    
    term.setBackground(0x000000)
    term.setForeground(0xFFFFFF)
    
    local procs = process.list()
    local maxRows = h - 4
    
    for i, p in ipairs(procs) do
        if i > maxRows then break end
        local status = p.dead and "dead" or "running"
        term.setForeground(p.dead and 0x666666 or 0xFFFFFF)
        io.write(text.padRight(
            text.padRight(tostring(p.pid), 5) ..
            text.padRight(status, 10) ..
            (p.name or "?"), w) .. "\n")
    end
    
    -- Footer
    term.setCursor(1, h)
    term.setBackground(0x222222)
    term.setForeground(0xAAAAAA)
    io.write(text.padRight(" q=Quit  Updates every 1s", w))
    term.setBackground(0x000000)
    term.setForeground(0xFFFFFF)
end

term.clear()
