-- components command: list hardware components

print("Installed Components:")
print("")

for address, componentType in component.list() do
  local short = address:sub(1, 8) .. "..."
  print("  " .. componentType .. " [" .. short .. "]")
end
