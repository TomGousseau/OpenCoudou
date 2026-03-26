-- sides.lua - Directional constants library
-- Maps human-readable names to side indices used by component APIs

local sides = {}

-- Numeric side constants
sides.bottom  = 0
sides.top     = 1
sides.back    = 2
sides.front   = 3
sides.right   = 4
sides.left    = 5

-- Aliases
sides.down    = sides.bottom
sides.up      = sides.top
sides.north   = sides.back   -- Minecraft default orientation
sides.south   = sides.front
sides.west    = sides.left
sides.east    = sides.right

-- All standard side names in index order
sides.names = {
    [0] = "bottom",
    [1] = "top",
    [2] = "back",
    [3] = "front",
    [4] = "right",
    [5] = "left",
}

-- Number of sides
sides.count = 6

-- Opposite side
sides.opposite = {
    [0] = 1, -- bottom <-> top
    [1] = 0,
    [2] = 3, -- back <-> front
    [3] = 2,
    [4] = 5, -- right <-> left
    [5] = 4,
}

-- Horizontal directions only
sides.horizontal = { sides.front, sides.back, sides.left, sides.right }

-- Vertical directions only
sides.vertical = { sides.top, sides.bottom }

-- All sides as array
sides.all = { 0, 1, 2, 3, 4, 5 }

-- Get the name of a side by index
function sides.name(index)
    return sides.names[index] or "unknown"
end

-- Get a side index by name (case-insensitive)
function sides.byName(name)
    if not name then return nil end
    name = name:lower()
    -- Check direct numeric
    local n = tonumber(name)
    if n and n >= 0 and n <= 5 then return n end
    -- Check aliases
    local aliases = {
        bottom = 0, down = 0,
        top    = 1, up   = 1,
        back   = 2, north= 2,
        front  = 3, south= 3,
        right  = 4, east = 4,
        left   = 5, west = 5,
    }
    return aliases[name]
end

-- Validate a side index
function sides.valid(index)
    return type(index) == "number" and index >= 0 and index <= 5
end

-- Rotate a side clockwise around Y axis (standing on ground)
local rotateY = {
    [0] = 0, -- bottom stays bottom
    [1] = 1, -- top stays top
    [2] = 5, -- back -> left
    [3] = 4, -- front -> right
    [4] = 2, -- right -> back
    [5] = 3, -- left -> front
}
function sides.rotateCW(side)
    return rotateY[side] or side
end

-- Rotate a side counter-clockwise around Y axis
local rotateYCCW = {
    [0] = 0,
    [1] = 1,
    [2] = 4, -- back -> right
    [3] = 5, -- front -> left
    [4] = 3, -- right -> front
    [5] = 2, -- left -> back
}
function sides.rotateCCW(side)
    return rotateYCCW[side] or side
end

-- Rotate around Z axis (tipping forward)
local rotateZ = {
    [0] = 3, -- bottom -> front
    [1] = 2, -- top -> back
    [2] = 0, -- back -> bottom
    [3] = 1, -- front -> top
    [4] = 4, -- right stays right
    [5] = 5, -- left stays left
}
function sides.rotateZ(side)
    return rotateZ[side] or side
end

-- Rotate around X axis (rolling to side)
local rotateX = {
    [0] = 4, -- bottom -> right
    [1] = 5, -- top -> left
    [2] = 2, -- back stays back
    [3] = 3, -- front stays front
    [4] = 1, -- right -> top
    [5] = 0, -- left -> bottom
}
function sides.rotateX(side)
    return rotateX[side] or side
end

-- Convert a side to a 3D unit vector { x, y, z }
local vectors = {
    [0] = { x =  0, y = -1, z =  0 }, -- bottom
    [1] = { x =  0, y =  1, z =  0 }, -- top
    [2] = { x =  0, y =  0, z = -1 }, -- back (north)
    [3] = { x =  0, y =  0, z =  1 }, -- front (south)
    [4] = { x =  1, y =  0, z =  0 }, -- right (east)
    [5] = { x = -1, y =  0, z =  0 }, -- left (west)
}
function sides.toVector(side)
    return vectors[side]
end

-- Convert a unit vector to a side index
function sides.fromVector(x, y, z)
    if y < 0 then return sides.bottom end
    if y > 0 then return sides.top end
    if z < 0 then return sides.back end
    if z > 0 then return sides.front end
    if x > 0 then return sides.right end
    if x < 0 then return sides.left end
    return nil
end

-- Check if two sides are parallel (both horizontal or both vertical)
function sides.parallel(a, b)
    local aH = a == sides.right or a == sides.left or a == sides.front or a == sides.back
    local bH = b == sides.right or b == sides.left or b == sides.front or b == sides.back
    return aH == bH
end

-- Check if two sides are perpendicular
function sides.perpendicular(a, b)
    return not sides.parallel(a, b)
end

-- Check if a side is horizontal
function sides.isHorizontal(s)
    return s >= 2
end

-- Check if a side is vertical
function sides.isVertical(s)
    return s <= 1
end

return sides
