-- text.lua - Text utility library

local unicode = unicode

local text = {}

-- Trim whitespace from start and end
function text.trim(str)
    return str:match("^%s*(.-)%s*$")
end

-- Trim from left only
function text.ltrim(str)
    return str:match("^%s*(.*)$")
end

-- Trim from right only
function text.rtrim(str)
    return str:match("^(.-)%s*$")
end

-- Split string by separator
function text.split(str, sep, plain, limit)
    sep = sep or "%s"
    plain = plain or false
    local result = {}
    local count = 0
    
    if limit and limit < 2 then
        return { str }
    end
    
    local pos = 1
    while true do
        local s, e = str:find(sep, pos, plain)
        if not s then
            table.insert(result, str:sub(pos))
            break
        end
        count = count + 1
        table.insert(result, str:sub(pos, s - 1))
        pos = e + 1
        if limit and count >= limit - 1 then
            table.insert(result, str:sub(pos))
            break
        end
    end
    return result
end

-- Tokenize a string (split by whitespace, respecting quotes)
function text.tokenize(str)
    local tokens = {}
    local token = ""
    local inSingle = false
    local inDouble = false
    local escape = false
    
    for i = 1, #str do
        local c = str:sub(i, i)
        if escape then
            token = token .. c
            escape = false
        elseif c == "\\" and (inSingle or inDouble) then
            escape = true
        elseif c == "'" and not inDouble then
            inSingle = not inSingle
        elseif c == '"' and not inSingle then
            inDouble = not inDouble
        elseif c:match("%s") and not inSingle and not inDouble then
            if #token > 0 then
                table.insert(tokens, token)
                token = ""
            end
        else
            token = token .. c
        end
    end
    
    if #token > 0 then
        table.insert(tokens, token)
    end
    
    return tokens
end

-- Wrap text to a given width, returning a table of lines
function text.wrap(str, width)
    width = width or 80
    local lines = {}
    local raw_lines = text.split(str, "\n", true)
    
    for _, line in ipairs(raw_lines) do
        if unicode.len(line) <= width then
            table.insert(lines, line)
        else
            local words = text.split(line, " ", true)
            local currentLine = ""
            
            for _, word in ipairs(words) do
                if unicode.len(currentLine) == 0 then
                    if unicode.len(word) > width then
                        -- Force break long word
                        while unicode.len(word) > width do
                            table.insert(lines, unicode.sub(word, 1, width))
                            word = unicode.sub(word, width + 1)
                        end
                        currentLine = word
                    else
                        currentLine = word
                    end
                elseif unicode.len(currentLine) + 1 + unicode.len(word) <= width then
                    currentLine = currentLine .. " " .. word
                else
                    table.insert(lines, currentLine)
                    if unicode.len(word) > width then
                        while unicode.len(word) > width do
                            table.insert(lines, unicode.sub(word, 1, width))
                            word = unicode.sub(word, width + 1)
                        end
                        currentLine = word
                    else
                        currentLine = word
                    end
                end
            end
            
            if #currentLine > 0 then
                table.insert(lines, currentLine)
            end
        end
    end
    
    return lines
end

-- Pad string to length (right pad)
function text.padRight(str, length, char)
    char = char or " "
    str = tostring(str)
    local len = unicode.len(str)
    if len >= length then
        return str
    end
    return str .. string.rep(char, length - len)
end

-- Pad string to length (left pad)
function text.padLeft(str, length, char)
    char = char or " "
    str = tostring(str)
    local len = unicode.len(str)
    if len >= length then
        return str
    end
    return string.rep(char, length - len) .. str
end

-- Center string within a width
function text.center(str, width, char)
    char = char or " "
    str = tostring(str)
    local len = unicode.len(str)
    if len >= width then return str end
    local leftPad = math.floor((width - len) / 2)
    local rightPad = width - len - leftPad
    return string.rep(char, leftPad) .. str .. string.rep(char, rightPad)
end

-- Truncate string to max width, optionally adding ellipsis
function text.truncate(str, maxLen, ellipsis)
    ellipsis = ellipsis or "..."
    if unicode.len(str) <= maxLen then return str end
    return unicode.sub(str, 1, maxLen - unicode.len(ellipsis)) .. ellipsis
end

-- Check if string starts with prefix
function text.startsWith(str, prefix)
    return str:sub(1, #prefix) == prefix
end

-- Check if string ends with suffix
function text.endsWith(str, suffix)
    return suffix == "" or str:sub(-#suffix) == suffix
end

-- Replace all occurrences (non-pattern)
function text.replace(str, find, replace)
    local result = ""
    local pos = 1
    while true do
        local s, e = str:find(find, pos, true)
        if not s then
            result = result .. str:sub(pos)
            break
        end
        result = result .. str:sub(pos, s - 1) .. replace
        pos = e + 1
    end
    return result
end

-- Count occurrences of a substring
function text.count(str, sub)
    local count = 0
    local pos = 1
    while true do
        local s = str:find(sub, pos, true)
        if not s then break end
        count = count + 1
        pos = s + #sub
    end
    return count
end

-- Format a table as a column-aligned layout
function text.tableFormat(rows, widths, separator)
    separator = separator or "  "
    local lines = {}
    for _, row in ipairs(rows) do
        local line = ""
        for i, cell in ipairs(row) do
            cell = tostring(cell)
            if widths and widths[i] then
                cell = text.padRight(cell, widths[i])
            end
            if i > 1 then line = line .. separator end
            line = line .. cell
        end
        table.insert(lines, line)
    end
    return lines
end

-- Build a separator line
function text.separator(width, char)
    return string.rep(char or "-", width or 80)
end

-- Convert a value to a printable string (similar to tostring but handles tables)
function text.serialize(val, indent, seen)
    local t = type(val)
    if t == "string" then
        return string.format("%q", val)
    elseif t == "number" or t == "boolean" then
        return tostring(val)
    elseif t == "nil" then
        return "nil"
    elseif t == "table" then
        seen = seen or {}
        if seen[val] then return "{...}" end
        seen[val] = true
        indent = indent or 0
        
        local parts = {}
        local isArray = true
        local maxN = 0
        for k, _ in pairs(val) do
            if type(k) ~= "number" or k ~= math.floor(k) or k < 1 then
                isArray = false
                break
            end
            if k > maxN then maxN = k end
        end
        if isArray and maxN ~= #val then isArray = false end
        
        if isArray then
            for i, v in ipairs(val) do
                table.insert(parts, text.serialize(v, indent, seen))
            end
        else
            for k, v in pairs(val) do
                local key
                if type(k) == "string" and k:match("^[%a_][%w_]*$") then
                    key = k
                else
                    key = "[" .. text.serialize(k, 0, seen) .. "]"
                end
                table.insert(parts, key .. " = " .. text.serialize(v, indent, seen))
            end
        end
        
        if indent > 0 then
            local pad = string.rep("  ", indent)
            return "{\n" .. pad .. table.concat(parts, ",\n" .. pad) .. "\n" .. string.rep("  ", indent - 1) .. "}"
        else
            return "{" .. table.concat(parts, ", ") .. "}"
        end
    else
        return tostring(val)
    end
end

return text
