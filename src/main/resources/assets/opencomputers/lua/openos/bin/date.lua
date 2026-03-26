-- bin/date.lua - Show current date/time (based on computer uptime)
-- OpenComputers has no real-time clock, so we use os.clock() / computer.uptime()

local shell  = require("shell")
local args   = shell.parse(...)

-- Get uptime in seconds
local ok, computer = pcall(require, "computer")
local uptime = 0
if ok and type(computer) == "table" and computer.uptime then
  uptime = computer.uptime()
else
  uptime = os.clock and os.clock() or 0
end

-- Format seconds as H:M:S
local function formatUptime(secs)
  secs = math.floor(secs)
  local h = math.floor(secs / 3600)
  local m = math.floor((secs % 3600) / 60)
  local s = secs % 60
  return string.format("%02d:%02d:%02d", h, m, s)
end

-- Parse days from total seconds
local totalSecs = math.floor(uptime)
local days  = math.floor(totalSecs / 86400)
local hours = math.floor((totalSecs % 86400) / 3600)
local mins  = math.floor((totalSecs % 3600) / 60)
local secs  = totalSecs % 60

-- Simple format specifiers
local function applyFormat(fmt)
  fmt = fmt:gsub("%%H", string.format("%02d", hours))
  fmt = fmt:gsub("%%M", string.format("%02d", mins))
  fmt = fmt:gsub("%%S", string.format("%02d", secs))
  fmt = fmt:gsub("%%d", string.format("%d", days % 30 + 1))
  fmt = fmt:gsub("%%m", string.format("%02d", (days // 30) % 12 + 1))
  fmt = fmt:gsub("%%Y", tostring(2024 + math.floor(days / 365)))
  fmt = fmt:gsub("%%j", string.format("%03d", days % 365 + 1))
  fmt = fmt:gsub("%%n", "\n")
  fmt = fmt:gsub("%%t", "\t")
  return fmt
end

-- Check for format argument
local fmt = nil
if args[1] and args[1]:sub(1,1) == "+" then
  fmt = args[1]:sub(2)
end

if fmt then
  io.write(applyFormat(fmt) .. "\n")
else
  -- Default output
  io.write(string.format(
    "Uptime: %d days, %02d:%02d:%02d\n",
    days, hours, mins, secs
  ))
  io.write(string.format(
    "Simulated date: Day %d, %02d:%02d:%02d\n",
    days + 1, hours, mins, secs
  ))
end
