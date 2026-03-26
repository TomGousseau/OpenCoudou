-- print3d.lua - 3D Printer interface library and print command
local component = component
local term = require("term")
local shell = require("shell")
local serialization = require("serialization")
local fs = require("filesystem")
local text = require("text")

-- ------- Library interface -------

local print3d = {}

-- Get the printer component
local function getPrinter()
    local addr = component.list("printer")()
    if not addr then
        error("no printer connected", 2)
    end
    return addr
end

-- Build a simple cuboid model
function print3d.newModel(label)
    return {
        label = label or "Printed Block",
        tooltip = "",
        shapes = {},
        buttonMode = false,
        lightLevel = 0,
    }
end

-- Add a shape to a model (coordinates in 0-16 voxel space)
function print3d.addShape(model, x1, y1, z1, x2, y2, z2, texture, tint, state)
    table.insert(model.shapes, {
        minX = x1, minY = y1, minZ = z1,
        maxX = x2, maxY = y2, maxZ = z2,
        texture = texture or "minecraft:block/stone",
        tint = tint or 0xFFFFFF,
        state = state or 0,
    })
end

-- Prepare the printer with a model
function print3d.prepare(model)
    local addr = getPrinter()
    
    -- Reset what's queued
    component.invoke(addr, "reset")
    component.invoke(addr, "setLabel", model.label or "Printed Block")
    component.invoke(addr, "setTooltip", model.tooltip or "")
    component.invoke(addr, "setButtonMode", model.buttonMode or false)
    component.invoke(addr, "setLightLevel", model.lightLevel or 0)
    
    for _, shape in ipairs(model.shapes) do
        local ok, err = component.invoke(addr, "addShape",
            shape.minX, shape.minY, shape.minZ,
            shape.maxX, shape.maxY, shape.maxZ,
            shape.texture, shape.tint, shape.state)
        if not ok then
            return false, "addShape failed: " .. tostring(err)
        end
    end
    
    return true
end

-- Print a model (blocks until done or times out)
function print3d.print(model, count, showProgress)
    count = count or 1
    showProgress = showProgress ~= false
    
    local ok, err = print3d.prepare(model)
    if not ok then return false, err end
    
    local addr = getPrinter()
    ok, err = component.invoke(addr, "commit", count)
    if not ok then return false, err end
    
    while true do
        local status, progress = component.invoke(addr, "status")
        if status == "idle" then
            return true
        elseif status == "busy" then
            if showProgress then
                local pct = math.floor((progress or 0) * 100)
                io.write("\rPrinting... " .. pct .. "%    ")
            end
            -- yield
            local sig = { computer.pullSignal(0.25) }
            if sig[1] == "print_complete" then
                if showProgress then io.write("\rDone!              \n") end
                return true
            end
        else
            return false, "unexpected status: " .. tostring(status)
        end
    end
end

-- Load model from a .3dm file (JSON-like serialized table)
function print3d.loadModel(path)
    local resolved = shell.resolve(path, "3dm")
    if not fs.exists(resolved) then
        return nil, "file not found: " .. path
    end
    local data = fs.readAll(resolved)
    if not data then return nil, "cannot read file" end
    
    local ok, model = pcall(serialization.unserialize, data)
    if not ok then return nil, "parse error: " .. tostring(model) end
    return model
end

-- Save model to file
function print3d.saveModel(path, model)
    local resolved = shell.resolve(path)
    if not resolved:match("%.3dm$") then resolved = resolved .. ".3dm" end
    return fs.writeAll(resolved, serialization.pretty(model))
end

-- Builtin shapes

-- Full block
function print3d.fullBlock(texture, tint)
    local m = print3d.newModel("Block")
    print3d.addShape(m, 0, 0, 0, 16, 16, 16, texture, tint, 0)
    return m
end

-- Slab (bottom half)
function print3d.slab(texture, tint)
    local m = print3d.newModel("Slab")
    print3d.addShape(m, 0, 0, 0, 16, 8, 16, texture, tint, 0)
    return m
end

-- Stairs
function print3d.stairs(texture, tint)
    local m = print3d.newModel("Stairs")
    print3d.addShape(m, 0, 0, 0, 16, 8, 16, texture, tint, 0)
    print3d.addShape(m, 0, 8, 8, 16, 16, 16, texture, tint, 0)
    return m
end

-- Thin panel (wall hanging picture style)
function print3d.panel(texture, tint)
    local m = print3d.newModel("Panel")
    print3d.addShape(m, 0, 0, 0, 16, 16, 2, texture, tint, 0)
    return m
end

-- ------- Command usage -------

-- If invoked directly as a command: print3d <model_file> [count]
local args = { ... }
if #args > 0 then
    local modelPath = args[1]
    local count = tonumber(args[2]) or 1
    
    local model, err = print3d.loadModel(modelPath)
    if not model then
        io.stderr:write("print3d: " .. tostring(err) .. "\n")
        os.exit(1)
    end
    
    term.setForeground(0x00AAFF)
    print("Printing: " .. (model.label or modelPath) .. " x" .. count)
    term.setForeground(0xFFFFFF)
    
    -- Check printer status
    local addr = component.list("printer")()
    if not addr then
        io.stderr:write("print3d: no printer found\n")
        os.exit(1)
    end
    
    local status, progress = component.invoke(addr, "status")
    if status == "busy" then
        io.stderr:write("print3d: printer is busy (progress: " .. math.floor((progress or 0) * 100) .. "%)\n")
        os.exit(1)
    end
    
    local ink = component.invoke(addr, "getInkLevel")
    local chamelium = component.invoke(addr, "getChameliumLevel")
    print(string.format("Materials: ink=%.0f%%  chamelium=%d", (ink or 0) * 100, chamelium or 0))
    
    local ok, printErr = print3d.print(model, count, true)
    if not ok then
        io.stderr:write("print3d: " .. tostring(printErr) .. "\n")
        os.exit(1)
    end
    
    print("Print complete!")
end

return print3d
