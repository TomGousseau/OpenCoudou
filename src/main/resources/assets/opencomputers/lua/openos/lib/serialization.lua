-- serialization.lua - Data serialization library

local serialization = {}

-- Serialize a value to a string
function serialization.serialize(value, opts)
    opts = opts or {}
    local pretty = opts.pretty or false
    local indent = opts.indent or 0
    local seen = opts.seen or {}
    local allowFunctions = opts.allowFunctions or false
    
    local t = type(value)
    
    if t == "nil" then
        return "nil"
        
    elseif t == "boolean" then
        return value and "true" or "false"
        
    elseif t == "number" then
        if value ~= value then return "0/0" end -- NaN
        if value == math.huge then return "math.huge" end
        if value == -math.huge then return "-math.huge" end
        if math.type(value) == "integer" then
            return tostring(value)
        end
        return string.format("%.17g", value)
        
    elseif t == "string" then
        -- Escape special characters
        local escaped = value
            :gsub("\\", "\\\\")
            :gsub('"', '\\"')
            :gsub("\n", "\\n")
            :gsub("\r", "\\r")
            :gsub("\t", "\\t")
        -- Check for non-printable chars
        escaped = escaped:gsub("[%z\1-\31\127-\255]", function(c)
            return string.format("\\%d", string.byte(c))
        end)
        return '"' .. escaped .. '"'
        
    elseif t == "table" then
        if seen[value] then
            error("circular reference detected during serialization")
        end
        seen[value] = true
        
        local result = {}
        local maxIndex = 0
        local hasNonArray = false
        
        -- Check if purely sequential
        for k, _ in pairs(value) do
            if type(k) ~= "number" or k < 1 or math.floor(k) ~= k then
                hasNonArray = true
                break
            end
            if k > maxIndex then maxIndex = k end
        end
        if maxIndex ~= #value then hasNonArray = true end
        
        local innerOpts = { pretty = pretty, indent = indent + 1, seen = seen, allowFunctions = allowFunctions }
        
        if not hasNonArray then
            -- Array-style
            for i, v in ipairs(value) do
                table.insert(result, serialization.serialize(v, innerOpts))
            end
        else
            -- Mixed/map-style — serialize in sorted key order for determinism
            local keys = {}
            for k in pairs(value) do
                table.insert(keys, k)
            end
            table.sort(keys, function(a, b)
                if type(a) == type(b) then
                    return tostring(a) < tostring(b)
                end
                return type(a) < type(b)
            end)
            
            for _, k in ipairs(keys) do
                local v = value[k]
                local keyStr
                if type(k) == "string" and k:match("^[%a_][%w_]*$") then
                    keyStr = k
                else
                    keyStr = "[" .. serialization.serialize(k, innerOpts) .. "]"
                end
                local valStr = serialization.serialize(v, innerOpts)
                if pretty then
                    table.insert(result, string.rep("  ", indent + 1) .. keyStr .. " = " .. valStr)
                else
                    table.insert(result, keyStr .. "=" .. valStr)
                end
            end
        end
        
        seen[value] = nil
        
        if pretty then
            if #result == 0 then return "{}" end
            return "{\n" .. table.concat(result, ",\n") .. "\n" .. string.rep("  ", indent) .. "}"
        else
            return "{" .. table.concat(result, ",") .. "}"
        end
        
    elseif t == "function" and allowFunctions then
        -- Limited function serialization — just store marker
        return '"[function]"'
        
    else
        error("cannot serialize value of type " .. t)
    end
end

-- Alias
serialization.stringify = serialization.serialize

-- Deserialize a string back to a value
function serialization.unserialize(str)
    if type(str) ~= "string" then
        error("expected string, got " .. type(str))
    end
    
    -- Wrap in a return statement and load
    local chunk, err = load("return " .. str, "=unserialize", "t", {
        -- Safe sandbox — only allow literal values
        math = { huge = math.huge },
    })
    
    if not chunk then
        -- Try without return
        chunk, err = load(str, "=unserialize", "t", {
            math = { huge = math.huge },
        })
    end
    
    if not chunk then
        return nil, "deserialization failed: " .. tostring(err)
    end
    
    local ok, result = pcall(chunk)
    if not ok then
        return nil, "deserialization error: " .. tostring(result)
    end
    
    return result
end

-- Alias
serialization.parse = serialization.unserialize

-- Serialize to a compact single-line format
function serialization.tostring(value)
    return serialization.serialize(value, { pretty = false })
end

-- Serialize to a pretty-printed multi-line format
function serialization.pretty(value)
    return serialization.serialize(value, { pretty = true, indent = 0 })
end

-- Check if a string is safe to unserialize (basic check)
function serialization.isSafe(str)
    -- Check for dangerous patterns
    if str:find("function") or str:find("pcall") or str:find("error")
       or str:find("load") or str:find("require") or str:find("os")
       or str:find("io") or str:find("dofile") or str:find("rawget")
       or str:find("rawset") or str:find("setmetatable") then
        return false
    end
    return true
end

-- Safe unserialize — checks before deserializing
function serialization.safeUnserialize(str)
    if not serialization.isSafe(str) then
        return nil, "potentially unsafe string rejected"
    end
    return serialization.unserialize(str)
end

return serialization
