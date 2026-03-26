-- uptime command: show computer uptime

local time = computer.uptime()

local hours = math.floor(time / 3600)
local minutes = math.floor((time % 3600) / 60)
local seconds = math.floor(time % 60)

if hours > 0 then
  print(string.format("Uptime: %d hours, %d minutes, %d seconds", hours, minutes, seconds))
elseif minutes > 0 then
  print(string.format("Uptime: %d minutes, %d seconds", minutes, seconds))
else
  print(string.format("Uptime: %d seconds", seconds))
end
