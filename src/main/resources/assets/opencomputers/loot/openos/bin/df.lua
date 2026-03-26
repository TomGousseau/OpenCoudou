-- df command: show filesystem usage

print("Filesystem Usage:")
print(string.format("%-12s %10s %10s %10s %5s", "Label", "Size", "Used", "Free", "Use%"))
print(string.rep("-", 50))

for address in component.list("filesystem") do
  local label = component.invoke(address, "getLabel") or address:sub(1, 8)
  local total = component.invoke(address, "spaceTotal")
  local used = component.invoke(address, "spaceUsed")
  local free = total - used
  local percent = math.floor((used / total) * 100)
  
  local function formatSize(bytes)
    if bytes >= 1024 * 1024 then
      return string.format("%.1fM", bytes / (1024 * 1024))
    elseif bytes >= 1024 then
      return string.format("%.1fK", bytes / 1024)
    else
      return string.format("%dB", bytes)
    end
  end
  
  print(string.format("%-12s %10s %10s %10s %4d%%",
    label:sub(1, 12),
    formatSize(total),
    formatSize(used),
    formatSize(free),
    percent
  ))
end
