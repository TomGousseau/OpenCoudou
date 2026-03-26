-- hologram.lua - Hologram projector tools
local component = component
local shell = require("shell")
local term = require("term")
local text = require("text")

local args = { ... }

local function getHologram()
    local addr = component.list("hologram")()
    if not addr then
        io.stderr:write("hologram: no hologram projector found\n")
        os.exit(1)
    end
    return addr
end

local function usage()
    print("Usage: hologram <command> [args]")
    print("Commands:")
    print("  clear               - clear the hologram")
    print("  set <x> <y> <z> <v> - set voxel at x,y,z to value v")
    print("  scale <s>           - set display scale (0.33-3.0)")
    print("  translate <x> <y> <z> - shift hologram offset")
    print("  palette <n> <color> - set palette color n (1-3) to hex color")
    print("  sphere <r> [v]      - draw a wireframe sphere of radius r")
    print("  box <x1> <y1> <z1> <x2> <y2> <z2> [v] - draw box")
    print("  demo                - run a rotating cube demo")
end

local cmd = args[1]
if not cmd or cmd == "help" or cmd == "--help" then
    usage()
    return
end

local addr = getHologram()

if cmd == "clear" then
    component.invoke(addr, "clear")
    print("Hologram cleared")

elseif cmd == "set" then
    local x = tonumber(args[2]) or 0
    local y = tonumber(args[3]) or 0
    local z = tonumber(args[4]) or 0
    local v = tonumber(args[5]) or 1
    component.invoke(addr, "set", x, y, z, v)

elseif cmd == "scale" then
    local s = tonumber(args[2]) or 1
    component.invoke(addr, "setScale", s)
    print("Scale set to " .. s)

elseif cmd == "translate" then
    local x = tonumber(args[2]) or 0
    local y = tonumber(args[3]) or 0
    local z = tonumber(args[4]) or 0
    component.invoke(addr, "setTranslation", x, y, z)
    print(string.format("Translation set to %.2f, %.2f, %.2f", x, y, z))

elseif cmd == "palette" then
    local n = tonumber(args[2]) or 1
    local colorStr = args[3] or "FF0000"
    local color = tonumber("0x" .. colorStr:gsub("^#", "")) or 0xFF0000
    component.invoke(addr, "setPaletteColor", n, color)
    print(string.format("Palette %d set to #%06X", n, color))

elseif cmd == "sphere" then
    local r = tonumber(args[2]) or 10
    local v = tonumber(args[3]) or 1
    local cx, cy, cz = 24, 24, 24
    local threshold = 0.5

    component.invoke(addr, "clear")
    local count = 0

    for x = math.max(1, cx - r - 1), math.min(48, cx + r + 1) do
        for y = math.max(1, cy - r - 1), math.min(48, cy + r + 1) do
            for z = math.max(1, cz - r - 1), math.min(48, cz + r + 1) do
                local dist = math.sqrt((x-cx)^2 + (y-cy)^2 + (z-cz)^2)
                if math.abs(dist - r) < threshold then
                    component.invoke(addr, "set", x, y, z, v)
                    count = count + 1
                end
            end
        end
    end

    print("Drew sphere with " .. count .. " voxels")

elseif cmd == "box" then
    local x1 = tonumber(args[2]) or 1
    local y1 = tonumber(args[3]) or 1
    local z1 = tonumber(args[4]) or 1
    local x2 = tonumber(args[5]) or 48
    local y2 = tonumber(args[6]) or 48
    local z2 = tonumber(args[7]) or 48
    local v = tonumber(args[8]) or 1

    component.invoke(addr, "fill", x1, y1, z1, x2-x1+1, y2-y1+1, z2-z1+1, v)
    print(string.format("Filled box (%d,%d,%d) to (%d,%d,%d)", x1, y1, z1, x2, y2, z2))

elseif cmd == "demo" then
    print("Running hologram demo... (press any key to stop)")
    component.invoke(addr, "clear")
    
    local size = 16
    local cx, cy, cz = 24, 24, 24
    local t = 0
    
    while true do
        -- Check for keypress
        local sig = { computer.pullSignal(0.05) }
        if sig[1] == "key_down" then break end
        
        component.invoke(addr, "clear")
        
        -- Rotating cube wireframe
        local angle = t * 0.05
        local cos_a = math.cos(angle)
        local sin_a = math.sin(angle)
        
        -- Draw cube edges by plotting points along edges
        local function rotateY(x, y, z)
            return x * cos_a - z * sin_a, y, x * sin_a + z * cos_a
        end
        
        -- 8 cube corners
        local corners = {
            {-size, -size, -size}, {size, -size, -size},
            {size, size, -size}, {-size, size, -size},
            {-size, -size, size}, {size, -size, size},
            {size, size, size}, {-size, size, size},
        }
        
        local rotated = {}
        for _, c in ipairs(corners) do
            local rx, ry, rz = rotateY(c[1], c[2], c[3])
            table.insert(rotated, {math.floor(cx + rx/2), math.floor(cy + ry/2), math.floor(cz + rz/2)})
        end
        
        -- Draw edges
        local edges = {
            {1,2},{2,3},{3,4},{4,1}, -- bottom face
            {5,6},{6,7},{7,8},{8,5}, -- top face
            {1,5},{2,6},{3,7},{4,8}  -- vertical edges
        }
        
        local function drawLine(p1, p2, val)
            local steps = 20
            for i = 0, steps do
                local f = i / steps
                local x = math.floor(p1[1] + (p2[1]-p1[1]) * f)
                local y = math.floor(p1[2] + (p2[2]-p1[2]) * f)
                local z = math.floor(p1[3] + (p2[3]-p1[3]) * f)
                if x >= 1 and x <= 48 and y >= 1 and y <= 48 and z >= 1 and z <= 48 then
                    component.invoke(addr, "set", x, y, z, val)
                end
            end
        end
        
        for _, e in ipairs(edges) do
            drawLine(rotated[e[1]], rotated[e[2]], 1)
        end
        
        t = t + 1
    end
    
    component.invoke(addr, "clear")
    print("Demo stopped")

else
    io.stderr:write("hologram: unknown command '" .. cmd .. "'\n")
    usage()
end
