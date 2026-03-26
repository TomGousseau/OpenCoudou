-- Hologram Library
-- Provides utilities for hologram rendering

local holo = {}

local component = component
local math = math

-- ========================================
-- Hologram Access
-- ========================================

local function getHologram()
  local addr = component.list("hologram")()
  if addr then
    return component.proxy(addr)
  end
  return nil
end

-- ========================================
-- Basic Functions
-- ========================================

function holo.clear()
  local h = getHologram()
  if h then
    h.clear()
    return true
  end
  return false
end

function holo.set(x, y, z, value)
  local h = getHologram()
  if h then
    return h.set(x, y, z, value or 1)
  end
  return false
end

function holo.get(x, y, z)
  local h = getHologram()
  if h then
    return h.get(x, y, z)
  end
  return 0
end

function holo.fill(x, z, minY, maxY, value)
  local h = getHologram()
  if h then
    return h.fill(x, z, minY, maxY, value or 1)
  end
  return false
end

function holo.setScale(scale)
  local h = getHologram()
  if h then
    return h.setScale(scale)
  end
  return false
end

function holo.setTranslation(x, y, z)
  local h = getHologram()
  if h then
    return h.setTranslation(x, y, z)
  end
  return false
end

function holo.setRotation(angle, x, y, z)
  local h = getHologram()
  if h then
    return h.setRotation(angle, x, y, z)
  end
  return false
end

function holo.setRotationSpeed(speed, x, y, z)
  local h = getHologram()
  if h then
    return h.setRotationSpeed(speed, x, y, z)
  end
  return false
end

function holo.setPaletteColor(index, value)
  local h = getHologram()
  if h then
    return h.setPaletteColor(index, value)
  end
  return false
end

function holo.getPaletteColor(index)
  local h = getHologram()
  if h then
    return h.getPaletteColor(index)
  end
  return 0
end

function holo.maxDepth()
  local h = getHologram()
  if h then
    return h.maxDepth()
  end
  return 0
end

-- ========================================
-- Drawing Functions
-- ========================================

-- Draw a line in 3D space
function holo.line(x1, y1, z1, x2, y2, z2, value)
  value = value or 1
  
  local dx = math.abs(x2 - x1)
  local dy = math.abs(y2 - y1)
  local dz = math.abs(z2 - z1)
  
  local xs = x1 < x2 and 1 or -1
  local ys = y1 < y2 and 1 or -1
  local zs = z1 < z2 and 1 or -1
  
  local dm = math.max(dx, dy, dz)
  
  for i = 0, dm do
    local t = dm == 0 and 0 or i / dm
    local x = math.floor(x1 + t * (x2 - x1) + 0.5)
    local y = math.floor(y1 + t * (y2 - y1) + 0.5)
    local z = math.floor(z1 + t * (z2 - z1) + 0.5)
    holo.set(x, y, z, value)
  end
end

-- Draw a box outline
function holo.box(x1, y1, z1, x2, y2, z2, value)
  -- Bottom
  holo.line(x1, y1, z1, x2, y1, z1, value)
  holo.line(x2, y1, z1, x2, y1, z2, value)
  holo.line(x2, y1, z2, x1, y1, z2, value)
  holo.line(x1, y1, z2, x1, y1, z1, value)
  
  -- Top
  holo.line(x1, y2, z1, x2, y2, z1, value)
  holo.line(x2, y2, z1, x2, y2, z2, value)
  holo.line(x2, y2, z2, x1, y2, z2, value)
  holo.line(x1, y2, z2, x1, y2, z1, value)
  
  -- Verticals
  holo.line(x1, y1, z1, x1, y2, z1, value)
  holo.line(x2, y1, z1, x2, y2, z1, value)
  holo.line(x2, y1, z2, x2, y2, z2, value)
  holo.line(x1, y1, z2, x1, y2, z2, value)
end

-- Fill a solid box
function holo.fillBox(x1, y1, z1, x2, y2, z2, value)
  value = value or 1
  
  for x = math.min(x1, x2), math.max(x1, x2) do
    for z = math.min(z1, z2), math.max(z1, z2) do
      holo.fill(x, z, math.min(y1, y2), math.max(y1, y2), value)
    end
  end
end

-- Draw a sphere
function holo.sphere(cx, cy, cz, radius, value)
  value = value or 1
  
  for x = -radius, radius do
    for y = -radius, radius do
      for z = -radius, radius do
        if x*x + y*y + z*z <= radius*radius then
          holo.set(cx + x, cy + y, cz + z, value)
        end
      end
    end
  end
end

-- Draw a circle (horizontal)
function holo.circle(cx, cy, cz, radius, value)
  value = value or 1
  
  for angle = 0, 360, 5 do
    local rad = math.rad(angle)
    local x = math.floor(cx + radius * math.cos(rad) + 0.5)
    local z = math.floor(cz + radius * math.sin(rad) + 0.5)
    holo.set(x, cy, z, value)
  end
end

-- Draw text (simple 3x5 font)
function holo.text(x, y, z, str, value)
  -- Simplified: just mark positions
  value = value or 1
  local offset = 0
  
  for i = 1, #str do
    -- Each character is 4 wide
    -- This is a placeholder - real implementation would have font data
    for dy = 0, 4 do
      holo.set(x + offset, y + dy, z, value)
    end
    offset = offset + 4
  end
end

return holo
