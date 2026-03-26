-- lua command: interactive Lua prompt

print("Lua 5.3 Interactive Mode")
print("Type 'exit' to return to shell")
print("")

while true do
  component.invoke(component.list("gpu")(), "setForeground", 0xFFFF00)
  term.write("lua> ")
  component.invoke(component.list("gpu")(), "setForeground", 0xFFFFFF)
  
  local input = term.read()
  
  if input == "exit" or input == "quit" then
    break
  end
  
  -- Try to evaluate as expression first
  local func, err = load("return " .. input, "=stdin", "t", _G)
  if not func then
    -- Try as statement
    func, err = load(input, "=stdin", "t", _G)
  end
  
  if func then
    local results = {pcall(func)}
    local ok = table.remove(results, 1)
    
    if ok then
      if #results > 0 then
        local output = {}
        for i, v in ipairs(results) do
          table.insert(output, tostring(v))
        end
        component.invoke(component.list("gpu")(), "setForeground", 0x00FF00)
        print(table.concat(output, "\t"))
        component.invoke(component.list("gpu")(), "setForeground", 0xFFFFFF)
      end
    else
      component.invoke(component.list("gpu")(), "setForeground", 0xFF0000)
      print("Error: " .. tostring(results[1]))
      component.invoke(component.list("gpu")(), "setForeground", 0xFFFFFF)
    end
  else
    component.invoke(component.list("gpu")(), "setForeground", 0xFF0000)
    print("Syntax error: " .. tostring(err))
    component.invoke(component.list("gpu")(), "setForeground", 0xFFFFFF)
  end
end
