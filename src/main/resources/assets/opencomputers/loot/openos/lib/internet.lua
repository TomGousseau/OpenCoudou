-- Internet Library
-- Provides HTTP and TCP networking

local internet = {}

local component = component

-- ========================================
-- Internet Card Access
-- ========================================

local function getInternet()
  local addr = component.list("internet")()
  if addr then
    return component.proxy(addr)
  end
  return nil
end

-- ========================================
-- HTTP Functions
-- ========================================

function internet.request(url, postData, headers)
  local inet = getInternet()
  if not inet then
    return nil, "no internet card"
  end
  
  if not inet.isHttpEnabled() then
    return nil, "HTTP is disabled"
  end
  
  local handle, err = inet.request(url, postData, headers)
  if not handle then
    return nil, err
  end
  
  -- Return response object
  return {
    read = function(self, n)
      local data, err = handle.read(n or math.huge)
      return data, err
    end,
    
    response = function(self)
      return handle.response()
    end,
    
    close = function(self)
      handle.close()
    end,
    
    finishConnect = function(self)
      while true do
        local status, err = handle.finishConnect()
        if status then
          return true
        elseif status == false then
          return false, err
        end
        -- status is nil, connection in progress
        os.sleep(0.05)
      end
    end
  }
end

-- Simple GET request
function internet.get(url, headers)
  return internet.request(url, nil, headers)
end

-- Simple POST request
function internet.post(url, data, headers)
  return internet.request(url, data, headers)
end

-- Fetch entire content
function internet.fetch(url, headers)
  local response, err = internet.get(url, headers)
  if not response then
    return nil, err
  end
  
  -- Wait for connection
  local ok, err = response:finishConnect()
  if not ok then
    response:close()
    return nil, err
  end
  
  -- Read all content
  local content = ""
  while true do
    local chunk = response:read(4096)
    if not chunk then
      break
    end
    content = content .. chunk
  end
  
  local code = response:response()
  response:close()
  
  return content, code
end

-- ========================================
-- TCP Functions
-- ========================================

function internet.socket(address, port)
  local inet = getInternet()
  if not inet then
    return nil, "no internet card"
  end
  
  if not inet.isTcpEnabled() then
    return nil, "TCP is disabled"
  end
  
  local handle, err = inet.connect(address, port)
  if not handle then
    return nil, err
  end
  
  -- Return socket object
  return {
    read = function(self, n)
      return handle.read(n or 1024)
    end,
    
    write = function(self, data)
      return handle.write(data)
    end,
    
    close = function(self)
      handle.close()
    end,
    
    finishConnect = function(self)
      while true do
        local status, err = handle.finishConnect()
        if status then
          return true
        elseif status == false then
          return false, err
        end
        os.sleep(0.05)
      end
    end,
    
    id = function(self)
      return handle.id()
    end
  }
end

-- ========================================
-- Utility Functions
-- ========================================

function internet.isAvailable()
  return getInternet() ~= nil
end

function internet.isHttpEnabled()
  local inet = getInternet()
  return inet and inet.isHttpEnabled()
end

function internet.isTcpEnabled()
  local inet = getInternet()
  return inet and inet.isTcpEnabled()
end

-- URL encode string
function internet.encode(str)
  str = str:gsub("\n", "\r\n")
  str = str:gsub("([^%w _%%%-%.~])", function(c)
    return string.format("%%%02X", string.byte(c))
  end)
  str = str:gsub(" ", "+")
  return str
end

-- URL decode string
function internet.decode(str)
  str = str:gsub("+", " ")
  str = str:gsub("%%(%x%x)", function(h)
    return string.char(tonumber(h, 16))
  end)
  return str
end

return internet
