-- free command: show memory usage

local total = computer.totalMemory()
local free = computer.freeMemory()
local used = total - free

print("Memory Usage:")
print(string.format("  Total:  %d bytes (%.1f KB)", total, total / 1024))
print(string.format("  Used:   %d bytes (%.1f KB)", used, used / 1024))
print(string.format("  Free:   %d bytes (%.1f KB)", free, free / 1024))
print(string.format("  Usage:  %.1f%%", (used / total) * 100))
