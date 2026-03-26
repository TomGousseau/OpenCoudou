-- bin/harvest.lua - Robot crop harvester
-- Harvests a rectangular field of crops and replants seeds

local robot = require("robot")
local shell = require("shell")
local args  = shell.parse(...)

if #args < 2 then
  io.write("Usage: harvest <width> <depth>\n")
  io.write("  width = number of columns\n")
  io.write("  depth = number of rows\n")
  io.write("\nSlot 1: seeds for replanting\n")
  io.write("Face the first crop row before starting.\n")
  io.write("Place a chest BEHIND the robot for output.\n")
  os.exit(1)
end

local W = math.max(1, math.floor(tonumber(args[1]) or 1))
local D = math.max(1, math.floor(tonumber(args[2]) or 1))

local SEED_SLOT = 1

local function move(fn)
  local ok = fn()
  if not ok then
    -- May be blocked by a grown crop / entity
    robot.swing()
    fn()
  end
end

-- Harvest the crop below (use sickle/hoe behaviour: swingDown + replant)
local function harvestAndReplant()
  -- Use the tool on the crop below
  local used, _ = robot.useDown()
  if used then
    -- Replant seed
    robot.select(SEED_SLOT)
    if robot.count(SEED_SLOT) > 0 then
      robot.placeDown()
    end
  end
end

-- Deposit harvested items into chest behind robot
local function deposit()
  robot.turnAround()
  for slot = 2, 16 do
    local count = robot.count(slot)
    if count > 0 then
      robot.select(slot)
      robot.drop(count)
    end
  end
  robot.select(1)
  robot.turnAround()
end

io.write(string.format("Harvesting %dx%d field...\n", W, D))
io.write("Starting in 2 seconds...\n")
os.sleep(2)

local total = 0

for row = 1, D do
  for col = 1, W do
    harvestAndReplant()
    total = total + 1

    -- Check inventory
    local used = 0
    for s = 2, 16 do if robot.count(s) > 0 then used = used + 1 end end
    if used >= 13 then
      -- Navigate back to deposit, then return to position
      -- Simplified: just continue and deposit at end
      io.write("  Inventory nearly full - consider adding more chests\n")
    end

    -- Move to next col (except last col in row)
    if col < W then
      move(robot.forward)
    end
  end

  -- End of this row
  if row < D then
    -- Snake pattern: alternate which side to turn
    if row % 2 == 1 then
      robot.turnRight()
      move(robot.forward)
      robot.turnRight()
    else
      robot.turnLeft()
      move(robot.forward)
      robot.turnLeft()
    end
    -- Move back to column 0 of next row
    for _ = 1, W - 1 do
      move(robot.forward)
    end
    robot.turnAround()
  else
    -- Last row: return to start
    robot.turnAround()
    for _ = 1, W - 1 do
      move(robot.forward)
    end
    -- Move back to start row
    if D % 2 == 0 then
      robot.turnRight()
      for _ = 1, D - 1 do
        move(robot.forward)
      end
      robot.turnLeft()
    end
  end

  io.write(string.format("Row %d/%d done\n", row, D))
end

-- Final deposit
deposit()

io.write(string.format("Harvest complete! %d blocks visited.\n", total))
