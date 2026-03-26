-- reboot.lua - Reboot the computer
local args = { ... }

if args[1] == "-h" or args[1] == "--help" then
    print("Usage: reboot - Reboot the computer")
    print("       reboot -p - Power off (shutdown)")
    return
end

if args[1] == "-p" then
    print("Shutting down...")
    os.sleep(0.5)
    computer.shutdown(false)
else
    print("Rebooting...")
    os.sleep(0.5)
    computer.shutdown(true)
end
