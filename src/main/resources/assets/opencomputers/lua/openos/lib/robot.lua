-- robot.lua - Robot control library
-- Wraps the robot component API with helpful utilities

local robot = {}

-- Lazy-load the component proxy
local function R()
    return component.proxy(component.list("robot")())
end

-- -----------------------------------------------------------------------
-- Movement
-- -----------------------------------------------------------------------

--- Move robot forward one block
function robot.forward()
    return R().move(3) -- sides.front
end

--- Move robot backward one block
function robot.back()
    return R().move(2) -- sides.back
end

--- Move robot up one block
function robot.up()
    return R().move(1) -- sides.top
end

--- Move robot down one block
function robot.down()
    return R().move(0) -- sides.bottom
end

--- Turn robot left 90 degrees
function robot.turnLeft()
    return R().turn(false)
end

--- Turn robot right 90 degrees
function robot.turnRight()
    return R().turn(true)
end

--- Turn robot 180 degrees
function robot.turnAround()
    R().turn(true)
    R().turn(true)
end

--- Move forward n steps, return false + reason on failure
function robot.forward_n(n)
    for i = 1, n do
        local ok, err = R().move(3)
        if not ok then return false, err, i end
    end
    return true
end

--- Move up n steps
function robot.up_n(n)
    for i = 1, n do
        local ok, err = R().move(1)
        if not ok then return false, err, i end
    end
    return true
end

--- Move in a direction by side constant
function robot.move(side)
    return R().move(side)
end

--- Turn to face a specific side (front/back/left/right only)
--- Requires tracking current facing! This is a stub; full nav needs a compass.
function robot.turn(clockwise)
    return R().turn(clockwise == true or clockwise == nil)
end

-- -----------------------------------------------------------------------
-- Detection
-- -----------------------------------------------------------------------

--- Detect block in front
function robot.detect()
    return R().detect()
end

--- Detect block above
function robot.detectUp()
    return R().detectUp()
end

--- Detect block below
function robot.detectDown()
    return R().detectDown()
end

--- Detect in given direction (0=down,1=up,2=back,3=front,4=right,5=left)
function robot.detectSide(side)
    if side == 1 then return R().detectUp()
    elseif side == 0 then return R().detectDown()
    else return R().detect() end
end

-- -----------------------------------------------------------------------
-- Mining / Interaction
-- -----------------------------------------------------------------------

--- Swing axe/pickaxe forward
function robot.swing()
    return R().swing(3)
end

--- Mine block above
function robot.swingUp()
    return R().swing(1)
end

--- Mine block below
function robot.swingDown()
    return R().swing(0)
end

--- Use equipped tool forward (place/activate)
function robot.use()
    return R().use(3)
end

--- Use tool above
function robot.useUp()
    return R().use(1)
end

--- Use tool below
function robot.useDown()
    return R().use(0)
end

--- Place block from selected slot forward
function robot.place()
    return R().place(3)
end

--- Place block above
function robot.placeUp()
    return R().place(1)
end

--- Place block below
function robot.placeDown()
    return R().place(0)
end

--- Drop items from selected slot forward
function robot.drop(count)
    return R().drop(3, count)
end

--- Drop items above
function robot.dropUp(count)
    return R().drop(1, count)
end

--- Drop items below
function robot.dropDown(count)
    return R().drop(0, count)
end

--- Pick up item forward
function robot.suck(count)
    return R().suck(3, count)
end

--- Pick up item above
function robot.suckUp(count)
    return R().suck(1, count)
end

--- Pick up item below
function robot.suckDown(count)
    return R().suck(0, count)
end

-- -----------------------------------------------------------------------
-- Inventory
-- -----------------------------------------------------------------------

--- Get selected inventory slot (1-based)
function robot.slot()
    return R().select()
end

--- Select an inventory slot (1-based)
function robot.select(slot)
    return R().select(slot)
end

--- Get item count in slot (default: selected slot)
function robot.count(slot)
    return R().count(slot)
end

--- Get item space in slot (default: selected slot)
function robot.space(slot)
    return R().space(slot)
end

--- Transfer items from selected slot to another slot
function robot.transferTo(destSlot, count)
    return R().transferTo(destSlot, count)
end

--- Compare selected slot with block in front
function robot.compare()
    return R().compare()
end

--- Compare selected slot with block above
function robot.compareUp()
    return R().compareUp()
end

--- Compare selected slot with block below
function robot.compareDown()
    return R().compareDown()
