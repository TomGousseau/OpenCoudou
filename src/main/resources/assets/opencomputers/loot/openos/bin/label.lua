-- label command: get/set filesystem labels

local args = {...}
local action = args[1]
local arg2 = args[2]
local arg3 = args[3]

local function usage()
  print("Usage:")
  print("  label          - List all labeled filesystems")
  print("  label get <fs> - Get filesystem label")
  print("  label set <fs> <name> - Set filesystem label")
end

if not action then
  -- List all
  print("Filesystem Labels:")
  for address in component.list("filesystem") do
    local label = component.invoke(address, "getLabel") or "(none)"
    print("  " .. address:sub(1, 8) .. "... -> " .. label)
  end
  return
end

if action == "get" then
  local addr = arg2
  if not addr then
    usage()
    return
  end
  
  -- Find matching filesystem
  for address in component.list("filesystem") do
    if address:sub(1, #addr) == addr then
      local label = component.invoke(address, "getLabel") or "(none)"
      print("Label: " .. label)
      return
    end
  end
  print("Filesystem not found")
  
elseif action == "set" then
  local addr = arg2
  local newLabel = arg3
  if not addr or not newLabel then
    usage()
    return
  end
  
  -- Find matching filesystem
  for address in component.list("filesystem") do
    if address:sub(1, #addr) == addr then
      component.invoke(address, "setLabel", newLabel)
      print("Label set to: " .. newLabel)
      return
    end
  end
  print("Filesystem not found")
  
else
  usage()
end
