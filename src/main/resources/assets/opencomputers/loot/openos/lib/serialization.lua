-- Serialization Library
-- Provides serialization and deserialization of Lua values

local serialization = {}

-- ========================================
-- Serialization
-- ========================================

function serialization.serialize(value, pretty)
  local seen = {}
  local indent = 0
  
  local function spaces()
    if pretty then
      return string.rep("  ", indent)
    end
    return ""
  end
  
  local function newline()
    if pretty then
      return "\n"
    end
    return ""
  end
  
  local function serialize(val)
    local t = type(val)
    
    if t == "nil" then
      return "nil"
    elseif t == "boolean" then
      return val and "true" or "false"
    elseif t == "number" then
      if val ~= val then
        return "0/0" -- NaN
      elseif val == math.huge then
        return "math.huge"
      elseif val == -math.huge then
        return "-math.huge"
      else
        return tostring(val)
      end
    elseif t == "string" then
      return string.format("%q", val)
    elseif t == "table" then
      if seen[val] then
        error("circular reference")
      end
      seen[val] = true
      
      local result = "{"
      indent = indent + 1
      
      local isArray = true
      local maxIndex = 0
      for k, v in pairs(val) do
        if type(k) ~= "number" or k < 1 or k ~= math.floor(k) then
          isArray = false
          break
        end
        maxIndex = math.max(maxIndex, k)
      end
      
      -- Check for holes
      if isArray then
        for i = 1, maxIndex do
          if val[i] == nil then
            isArray = false
            break
          end
        end
      end
      
      if isArray and maxIndex > 0 then
        for i = 1, maxIndex do
          result = result .. newline() .. spaces() .. serialize(val[i])
          if i < maxIndex then
            result = result .. ","
          end
        end
      else
        local first = true
        for k, v in pairs(val) do
          if not first then
            result = result .. ","
          end
          first = false
          
          result = result .. newline() .. spaces()
          
          if type(k) == "string" and k:match("^[%a_][%w_]*$") then
            result = result .. k .. "="
          else
            result = result .. "[" .. serialize(k) .. "]="
          end
          
          result = result .. serialize(v)
        end
      end
      
      indent = indent - 1
      
      if not first then
        result = result .. newline() .. spaces()
      end
      result = result .. "}"
      
      seen[val] = nil
      return result
    else
      error("cannot serialize type: " .. t)
    end
  end
  
  return serialize(value)
end

-- ========================================
-- Deserialization
-- ========================================

function serialization.unserialize(str)
  local result, err = load("return " .. str, "=unserialize", "t", {
    math = { huge = math.huge }
  })
  
  if not result then
    return nil, err
  end
  
  local ok, value = pcall(result)
  if not ok then
    return nil, value
  end
  
  return value
end

-- ========================================
-- Convenience Functions
-- ========================================

-- Save table to file
function serialization.save(path, value, pretty)
  local str = serialization.serialize(value, pretty)
  
  local bootFs = computer.getBootAddress()
  local handle = component.invoke(bootFs, "open", path, "w")
  if not handle then
    return false, "cannot open file"
  end
  
  component.invoke(bootFs, "write", handle, str)
  component.invoke(bootFs, "close", handle)
  return true
end

-- Load table from file
function serialization.load(path)
  local bootFs = computer.getBootAddress()
  local handle = component.invoke(bootFs, "open", path, "r")
  if not handle then
    return nil, "cannot open file"
  end
  
  local str = ""
  repeat
    local chunk = component.invoke(bootFs, "read", handle, 4096)
    if chunk then
      str = str .. chunk
    end
  until not chunk
  component.invoke(bootFs, "close", handle)
  
  return serialization.unserialize(str)
end

return serialization
