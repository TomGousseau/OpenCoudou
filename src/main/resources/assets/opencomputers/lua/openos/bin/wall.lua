-- wrapBlock.lua - Robot utility to wrap around a building and build a wall
-- Usage: wrapBlock <width> <height>

local robot = require("robot")

local args = { ... }
local width  = tonumber(args[1]) or 5
local height = tonumber(args[2]) or 3

print(string.format("Building %dx%d wall...", width, height))

-- Build a single layer
local function buildLayer()
    for i = 1, width do
        robot.placeDown()
        if i < width then robot.forward() end
    end
end

-- Start from the ground, build layer by layer
for lvl = 1, height do
    buildLayer()
    if lvl < height then
        -- Rise up one level and come back
        robot.up()
        robot.turnAround()
    end
end

print("Wall complete!")
return 0
