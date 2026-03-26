-- bin/man.lua - Manual page viewer

local shell = require("shell")
local term  = require("term")
local text  = require("text")

local args = shell.parse(...)

if #args == 0 then
  io.write("Usage: man <command>\n")
  os.exit(1)
end

local topic = args[1]

-- Built-in man pages
local pages = {
  sh      = "sh - OpenOS shell\n\nUsage: sh [script] [args...]\n\nThe default command interpreter. Supports:\n  - Piping: cmd1 | cmd2\n  - Redirection: cmd > file, cmd >> file, cmd < file\n  - Background: cmd &\n  - Variables: $VAR, ${VAR}\n  - Semicolons: cmd1; cmd2\n",
  ls      = "ls - List directory contents\n\nUsage: ls [-la] [dir]\n\nOptions:\n  -l   Long listing format\n  -a   Show hidden files (starting with .)\n  -h   Human-readable sizes\n",
  cat     = "cat - Concatenate and print files\n\nUsage: cat [file...]\n\nPrints contents of each file to stdout.\nWith no arguments, reads from stdin.\n",
  cp      = "cp - Copy files\n\nUsage: cp <src> <dst>\n       cp <src...> <dir>\n",
  mv      = "mv - Move/rename files\n\nUsage: mv <src> <dst>\n",
  rm      = "rm - Remove files\n\nUsage: rm [-r] <file...>\n\nOptions:\n  -r   Recursive (remove directories)\n",
  mkdir   = "mkdir - Create directories\n\nUsage: mkdir <dir...>\n",
  grep    = "grep - Search for patterns\n\nUsage: grep [-niv] <pattern> [file...]\n\nOptions:\n  -n   Show line numbers\n  -i   Case-insensitive\n  -v   Invert match\n",
  find    = "find - Search for files\n\nUsage: find <dir> [-name pat] [-type f|d] [-size n]\n\nOptions:\n  -name   Match filename pattern (supports * wildcard)\n  -type   f=file, d=directory\n  -size   File size in bytes (prefix + for greater, - for less)\n",
  nano    = "nano - Text editor\n\nUsage: nano [file]\n\nControls:\n  Ctrl+S   Save\n  Ctrl+Q   Quit\n  Ctrl+F   Find\n  Ctrl+H   Find & Replace\n  Ctrl+G   Go to line\n  Ctrl+K   Delete line\n  Ctrl+C   Copy line\n  Ctrl+V   Paste\n",
  ping    = "ping - Test network connectivity\n\nUsage: ping <address> [-c count] [-t timeout]\n\nOptions:\n  -c   Number of pings (default 4)\n  -t   Timeout per ping in seconds (default 2)\n  -a   Broadcast ping to all nodes\n",
  wget    = "wget - Download files from the internet\n\nUsage: wget <url> [filename]\n\nRequires an internet card.\n",
  pastebin = "pastebin - Pastebin.com client\n\nUsage: pastebin get <id> [file]\n       pastebin put <file>\n       pastebin run <id> [args]\n\nRequires an internet card.\n",
  lua     = "lua - Interactive Lua interpreter\n\nUsage: lua [file] [args]\n\nWith no arguments, starts an interactive REPL.\n= expr prints the value of expr.\n",
  ps      = "ps - List running processes\n\nUsage: ps\n\nShows all running processes with their PID and status.\n",
  top     = "top - Resource monitor\n\nUsage: top\n\nShows live CPU and memory usage. Press Q to quit.\n",
  df      = "df - Disk usage\n\nUsage: df [-h] [path]\n\nOptions:\n  -h   Human-readable sizes\n",
  more    = "more - Page through text\n\nUsage: more [file]\n\nSpace/Enter to advance, Q to quit.\n",
  wc      = "wc - Word, line, byte count\n\nUsage: wc [-lwc] [file...]\n\nOptions:\n  -l   Count lines\n  -w   Count words\n  -c   Count bytes\n",
  sort    = "sort - Sort lines of text\n\nUsage: sort [-rnu] [file]\n\nOptions:\n  -r   Reverse order\n  -n   Numeric sort\n  -u   Unique lines only\n",
  tee     = "tee - Read stdin, write to file AND stdout\n\nUsage: tee [-a] <file>\n\nOptions:\n  -a   Append to file instead of overwrite\n",
  date    = "date - Show current date/time\n\nUsage: date [+format]\n\nShows uptime formatted as date.\nFormat specifiers: %H %M %S %d %m %Y %j\n",
  diff    = "diff - Compare two files\n\nUsage: diff <file1> <file2>\n\nOutputs differences in unified diff format.\nLines prefixed with + exist only in file2.\nLines prefixed with - exist only in file1.\n",
  alias   = "alias - Define or list command aliases\n\nUsage: alias [name[=value]]\n       alias -d <name>\n\nWith no arguments, lists all aliases.\n",
  export  = "export - Set environment variables\n\nUsage: export [NAME=value...]\n       export -n <NAME>\n\nWith no arguments, lists all exported variables.\n",
  which   = "which - Locate commands\n\nUsage: which [-a] <command>\n\nOptions:\n  -a   Show all matches, not just the first\n",
  cron    = "cron - Schedule periodic tasks\n\nUsage: cron list\n       cron add <interval> <command>\n       cron remove <id>\n       cron start|stop\n\nInterval in seconds.\n",
  chat    = "chat - Network chat client\n\nUsage: chat [channel]\n\nCommands:\n  /nick <name>   Change nickname\n  /who           List online users\n  /ping <addr>   Ping a computer\n  /clear         Clear screen\n  /quit          Exit chat\n",
  robot   = "robot - Robot control library\n\nUsage: require(\"robot\")\n\nFunctions: forward, back, up, down, turnLeft, turnRight,\ndetect, swing, place, drop, suck, select, craft, etc.\n",
  tunnel  = "tunnel - Robot tunnel miner\n\nUsage: tunnel <length> [height] [width]\n\nMines a tunnel of given dimensions.\n",
  wall    = "wall - Robot wall builder\n\nUsage: wall <width> <height>\n\nBuilds a wall using blocks from inventory.\n",
  quarry  = "quarry - Robot quarry program\n\nUsage: quarry <width> <depth> [height]\n\nMines a rectangular area, deposits items to adjacent chest.\n",
  harvest = "harvest - Robot crop harvester\n\nUsage: harvest <width> <depth>\n\nHarvests crops in a rectangular field and replants seeds.\n",
  treefarm = "treefarm - Robot tree farm\n\nUsage: treefarm <cols> <rows> [spacing]\n\nChops trees in a grid pattern and replants saplings.\n",
  install = "install - Install OpenOS to hard drive\n\nUsage: install\n\nGuides you through installing OpenOS from a floppy disk\nto a hard drive and configures the EEPROM to boot from it.\n",
  monitor = "monitor - System resource monitor\n\nUsage: monitor\n\nDisplays memory, energy, disk usage and process list.\nPress Q or Ctrl+C to quit.\n",
  filesync = "filesync - Sync files over modem\n\nUsage: filesync server\n       filesync push <target_addr> <local> [remote]\n       filesync pull <target_addr> <remote> [local]\n",
  net     = "net - Network configuration\n\nUsage: net status\n       net list\n       net open <port>\n       net close <port>\n       net send <addr> <port> <msg>\n",
  settings = "settings - Configuration manager\n\nUsage: settings [get <key>] [set <key> <val>] [delete <key>] [list]\n",
  uuid    = "uuid - UUID generation library\n\nUsage: require(\"uuid\")\n\nFunctions: uuid.next(), uuid.isValid(s), uuid.toBytes(s), uuid.fromBytes(s)\n",
  zip     = "zip - Create compressed archives\n\nUsage: zip <archive.zip> <file...>\n       zip -l <archive.zip>   (list contents)\n",
  unzip   = "unzip - Extract compressed archives\n\nUsage: unzip <archive.zip> [destdir]\n",
  diff    = "diff - Compare two files line by line\n\nUsage: diff <file1> <file2>\n\nLines with - are only in file1.\nLines with + are only in file2.\n",
  edit    = "edit - Simple line editor\n\nUsage: edit <file>\n\nBasic text editor. Use 'nano' for a full-screen editor.\n",
  help    = "help - Show help for commands\n\nUsage: help [command]\n\nWith no arguments, lists available commands.\nSame as 'man' but shorter.\n",
}

local page = pages[topic]
if not page then
  -- Try to find a help file
  local helpFile = "/usr/share/man/" .. topic .. ".txt"
  if require("filesystem").exists(helpFile) then
    local f = io.open(helpFile, "r")
    if f then
      page = f:read("*a")
      f:close()
    end
  end
end

if not page then
  io.write("man: no manual entry for '" .. topic .. "'\n")
  io.write("Try 'help' for a list of commands.\n")
  os.exit(1)
end

-- Display with pager
local w, h = term.getViewport and term.getViewport() or (80, 24)
local lines = text.wrap(page, w - 2)
local lineNum = 0

io.write("\n")
for _, line in ipairs(lines) do
  io.write(line .. "\n")
  lineNum = lineNum + 1
  if lineNum >= h - 2 then
    io.write("-- More -- (Enter/Space to continue, Q to quit) ")
    local key = term.read and term.read() or "\n"
    if key and (key:lower() == "q" or key:lower() == "q\n") then
      io.write("\n")
      break
    end
    io.write("\r" .. string.rep(" ", 50) .. "\r")
    lineNum = 0
  end
end
io.write("\n")
