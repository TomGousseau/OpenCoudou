-- colors.lua - Color constants library
-- Provides named color values for use with GPU/terminal

local colors = {}

-- Standard Minecraft colors (matching dye names)
colors.white      = 0xFFFFFF
colors.orange     = 0xFF6600
colors.magenta    = 0xFF00FF
colors.lightBlue  = 0x6699FF
colors.yellow     = 0xFFFF00
colors.lime       = 0x00FF00
colors.pink       = 0xFF6699
colors.gray       = 0x555555
colors.lightGray  = 0xAAAAAA
colors.cyan       = 0x00FFFF
colors.purple     = 0x9900CC
colors.blue       = 0x0000FF
colors.brown      = 0x663300
colors.green      = 0x007700
colors.red        = 0xFF0000
colors.black      = 0x000000

-- Additional useful colors
colors.silver     = 0xCCCCCC
colors.maroon     = 0x800000
colors.olive      = 0x808000
colors.teal       = 0x008080
colors.navy       = 0x000080

-- Terminal / ANSI-style colors
colors.terminal = {
    black        = 0x000000,
    darkRed      = 0xAA0000,
    darkGreen    = 0x00AA00,
    darkYellow   = 0xAA5500,
    darkBlue     = 0x0000AA,
    darkMagenta  = 0xAA00AA,
    darkCyan     = 0x00AAAA,
    lightGray    = 0xAAAAAA,
    darkGray     = 0x555555,
    red          = 0xFF5555,
    green        = 0x55FF55,
    yellow       = 0xFFFF55,
    blue         = 0x5555FF,
    magenta      = 0xFF55FF,
    cyan         = 0x55FFFF,
    white        = 0xFFFFFF,
}

-- Default foreground / background palette for OpenOS
colors.palette = {
    colors.terminal.white,      -- 0 default fg
    colors.terminal.black,      -- 1 default bg
    colors.terminal.darkGreen,  -- 2 success
    colors.terminal.red,        -- 3 error
    colors.terminal.yellow,     -- 4 warning
    colors.terminal.cyan,       -- 5 info
    colors.terminal.darkBlue,   -- 6 header
    colors.terminal.darkGray,   -- 7 muted
}

-- Convert color integer to hex string
function colors.toHex(color)
    return string.format("#%06X", color)
end

-- Convert hex string to color integer
function colors.fromHex(hex)
    hex = hex:gsub("^#", ""):gsub("^0x", ""):gsub("^0X", "")
    return tonumber(hex, 16) or 0
end

-- Mix two colors by ratio (0.0 = full a, 1.0 = full b)
function colors.mix(a, b, ratio)
    ratio = math.max(0, math.min(1, ratio or 0.5))
    local ra = (a >> 16) & 0xFF
    local ga = (a >>  8) & 0xFF
    local ba =  a        & 0xFF
    local rb = (b >> 16) & 0xFF
    local gb = (b >>  8) & 0xFF
    local bb =  b        & 0xFF
    local r = math.floor(ra + (rb - ra) * ratio)
    local g = math.floor(ga + (gb - ga) * ratio)
    local bv= math.floor(ba + (bb - ba) * ratio)
    return (r << 16) | (g << 8) | bv
end

-- Darken a color by factor (0.0 = black, 1.0 = original)
function colors.darken(c, factor)
    return colors.mix(0x000000, c, factor)
end

-- Lighten a color by factor (0.0 = original, 1.0 = white)
function colors.lighten(c, factor)
    return colors.mix(c, 0xFFFFFF, factor)
end

-- Get luminance (perceived brightness 0.0 - 1.0)
function colors.luminance(c)
    local r = ((c >> 16) & 0xFF) / 255
    local g = ((c >>  8) & 0xFF) / 255
    local b = (c         & 0xFF) / 255
    return 0.299 * r + 0.587 * g + 0.114 * b
end

-- Return white or black depending on background for contrast
function colors.contrast(bg)
    if colors.luminance(bg) > 0.5 then
        return colors.black
    else
        return colors.white
    end
end

-- Named color lookup
function colors.find(name)
    name = name:lower()
    for k, v in pairs(colors) do
        if type(v) == "number" and k:lower() == name then
            return v
        end
    end
    return nil
end

-- Iterate all named colors
function colors.list()
    local result = {}
    for k, v in pairs(colors) do
        if type(v) == "number" then
            result[k] = v
        end
    end
    return result
end

return colors