end

--- Get inventory size (number of slots)
function robot.inventorySize()
    return R().inventorySize()
end

--- Find first slot containing a non-empty item
function robot.firstFilledSlot()
    for i = 1, robot.inventorySize() do
        if R().count(i) > 0 then return i end
    end
    return nil
end

--- Find first empty slot
function robot.firstEmptySlot()
    for i = 1, robot.inventorySize() do
        if R().count(i) == 0 then return i end
    end
    return nil
end

--- Drop all items in inventory
function robot.dropAll()
    for i = 1, robot.inventorySize() do
        if R().count(i) > 0 then
            R().select(i)
            R().drop(3)
        end
    end
    R().select(1)
end

-- -----------------------------------------------------------------------
-- Energy
-- -----------------------------------------------------------------------

function robot.energy()
    return R().energy()
end

function robot.maxEnergy()
    return R().maxEnergy()
end

function robot.energyLevel()
    local max = robot.maxEnergy()
    if max == 0 then return 0 end
    return robot.energy() / max
end

-- Wait until energy level is at or above threshold (0.0-1.0)
function robot.waitForCharge(threshold)
    threshold = threshold or 0.9
    while robot.energyLevel() < threshold do
        os.sleep(5)
    end
end

-- -----------------------------------------------------------------------
-- Light / Name
-- -----------------------------------------------------------------------

function robot.getLightColor()
    return R().getLightColor()
end

function robot.setLightColor(color)
    return R().setLightColor(color)
end

function robot.name()
    return R().name()
end

-- -----------------------------------------------------------------------
-- High-level navigation helpers
-- -----------------------------------------------------------------------

--- Dig forward, moving through the hole
function robot.digForward()
    local det, _ = R().detect()
    if det then
        local ok, err = R().swing(3)
        if not ok then return false, err end
    end
    return R().move(3)
end

--- Dig up and move through
function robot.digUp()
    local det, _ = R().detectUp()
    if det then
        local ok, err = R().swing(1)
        if not ok then return false, err end
    end
    return R().move(1)
end

--- Dig down and move through
function robot.digDown()
    local det, _ = R().detectDown()
    if det then
        local ok, err = R().swing(0)
        if not ok then return false, err end
    end
    return R().move(0)
end

--- Tunnel forward n blocks (digs through everything)
function robot.tunnel(n)
    for i = 1, (n or 1) do
        if not robot.digForward() then
            return false, "blocked at step " .. i
        end
    end
    return true
end

--- Mine a 1x1xN vertical shaft downward
function robot.shaft(depth)
    for i = 1, (depth or 1) do
        if not robot.digDown() then
            return false, "blocked at depth " .. i
        end
    end
    return true
end

--- Fill in blocks below while moving forward n steps
function robot.fillForward(n)
    for i = 1, (n or 1) do
        if not robot.placeDown() then
            -- No block in inventory or couldn't place - just continue
        end
        if not R().move(3) then
            return false, "blocked at step " .. i
        end
    end
    return true
end

--- Return to surface from a vertical shaft (move up n blocks)
function robot.surface(depth)
    for i = 1, (depth or 1) do
        local ok, err = R().move(1)
        if not ok then return false, err, i end
    end
    return true
end

-- -----------------------------------------------------------------------
-- Inventory scanning
-- -----------------------------------------------------------------------

--- Get a summary table of all items in the robot's inventory
--- Returns { [slot] = { count, label, name } }
function robot.inventory()
    local inv = {}
    local api = R()
    for i = 1, api.inventorySize() do
        local count = api.count(i)
        if count > 0 then
            inv[i] = { count = count }
        end
    end
    return inv
end

-- -----------------------------------------------------------------------
-- Crafting
-- -----------------------------------------------------------------------

--- Craft using 3x3 recipe in first 9 slots, result goes to slot 10+
function robot.craft(count)
    local craftComp = component.list("crafting")()
    if not craftComp then
        return false, "no crafting upgrade installed"
    end
    return component.invoke(craftComp, "craft", count or 1)
end

-- -----------------------------------------------------------------------
-- Wireless / redstone helpers
-- -----------------------------------------------------------------------

function robot.redstone(side, value)
    local rs = component.list("redstone")()
    if not rs then return nil end
    if value then
        component.invoke(rs, "setOutput", side, value)
    else
        return component.invoke(rs, "getInput", side)
    end
end

return robot
