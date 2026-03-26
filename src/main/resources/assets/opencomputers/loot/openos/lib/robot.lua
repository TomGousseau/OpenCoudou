-- Robot Control Library
-- Provides high-level robot control functions

local robot = {}

local component = component
local computer = computer

-- Movement directions
robot.FORWARD = 3
robot.BACK = 2
robot.UP = 1
robot.DOWN = 0

-- ========================================
-- Robot Component Access
-- ========================================

local function getRobot()
  local addr = component.list("robot")()
  if addr then
    return component.proxy(addr)
  end
  return nil
end

-- ========================================
-- Movement
-- ========================================

function robot.forward()
  local r = getRobot()
  if r then
    return r.move(robot.FORWARD)
  end
  return false, "no robot"
end

function robot.back()
  local r = getRobot()
  if r then
    return r.move(robot.BACK)
  end
  return false, "no robot"
end

function robot.up()
  local r = getRobot()
  if r then
    return r.move(robot.UP)
  end
  return false, "no robot"
end

function robot.down()
  local r = getRobot()
  if r then
    return r.move(robot.DOWN)
  end
  return false, "no robot"
end

function robot.turnLeft()
  local r = getRobot()
  if r then
    return r.turn(false)
  end
  return false, "no robot"
end

function robot.turnRight()
  local r = getRobot()
  if r then
    return r.turn(true)
  end
  return false, "no robot"
end

function robot.turnAround()
  robot.turnRight()
  return robot.turnRight()
end

-- ========================================
-- Interaction
-- ========================================

function robot.swing(side)
  local r = getRobot()
  if r then
    return r.swing(side or robot.FORWARD)
  end
  return false, "no robot"
end

function robot.use(side, sneaky, duration)
  local r = getRobot()
  if r then
    return r.use(side or robot.FORWARD, sneaky, duration)
  end
  return false, "no robot"
end

function robot.place(side, sneaky)
  local r = getRobot()
  if r then
    return r.place(side or robot.FORWARD, sneaky)
  end
  return false, "no robot"
end

function robot.detect(side)
  local r = getRobot()
  if r then
    return r.detect(side or robot.FORWARD)
  end
  return false, "no robot"
end

function robot.detectUp()
  return robot.detect(robot.UP)
end

function robot.detectDown()
  return robot.detect(robot.DOWN)
end

-- ========================================
-- Inventory
-- ========================================

function robot.select(slot)
  local r = getRobot()
  if r then
    return r.select(slot)
  end
  return false, "no robot"
end

function robot.selected()
  local r = getRobot()
  if r then
    return r.selected()
  end
  return 0
end

function robot.count(slot)
  local r = getRobot()
  if r then
    return r.count(slot or robot.selected())
  end
  return 0
end

function robot.space(slot)
  local r = getRobot()
  if r then
    return r.space(slot or robot.selected())
  end
  return 0
end

function robot.inventorySize()
  local r = getRobot()
  if r then
    return r.inventorySize()
  end
  return 0
end

function robot.drop(count, side)
  local r = getRobot()
  if r then
    return r.drop(side or robot.FORWARD, count)
  end
  return false, "no robot"
end

function robot.dropUp(count)
  return robot.drop(count, robot.UP)
end

function robot.dropDown(count)
  return robot.drop(count, robot.DOWN)
end

function robot.suck(count, side)
  local r = getRobot()
  if r then
    return r.suck(side or robot.FORWARD, count)
  end
  return false, "no robot"
end

function robot.suckUp(count)
  return robot.suck(count, robot.UP)
end

function robot.suckDown(count)
  return robot.suck(count, robot.DOWN)
end

function robot.transfer(slot, count)
  local r = getRobot()
  if r then
    return r.transfer(slot, count)
  end
  return false, "no robot"
end

-- ========================================
-- Tool
-- ========================================

function robot.durability()
  local r = getRobot()
  if r then
    return r.durability()
  end
  return 0
end

-- ========================================
-- Tank (if tank upgrade installed)
-- ========================================

function robot.tankCount()
  local r = getRobot()
  if r and r.tankCount then
    return r.tankCount()
  end
  return 0
end

function robot.selectTank(tank)
  local r = getRobot()
  if r and r.selectTank then
    return r.selectTank(tank)
  end
  return false, "no tank upgrade"
end

function robot.tankLevel(tank)
  local r = getRobot()
  if r and r.tankLevel then
    return r.tankLevel(tank)
  end
  return 0
end

function robot.tankSpace(tank)
  local r = getRobot()
  if r and r.tankSpace then
    return r.tankSpace(tank)
  end
  return 0
end

function robot.drain(count, side)
  local r = getRobot()
  if r and r.drain then
    return r.drain(side or robot.FORWARD, count)
  end
  return false, "no tank upgrade"
end

function robot.fill(count, side)
  local r = getRobot()
  if r and r.fill then
    return r.fill(side or robot.FORWARD, count)
  end
  return false, "no tank upgrade"
end

-- ========================================
-- High-Level Functions
-- ========================================

-- Move multiple blocks in a direction
function robot.move(direction, count)
  count = count or 1
  local moved = 0
  
  local moveFunc
  if direction == robot.FORWARD then moveFunc = robot.forward
  elseif direction == robot.BACK then moveFunc = robot.back
  elseif direction == robot.UP then moveFunc = robot.up
  elseif direction == robot.DOWN then moveFunc = robot.down
  else return false, "invalid direction"
  end
  
  for i = 1, count do
    local ok, err = moveFunc()
    if not ok then
      return false, err, moved
    end
    moved = moved + 1
  end
  
  return true, nil, moved
end

-- Find and select a non-empty slot
function robot.findItem()
  for slot = 1, robot.inventorySize() do
    if robot.count(slot) > 0 then
      robot.select(slot)
      return slot
    end
  end
  return nil
end

-- Compact inventory
function robot.compactInventory()
  local size = robot.inventorySize()
  for target = 1, size - 1 do
    if robot.count(target) == 0 then
      for source = target + 1, size do
        if robot.count(source) > 0 then
          robot.select(source)
          robot.transfer(target)
          break
        end
      end
    end
  end
end

return robot
