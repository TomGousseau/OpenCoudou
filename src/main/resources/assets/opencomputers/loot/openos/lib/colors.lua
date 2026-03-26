-- Colors Library
-- Provides color constants and utilities

local colors = {}

-- Basic colors
colors.white = 0xFFFFFF
colors.orange = 0xFFA500
colors.magenta = 0xFF00FF
colors.lightblue = 0xADD8E6
colors.yellow = 0xFFFF00
colors.lime = 0x00FF00
colors.pink = 0xFFC0CB
colors.gray = 0x808080
colors.grey = 0x808080
colors.silver = 0xC0C0C0
colors.lightgray = 0xC0C0C0
colors.lightgrey = 0xC0C0C0
colors.cyan = 0x00FFFF
colors.purple = 0x800080
colors.blue = 0x0000FF
colors.brown = 0x8B4513
colors.green = 0x008000
colors.red = 0xFF0000
colors.black = 0x000000

-- Minecraft dye colors (by index)
colors[0] = colors.white
colors[1] = colors.orange
colors[2] = colors.magenta
colors[3] = colors.lightblue
colors[4] = colors.yellow
colors[5] = colors.lime
colors[6] = colors.pink
colors[7] = colors.gray
colors[8] = colors.silver
colors[9] = colors.cyan
colors[10] = colors.purple
colors[11] = colors.blue
colors[12] = colors.brown
colors[13] = colors.green
colors[14] = colors.red
colors[15] = colors.black

-- ========================================
-- Color Functions
-- ========================================

function colors.rgb(r, g, b)
  r = math.max(0, math.min(255, math.floor(r)))
  g = math.max(0, math.min(255, math.floor(g)))
  b = math.max(0, math.min(255, math.floor(b)))
  return r * 65536 + g * 256 + b
end

function colors.unpack(color)
  local r = math.floor(color / 65536) % 256
  local g = math.floor(color / 256) % 256
  local b = color % 256
  return r, g, b
end

function colors.brightness(color, factor)
  local r, g, b = colors.unpack(color)
  r = math.min(255, r * factor)
  g = math.min(255, g * factor)
  b = math.min(255, b * factor)
  return colors.rgb(r, g, b)
end

function colors.blend(color1, color2, ratio)
  ratio = ratio or 0.5
  local r1, g1, b1 = colors.unpack(color1)
  local r2, g2, b2 = colors.unpack(color2)
  local r = r1 * (1 - ratio) + r2 * ratio
  local g = g1 * (1 - ratio) + g2 * ratio
  local b = b1 * (1 - ratio) + b2 * ratio
  return colors.rgb(r, g, b)
end

function colors.invert(color)
  local r, g, b = colors.unpack(color)
  return colors.rgb(255 - r, 255 - g, 255 - b)
end

return colors
