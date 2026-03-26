-- lib/uuid.lua - RFC 4122 v4 UUID generation

local uuid = {}

-- Generate a random UUID (version 4, variant 1)
function uuid.next()
  local template = "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx"
  return string.gsub(template, "[xy]", function(c)
    local v = c == "x" and math.random(0, 0xf) or math.random(8, 0xb)
    return string.format("%x", v)
  end)
end

-- Check if a string is a valid UUID format
function uuid.isValid(s)
  if type(s) ~= "string" then return false end
  return s:match("^%x%x%x%x%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%-%x%x%x%x%x%x%x%x%x%x%x%x$") ~= nil
end

-- Strip hyphens from UUID string
function uuid.toBytes(s)
  return s:gsub("-", "")
end

-- Format a 32-hex-char string as a UUID
function uuid.fromBytes(s)
  s = s:gsub("-", "")
  if #s ~= 32 then return nil end
  return string.format("%s-%s-%s-%s-%s",
    s:sub(1, 8), s:sub(9, 12), s:sub(13, 16),
    s:sub(17, 20), s:sub(21, 32))
end

-- Generate a name-based UUID v5 (SHA-1 based, simplified)
-- Note: uses simple hash, not full SHA-1 for Lua 5.4 compat
function uuid.v5(namespace, name)
  local hash = 0
  local ns = namespace:gsub("-", "")
  local combined = ns .. name
  for i = 1, #combined do
    hash = (hash * 31 + combined:byte(i)) % 0x100000000
  end
  math.randomseed(hash)
  local result = uuid.next()
  -- Set version to 5
  result = result:sub(1, 14) .. "5" .. result:sub(16)
  return result
end

return uuid
