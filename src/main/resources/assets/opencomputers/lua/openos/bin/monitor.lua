-- monitor.lua - Resource monitor program with graphical display
-- Shows CPU/memory/energy/disk usage as a graphical dashboard on a screen
-- Usage: monitor [refresh_interval_seconds]

local gpu    = require("gpu")
local event  = require("event")
local colors = require("colors")

local args    = { ... }
local REFRESH = tonumber(args[1]) or 2

-- -----------------------------------------------------------------------
-- Data collection helpers
-- -----------------------------------------------------------------------

local function getMemory()
    local free  = computer.freeMemory()
    local total = computer.totalMemory()
    return { free = free, total = total, used = total - free }
end

local function getEnergy()
    local cur = computer.energy()
    local max = computer.maxEnergy()
    return { current = cur, max = max }
end

local function getProcesses()
    local ok, process = pcall(require, "process")
    if not ok then return {} end
    -- Return list of {pid, name, status}
    local list = {}
    if type(process.list) == "function" then
        for pid, info in pairs(process.list()) do
            table.insert(list, { pid = pid, name = info.name or "?", status = info.status or "?" })
        end
    end
    table.sort(list, function(a, b) return (a.pid or 0) < (b.pid or 0) end)
    return list
end

local function getFilesystems()
    local drives = {}
    for addr, _ in component.list("filesystem") do
        local ok1, total = pcall(component.invoke, addr, "spaceTotal")
        local ok2, free  = pcall(component.invoke, addr, "spaceAvailable")
        if ok1 and ok2 then
            table.insert(drives, {
                addr  = addr:sub(1, 8),
                total = ok1 and total or 0,
                free  = ok2 and free  or 0,
            })
        end
    end
    return drives
end

-- -----------------------------------------------------------------------
-- Layout rendering
-- -----------------------------------------------------------------------

local function formatBytes(b)
    if b >= 1024*1024 then return string.format("%.1fMB", b/(1024*1024))
    elseif b >= 1024  then return string.format("%.1fKB", b/1024)
    else                   return b.."B"
    end
end

local function drawDashboard(w, h)
    local g = component.proxy(component.list("gpu")())
    g.setBackground(0x0A0A1E)
    g.setForeground(0xFFFFFF)

    -- Title bar
    g.setBackground(0x1A1A4E)
    g.setForeground(0x55FFFF)
    g.fill(1, 1, w, 1, " ")
    local title = " OpenComputers Resource Monitor"
    local uptime_str = string.format("Uptime: %s ", require("computer").formatUptime())
    g.set(1, 1, title)
    g.set(w - #uptime_str + 1, 1, uptime_str)

    -- Clear content area
    g.setBackground(0x0A0A1E)
    g.fill(1, 2, w, h - 1, " ")

    local row = 3

    -- Memory section
    local mem = getMemory()
    g.setForeground(0xFFFF55)
    g.set(2, row, "MEMORY")
    row = row + 1

    g.setForeground(0xAAAAAA)
    local memPct = mem.total > 0 and (mem.used / mem.total) or 0
    local barW = math.floor(w / 2) - 4
    gpu.progressBar(2, row, barW, memPct,
        memPct > 0.85 and 0xFF4444 or 0x44FF44, 0x333333,
        nil, 0x111111)
    local memStr = string.format(" %s / %s (%.0f%%)",
        formatBytes(mem.used), formatBytes(mem.total), memPct * 100)
    g.set(2 + barW, row, memStr)
    row = row + 2

    -- Energy section
    local energy = getEnergy()
    if energy.max > 0 then
        g.setForeground(0xFFFF55)
        g.set(2, row, "ENERGY")
        row = row + 1

        local engPct = energy.current / energy.max
        gpu.progressBar(2, row, barW, engPct,
            engPct < 0.2 and 0xFF4444 or 0xFFAA00, 0x333333,
            nil, 0x111111)
        local engStr = string.format(" %.0f / %.0f (%.0f%%)",
            energy.current, energy.max, engPct * 100)
        g.set(2 + barW, row, engStr)
        row = row + 2
    end

    -- Disk section
    local drives = getFilesystems()
    if #drives > 0 then
        g.setForeground(0xFFFF55)
        g.set(2, row, "FILESYSTEMS")
        row = row + 1

        for _, d in ipairs(drives) do
            if row >= h - 3 then break end
            local used = d.total - d.free
            local pct  = d.total > 0 and (used / d.total) or 0
            local dbarW = math.floor(w / 3) - 4
            g.setForeground(0xCCCCCC)
            g.set(2, row, d.addr .. " ")
            gpu.progressBar(12, row, dbarW, pct,
                pct > 0.9 and 0xFF4444 or 0x4488FF, 0x333333,
                nil, 0x111111)
            local dstr = string.format(" %s/%s", formatBytes(used), formatBytes(d.total))
            g.set(12 + dbarW, row, dstr)
            row = row + 1
        end
        row = row + 1
    end

    -- Process list
    if row < h - 2 then
        g.setForeground(0xFFFF55)
        g.set(2, row, "PROCESSES")
        row = row + 1

        local processes = getProcesses()
        g.setForeground(0x888888)
        g.set(2, row, string.format("%-5s %-20s %s", "PID", "NAME", "STATUS"))
        row = row + 1

        for _, proc in ipairs(processes) do
            if row >= h - 1 then break end
            local statusColor = proc.status == "running" and 0x44FF44 or 0xAAAAAA
            g.setForeground(0xCCCCCC)
            g.set(2, row, string.format("%-5s %-20s", tostring(proc.pid), proc.name:sub(1,20)))
            g.setForeground(statusColor)
            g.set(28, row, proc.status or "?")
            row = row + 1
        end
    end

    -- Status bar
    g.setBackground(0x1A1A4E)
    g.setForeground(0x888888)
    g.fill(1, h, w, 1, " ")
    g.set(2, h, string.format("Refreshing every %ds | Press Q or Ctrl+C to quit", REFRESH))
end

-- -----------------------------------------------------------------------
-- Main loop
-- -----------------------------------------------------------------------

-- Find GPU and screen
local gpuAddr = component.list("gpu")()
if not gpuAddr then
    io.stderr:write("monitor: no GPU found\n")
    return 1
end

local w, h = component.invoke(gpuAddr, "getResolution")
drawDashboard(w, h)

local running = true
event.listen("interrupted", function() running = false end)

local refreshTimer = event.timer(REFRESH, function()
    if running then
        drawDashboard(w, h)
    end
end)

while running do
    local ev, _, char, key = event.pull(REFRESH + 0.5)
    if ev == "key_down" then
        if key == 16 or char == string.byte("q") or char == string.byte("Q") then
            running = false
        end
    end
end

event.cancel(refreshTimer)

-- Restore terminal
local term = require("term")
term.clear()
term.setCursor(1, 1)
term.setForeground(0xFFFFFF)
term.setBackground(0x000000)
print("monitor exited")
