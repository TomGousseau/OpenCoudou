-- df.lua - Display filesystem disk usage
local fs = require("filesystem")
local term = require("term")
local text = require("text")
local component = component

local function formatSize(bytes)
    if bytes < 1024 then return bytes .. "B" end
    if bytes < 1024 * 1024 then return string.format("%.1fK", bytes / 1024) end
    return string.format("%.1fM", bytes / (1024 * 1024))
end

local w = select(1, term.getSize())

term.setForeground(0x88AAFF)
print(text.padRight("Filesystem", 24) ..
      text.padLeft("Size", 8) ..
      text.padLeft("Used", 8) ..
      text.padLeft("Free", 8) ..
      text.padLeft("Use%", 6) ..
      "  Mounted on")
term.setForeground(0xFFFFFF)
print(string.rep("-", w < 60 and 60 or w - 1))

-- Go through all filesystem components
local mounts = fs.getMounts()
-- Add root if not explicitly mounted
if not mounts["/"] then
    local addr = component.list("filesystem")()
    if addr then mounts["/"] = addr end
end

for mountPoint, addr in pairs(mounts) do
    local ok, total = pcall(component.invoke, addr, "spaceTotal")
    local ok2, free = pcall(component.invoke, addr, "spaceAvailable")
    
    total = ok and total or 0
    free = ok2 and free or 0
    local used = total - free
    local pct = total > 0 and math.floor(used / total * 100) or 0
    
    local label = ""
    local ok3, lbl = pcall(component.invoke, addr, "getLabel")
    if ok3 and lbl then label = lbl end
    
    local name = label ~= "" and label or (addr:sub(1, 8) .. "...")
    
    local pctStr = pct .. "%"
    print(text.padRight(name, 24) ..
          text.padLeft(formatSize(total), 8) ..
          text.padLeft(formatSize(used), 8) ..
          text.padLeft(formatSize(free), 8) ..
          text.padLeft(pctStr, 6) ..
          "  " .. mountPoint)
end
