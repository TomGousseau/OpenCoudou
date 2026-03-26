-- Data Library
-- Provides data processing utilities using the data card

local data = {}

local component = component

-- ========================================
-- Data Card Access
-- ========================================

local function getDataCard()
  local addr = component.list("data")()
  if addr then
    return component.proxy(addr)
  end
  return nil
end

local function getTier()
  local card = getDataCard()
  if card then
    if card.encrypt then return 3
    elseif card.generateKeyPair then return 2
    else return 1
    end
  end
  return 0
end

-- ========================================
-- Hashing (Tier 1+)
-- ========================================

function data.crc32(str)
  local card = getDataCard()
  if card then
    return card.crc32(str)
  end
  error("No data card available")
end

function data.md5(str)
  local card = getDataCard()
  if card then
    return card.md5(str)
  end
  error("No data card available")
end

function data.sha256(str)
  local card = getDataCard()
  if card then
    return card.sha256(str)
  end
  error("No data card available")
end

-- ========================================
-- Compression (Tier 1+)
-- ========================================

function data.deflate(str)
  local card = getDataCard()
  if card then
    return card.deflate(str)
  end
  error("No data card available")
end

function data.inflate(str)
  local card = getDataCard()
  if card then
    return card.inflate(str)
  end
  error("No data card available")
end

-- ========================================
-- Base64 (Tier 1+)
-- ========================================

function data.encode64(str)
  local card = getDataCard()
  if card then
    return card.encode64(str)
  end
  error("No data card available")
end

function data.decode64(str)
  local card = getDataCard()
  if card then
    return card.decode64(str)
  end
  error("No data card available")
end

-- ========================================
-- Random (Tier 1+)
-- ========================================

function data.random(count)
  local card = getDataCard()
  if card then
    return card.random(count)
  end
  error("No data card available")
end

-- ========================================
-- Encryption (Tier 2+)
-- ========================================

function data.generateKeyPair(bits)
  if getTier() < 2 then
    error("Tier 2 data card required")
  end
  local card = getDataCard()
  return card.generateKeyPair(bits or 384)
end

function data.ecdh(privateKey, publicKey)
  if getTier() < 2 then
    error("Tier 2 data card required")
  end
  local card = getDataCard()
  return card.ecdh(privateKey, publicKey)
end

function data.ecdsa(data, privateKey, publicKey)
  if getTier() < 2 then
    error("Tier 2 data card required")
  end
  local card = getDataCard()
  if publicKey then
    -- Verify
    return card.ecdsa(data, privateKey, publicKey)
  else
    -- Sign
    return card.ecdsa(data, privateKey)
  end
end

-- ========================================
-- AES Encryption (Tier 3)
-- ========================================

function data.encrypt(data, key, iv)
  if getTier() < 3 then
    error("Tier 3 data card required")
  end
  local card = getDataCard()
  return card.encrypt(data, key, iv)
end

function data.decrypt(data, key, iv)
  if getTier() < 3 then
    error("Tier 3 data card required")
  end
  local card = getDataCard()
  return card.decrypt(data, key, iv)
end

-- ========================================
-- High-Level Functions
-- ========================================

-- Hash a file
function data.hashFile(path, algorithm)
  algorithm = algorithm or "sha256"
  
  local bootFs = computer.getBootAddress()
  local handle = component.invoke(bootFs, "open", path, "r")
  if not handle then
    error("Cannot open file: " .. path)
  end
  
  local content = ""
  repeat
    local chunk = component.invoke(bootFs, "read", handle, 4096)
    if chunk then
      content = content .. chunk
    end
  until not chunk
  component.invoke(bootFs, "close", handle)
  
  if algorithm == "crc32" then
    return data.crc32(content)
  elseif algorithm == "md5" then
    return data.md5(content)
  else
    return data.sha256(content)
  end
end

-- Compress and encode for transmission
function data.pack(str)
  return data.encode64(data.deflate(str))
end

-- Decode and decompress received data
function data.unpack(str)
  return data.inflate(data.decode64(str))
end

-- Get tier of installed data card
function data.tier()
  return getTier()
end

return data
