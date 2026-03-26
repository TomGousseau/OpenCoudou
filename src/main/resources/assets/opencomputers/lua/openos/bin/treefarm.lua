-- bin/treefarm.lua - Robot tree farm
-- Chops trees in a grid, replants saplings

local robot = require("robot")
local shell = require("shell")
local args  = shell.parse(...)

if #args < 2 then
  io.write("Usage: treefarm <cols> <rows> [spacing]\n")
  io.write("  cols    = number of trees wide\n")
  io.write("  rows    = number of trees deep\n")
  io.write("  spacing = blocks between trees (default 2)\n")
  io.write("\nInventory slot 1: saplings\n")
  io.write("Place a chest behind the robot for wood collection.\n")
  os.exit(1)
end

local COLS    = math.max(1, math.floor(tonumber(args[1]) or 1))
local ROWS    = math.max(1, math.floor(tonumber(args[2]) or 1))
local SPACING = math.max(1, math.floor(tonumber(args[3]) or 2))

-- Sapling slot
local SAPLING_SLOT = 1

local function move(fn, times)
  times = times or 1
  for _ = 1, times do
    local ok, _ = fn()
    if not ok then
      -- Try to break obstacle
      robot.swing()
      fn()
    end
  end
end

-- Chop upward until no more logs above
local function chopUp()
  local chopped = 1 -- we'll count the ground log later
  robot.swing() -- chop ground level
  while robot.detectUp() do
    robot.swingUp()
    move(robot.up)
    chopped = chopped + 1
  end
  -- Descend back to ground
  for _ = 1, chopped - 1 do
    move(robot.down)
  end
end

-- Plant sapling from slot 1
local function plantSapling()
  robot.select(SAPLING_SLOT)
  if robot.count(SAPLING_SLOT) > 0 then
    robot.place()
    io.write("  Planted sapling\n")
  else
    io.write("  WARNING: No saplings in slot 1!\n")
  end
end

-- Deposit wood into chest behind robot
local function deposit()
  robot.turnAround()
  for slot = 2, 16 do
    local count = robot.count(slot)
    if count > 0 then
      robot.select(slot)
      robot.drop(count)
    end
  end
  robot.turnAround()
end

local function isTree()
  -- Try to detect if there's a tree (wood block) in front
  return robot.detect()
end

-- Visit each tree position
io.write(string.format("Tree farm: %d x %d grid, spacing %d\n", COLS, ROWS, SPACING))
io.write("Starting in 2 seconds...\n")
os.sleep(2)

local visited = 0
for row = 1, ROWS do
  for col = 1, COLS do
    -- At tree position: chop if grown, replant if not
    if isTree() then
      io.write(string.format("Tree at (%d,%d) - chopping...\n", col, row))
      chopUp()
      os.sleep(0.5)
      plantSapling()
    else
      -- Check if sapling is already here (no detect = air = needs planting)
      io.write(string.format("Slot (%d,%d) - empty, planting...\n", col, row))
      plantSapling()
    end
    visited = visited + 1

    -- Deposit if inventory getting full
    local used = 0
    for s = 2, 16 do if robot.count(s) > 0 then used = used + 1 end end
    if used >= 13 then deposit() end

    -- Move to next tree in this row
    if col < COLS then
      move(robot.forward, SPACING)
    end
  end

  -- End of row: move to next row
  if row < ROWS then
    -- Turn, advance rows, turn back
    if row % 2 == 1 then
      robot.turnRight()
      move(robot.forward, SPACING)
      robot.turnRight()
    else
      robot.turnLeft()
      move(robot.forward, SPACING)
      robot.turnLeft()
    end
    -- Return to col 0 of new row
    move(robot.forward, (COLS - 1) * SPACING)
    robot.turnAround()
  end
end

-- Final deposit
deposit()

io.write(string.format("Tree farm cycle complete (%d positions visited).\n", visited))
