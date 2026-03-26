-- components.lua - List and inspect connected components
local component = component
local term = require("term")
local text = require("text")
local serialization = require("serialization")

local args = { ... }
local filterType = args[1]
local showMethods = args[2] == "-m" or args[2] == "--methods"

local w = select(1, term.getSize())

-- List components
local found = {}
for addr, ctype in component.list(filterType) do
    table.insert(found, { addr = addr, type = ctype })
end

table.sort(found, function(a, b)
    if a.type ~= b.type then return a.type < b.type end
    return a.addr < b.addr
end)

if #found == 0 then
    if filterType then
        print("No components of type '" .. filterType .. "' found")
    else
        print("No components connected")
    end
    return
end

term.setForeground(0x88AAFF)
print(text.padRight("Type", 20) .. "Address")
term.setForeground(0xFFFFFF)
print(string.rep("-", w < 60 and 60 or w - 1))

local isPrimary = {}
-- Mark primary components
for addr, ctype in component.list() do
    local primary = component.list(ctype)()
    if primary == addr then
        isPrimary[addr] = true
    end
end

for _, comp in ipairs(found) do
    local isP = isPrimary[comp.addr]
    if isP then term.setForeground(0xFFFF44) else term.setForeground(0xFFFFFF) end
    
    local mark = isP and "* " or "  "
    print(mark .. text.padRight(comp.type, 18) .. comp.addr)
    
    if showMethods then
        term.setForeground(0xAAAAAA)
        local methods = component.methods(comp.addr)
        if methods then
            local mlist = {}
            for name, _ in pairs(methods) do
                table.insert(mlist, name)
            end
            table.sort(mlist)
            print("   Methods: " .. table.concat(mlist, ", "))
        end
        term.setForeground(0xFFFFFF)
    end
end

term.setForeground(0xFFFFFF)
print("")
print(string.format("Total: %d component(s)", #found))
if not filterType then
    print("Usage: components [type] [-m]  -- filter by type, -m shows methods")
    print("* = primary component of its type")
end
