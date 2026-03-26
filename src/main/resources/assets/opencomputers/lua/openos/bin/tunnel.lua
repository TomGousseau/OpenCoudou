-- tunnel.lua - Robot tunneling program
-- Usage: tunnel <length> [height] [width]
--   length: number of blocks to mine forward
--   height: tunnel height (default 2)
--   width:  tunnel width (default 1)

local robot = require("robot")
local shell = require("shell")
local term  = require("term")

local args = { ... }
local length = tonumber(args[1]) or 10
local height = tonumber(args[2]) or 2
local width  = tonumber(args[3]) or 1

if length < 1 then
    io.stderr:write("tunnel: length must be >= 1\n")
    return 1
end
if height < 1 or height > 4 then
    io.stderr:write("tunnel: height must be 1-4\n")
    return 1
end
if width < 1 or width > 5 then
    io.stderr:write("tunnel: width must be 1-5\n")
    return 1
end

print(string.format("Tunneling %dx%dx%d...", length, width, height))
print("Press Ctrl+C to stop early.")

local stepsDone = 0
local blocksMined = 0

local function mineColumn()
    -- Mine up to height-1 blocks above head (robot occupies 1 block)
    for lvl = 1, height - 1 do
        local det, _ = robot.detectUp and robot.detectUp()
        if det then
            robot.swingUp()
            blocksMined = blocksMined + 1
        end
        if lvl < height - 1 then
            robot.up()
        end
    end
    -- Come back down
    for lvl = 1, height - 2 do
        robot.down()
    end
end

local function mineRow()
    -- For the current column width, mine and move
    mineColumn()
    for w = 1, width - 1 do
        robot.turnRight()
        robot.digForward()
        blocksMined = blocksMined + 1
        robot.move and robot.forward()
        mineColumn()
    end
    -- Return to start of row
    if width > 1 then
        robot.turnAround()
        for w = 1, width - 1 do
            robot.forward()
        end
        robot.turnAround()
    end
end

-- Main tunneling loop
local ok = true
for i = 1, length do
    -- Check energy
    if robot.energyLevel() < 0.1 then
        print("Low energy! Stopping at step " .. i)
        ok = false
        break
    end
    
    -- Mine forward
    local det, _ = robot.detect()
    if det then
        robot.swing()
        blocksMined = blocksMined + 1
    end
    
    local moved, err = robot.forward()
    if not moved then
        io.stderr:write("Blocked at step " .. i .. ": " .. tostring(err) .. "\n")
        ok = false
        break
    end
    
    stepsDone = i
    
    -- Mine height and width
    mineRow()
    
    -- Print progress every 10 steps
    if i % 10 == 0 then
        local w2, _h = term.getSize()
        local pct = math.floor(i / length * 100)
        io.write(string.format("\r  Progress: %d/%d (%d%%)  blocks: %d  energy: %.0f%%   ",
            i, length, pct, blocksMined, robot.energyLevel() * 100))
    end
end

print(string.format("\nDone! Traveled %d/%d blocks, mined %d blocks.", stepsDone, length, blocksMined))
return ok and 0 or 1
