-- help.lua - Display help information
local term = require("term")
local text = require("text")
local fs = require("filesystem")
local shell = require("shell")

local args = { ... }
local topic = args[1]

local w = select(1, term.getSize())

-- Built-in help entries
local builtinHelp = {
    overview = [[
OpenOS - OpenComputers Operating System

OpenOS is a Lua-based operating system for OpenComputers mod computers.
It provides a Unix-like environment with a shell, filesystem, networking,
and Lua scripting capabilities.

Core commands:
  ls      - list directory contents
  cd      - change directory  
  cat     - display file contents
  edit    - text editor
  lua     - Lua interpreter/REPL
  cp      - copy files
  mv      - move/rename files
  rm      - remove files
  mkdir   - create directories
  pwd     - print working directory
  echo    - print text
  help    - show this help

Type 'help <topic>' for more information on a specific topic.
Topics: filesystem, networking, programming, components
]],

    filesystem = [[
Filesystem Help
===============
The filesystem is organized as a Unix-like hierarchy:
  /       - root directory
  /bin    - executable programs
  /etc    - configuration files
  /home   - user home directories
  /lib    - shared libraries
  /tmp    - temporary files (cleared on reboot)
  /usr    - user programs and data

Paths can be absolute (starting with /) or relative to the current
working directory. Use 'pwd' to see the current directory and 'cd' to
change it.

File operations:
  fs.readAll(path)        - read entire file
  fs.writeAll(path, data) - write entire file
  fs.list(path)           - list directory
  fs.exists(path)         - check if file exists
  fs.isDirectory(path)    - check if path is directory
  fs.makeDirectory(path)  - create directory
  fs.remove(path)         - delete file/dir
  fs.copy(from, to)       - copy file
  fs.rename(from, to)     - rename/move file
]],

    networking = [[
Networking Help
===============
Computers can communicate via network cards and cables.

Component: modem
  modem.open(port)           - open port for listening
  modem.close(port)          - close port
  modem.send(addr, port, ...) - send message to address
  modem.broadcast(port, ...) - broadcast to all
  modem.isOpen(port)         - check if port is open

Events:
  modem_message: address, port, senderAddress, senderPort, distance, ...data

Example - simple server:
  local modem = component.getPrimary("modem")
  component.invoke(modem, "open", 80)
  while true do
    local sig, _, from, port, _, data = computer.pullSignal()
    if sig == "modem_message" then
      print("From: " .. from .. ": " .. data)
    end
  end
]],

    programming = [[
Programming Help
================
OpenOS programs are Lua 5.4 scripts. Key APIs:

component.*
  component.list(type)         - list component addresses
  component.invoke(addr, m, ..) - call component method
  component.getPrimary(type)   - get primary component address
  component.proxy(addr)        - get proxy object

computer.*
  computer.address()           - get computer address
  computer.uptime()            - time since boot
  computer.energy()            - current energy
  computer.shutdown([reboot])  - shutdown/reboot

io / filesystem:
  require("filesystem")        - fs library
  require("serialization")     - serialize/deserialize data
  require("text")              - text utilities
  require("event")             - event system
  require("term")              - terminal control

Writing programs:
  Edit a file in /bin/ or /home/ with the edit command
  Require libraries with require("libname")
  Run with 'lua myprogram.lua' or just 'myprogram'
]],

    components = [[
Components Help
===============
Components are hardware devices attached to your computer.

Tier 1 computer:
  - CPU T1: 2 component slots
  - RAM T1: 192 KB
  - GPU T1: 80x25 text resolution

Tier 2 computer:
  - CPU T2: 3 component slots
  - RAM T2: 512 KB
  - GPU T2: 160x50 resolution

Tier 3 computer:
  - CPU T3: 4 component slots
  - RAM T3: 2 MB
  - GPU T3: 320x200 resolution

Common components:
  gpu         - graphics processing unit
  screen      - display screen
  filesystem  - disk drive or floppy
  modem       - network card
  keyboard    - keyboard input
  drone       - autonomous drone unit
  robot       - programmable robot
  geolyzer    - geological scanner
  redstone    - redstone interface
  motion_sensor - motion detection
  internet    - HTTP/TCP access
]]
}

if not topic then
    -- Show general help
    term.setForeground(0x00AAFF)
    print(text.separator(w, "="))
    print(text.center("OpenOS Help", w))
    print(text.separator(w, "="))
    term.setForeground(0xFFFFFF)
    print(builtinHelp.overview)
    return
end

topic = topic:lower()

if builtinHelp[topic] then
    term.setForeground(0x00AAFF)
    print(builtinHelp[topic])
    term.setForeground(0xFFFFFF)
    return
end

-- Check for a help file in /usr/help/
local helpPaths = {
    "/usr/help/" .. topic .. ".txt",
    "/usr/help/" .. topic .. ".md",
    "/usr/help/" .. topic,
}

for _, hpath in ipairs(helpPaths) do
    if fs.exists(hpath) then
        local data = fs.readAll(hpath)
        if data then
            print(data)
            return
        end
    end
end

-- Try to get help from the command's --help flag
local cmdPath = shell.which(topic)
if cmdPath then
    shell.execute(topic, "--help")
    return
end

io.stderr:write("help: no documentation for '" .. topic .. "'\n")
