-- bin/quarry.lua - Robot quarry program
-- Mines a rectangular volume W x D x H and deposits items to a chest behind start

local robot   = require("robot")
local shell   = require("shell")
local args    = shell.parse(...)

if #args < 2 then
  io.write("Usage: quarry <width> <depth> [height]\n")
  io.write("  width  = X axis (perpendicular to robot facing)\n")
  io.write("  depth  = forward distance\n")
  io.write("  height = layers to mine downward (default 1)\n")
  io.write("\nPlace a chest directly BEHIND the robot before starting.\n")
  os.exit(1)
end

local W = math.max(1, math.floor(tonumber(args[1]) or 0))
local D = math.max(1, math.floor(tonumber(args[2]) or 0))
local H = math.max(1, math.floor(tonumber(args[3]) or 1))

if W == 0 or D == 0 then
  io.stderr:write("quarry: invalid dimensions\n")
  os.exit(1)
end

-- ──────────────────────────────────────────────
-- Helpers
-- ──────────────────────────────────────────────

local function move(fn, times)
  times = times or 1
  for _ = 1, times do
    while not fn() do
      os.sleep(0.5)
    end
  end
end

local function digAndMove(moveFn, digFn)
  digFn()
  while not moveFn() do
    digFn()
    os.sleep(0.2)
  end
end

-- Deposit all items into adjacent chest (the robot drops behind itself = back)
local function depositAll()
  -- Turn around to face the chest
  robot.turnAround()
  for slot = 1, 16 do
    robot.select(slot)
    local count = robot.count(slot)
    if count > 0 then
      robot.drop(count)
    end
  end
  robot.turnAround()
end

-- Return home (row, col track current position in XZ plane)
-- We store absolute position to navigate back
local startX, startZ = 0, 0  -- offsets from start
local facingOffset   = 0      -- 0=forward(south), 1=right(west), 2=back(north), 3=left(east)

-- State tracking
local curRow = 0  -- 0-indexed row in Z direction
local curCol = 0  -- 0-indexed col in X direction
local층 = 0       -- current layer (0 = top)

local function checkInventory()
  -- If inventory > 80% full, go deposit
  local used = 0
  for s = 1, 16 do
    if robot.count(s) > 0 then used = used + 1 end
  end
  if used >= 13 then
    -- Save position, return to start, deposit, come back
    -- simplified: just drop in front if there's a chest there
    depositAll()
  end
end

-- ──────────────────────────────────────────────
-- Main quarry logic
-- ──────────────────────────────────────────────

io.write(string.format("Quarry: %d x %d x %d (%d blocks)\n", W, D, H, W*D*H))
io.write("Starting in 3 seconds... (place chest behind robot)\n")
os.sleep(3)

-- Check energy
local energy, maxEnergy = robot.energy()
if energy < maxEnergy * 0.2 then
  io.stderr:write("quarry: low energy (" .. math.floor(energy) .. "/" .. math.floor(maxEnergy) .. ")\n")
  io.stderr:write("Charge to at least 20% before starting.\n")
  os.exit(1)
end

local blocksMineds = 0

for layer = 1, H do
  -- Mine downward at start of each layer (except first)
  if layer > 1 then
    digAndMove(robot.down, robot.swingDown)
  end

  -- Mine the W x D grid in a snake pattern
  for row = 1, D do
    -- Mine current row across W columns
    for col = 1, W do
      -- Mine the block in front
      robot.swingDown()  -- mine floor of current position
      
      if col < W then
        -- Move to next column
        if row % 2 == 1 then
          -- Going right: turn right, move, turn left (or just strafe)
          digAndMove(robot.forward, robot.swing)
        else
          -- Going left
          digAndMove(robot.forward, robot.swing)
        end
      end
      blocksMineds = blocksMineds + 1
      checkInventory()
    end

    -- Turn to align for next row
    if row < D then
      if row % 2 == 1 then
        -- After going right: turn right, advance, turn right
        robot.turnRight()
        digAndMove(robot.forward, robot.swing)
        robot.turnRight()
      else
        -- After going left: turn left, advance, turn left
        robot.turnLeft()
        digAndMove(robot.forward, robot.swing)
        robot.turnLeft()
      end
    end
  end

  -- Return to column 0 at end of this layer
  if D % 2 == 1 then
    -- Facing away from start: turn around for next layer descent
    robot.turnAround()
  end
  -- Move back to start column
  for _ = 1, D - 1 do
    move(robot.forward)
  end

  io.write(string.format("Layer %d/%d done (%d blocks mined)\n", layer, H, blocksMineds))
end

-- Final deposit
robot.turnAround()
depositAll()
robot.turnAround()

io.write(string.format("Quarry complete! Mined approximately %d blocks.\n", blocksMineds))
