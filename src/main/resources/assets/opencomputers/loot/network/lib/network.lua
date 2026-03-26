-- Network Library
-- Provides high-level networking functions

local network = {}

local component = component
local event = event or {}

-- ========================================
-- Modem Access
-- ========================================

local function getModem()
  local addr = component.list("modem")()
  if addr then
    return component.proxy(addr)
  end
  return nil
end

-- ========================================
-- Basic Functions
-- ========================================

function network.open(port)
  local modem = getModem()
  if modem then
    return modem.open(port)
  end
  return false
end

function network.close(port)
  local modem = getModem()
  if modem then
    if port then
      return modem.close(port)
    else
      return modem.close()
    end
  end
  return false
end

function network.send(address, port, ...)
  local modem = getModem()
  if modem then
    return modem.send(address, port, ...)
  end
  return false
end

function network.broadcast(port, ...)
  local modem = getModem()
  if modem then
    return modem.broadcast(port, ...)
  end
  return false
end

function network.isOpen(port)
  local modem = getModem()
  if modem then
    return modem.isOpen(port)
  end
  return false
end

function network.getStrength()
  local modem = getModem()
  if modem and modem.getStrength then
    return modem.getStrength()
  end
  return 0
end

function network.setStrength(value)
  local modem = getModem()
  if modem and modem.setStrength then
    return modem.setStrength(value)
  end
  return false
end

function network.isWireless()
  local modem = getModem()
  if modem and modem.isWireless then
    return modem.isWireless()
  end
  return false
end

-- ========================================
-- High-Level Functions
-- ========================================

-- Receive a message with timeout
function network.receive(timeout)
  local deadline = computer.uptime() + (timeout or math.huge)
  
  while computer.uptime() < deadline do
    local signal = {computer.pullSignal(deadline - computer.uptime())}
    if signal[1] == "modem_message" then
      return table.unpack(signal, 2)
    end
  end
  
  return nil
end

-- Send and wait for response
function network.request(address, port, timeout, ...)
  network.send(address, port, ...)
  return network.receive(timeout)
end

-- Create a simple server on a port
function network.serve(port, handler)
  network.open(port)
  
  while true do
    local data = {network.receive()}
    if data[1] then
      local localAddr, remoteAddr, remotePort, distance = data[1], data[2], data[3], data[4]
      local message = {table.unpack(data, 5)}
      
      local response = handler(remoteAddr, message)
      if response then
        network.send(remoteAddr, remotePort, table.unpack(response))
      end
    end
  end
end

return network
