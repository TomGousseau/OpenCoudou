-- chat.lua - Network chat program using modem component
-- Usage: chat [channel] [nick]
-- Default channel: 123, default nick: computer address prefix

local term   = require("term")
local shell  = require("shell")
local event  = require("event")
local text   = require("text")
local colors = require("colors")

local args = { ... }
local CHANNEL = tonumber(args[1]) or 123
local NICK    = args[2] or ("oc-" .. computer.address():sub(1, 8))
local PORT    = CHANNEL

-- Protocol constants
local MSG_CHAT   = "chat"
local MSG_JOIN   = "join"
local MSG_LEAVE  = "leave"
local MSG_PING   = "ping"
local MSG_PONG   = "pong"

local function findModem()
    for addr, _ in component.list("modem") do
        return addr
    end
    return nil
end

local modemAddr = findModem()
if not modemAddr then
    io.stderr:write("chat: no modem found\n")
    return 1
end

-- Open the channel
component.invoke(modemAddr, "open", PORT)

-- Track online users
local users = { [NICK] = true }

-- Display state
local w, h = term.getSize()
local chatH  = h - 3   -- lines for chat history
local inputY = h - 1   -- input line
local statusY= h        -- status bar
local history = {}       -- { { nick, msg, time } }
local MAX_HISTORY = 200

-- Serialize message
local serialization = require("serialization")

local function send(msgType, payload)
    local pkt = serialization.serialize({ type = msgType, nick = NICK, data = payload })
    component.invoke(modemAddr, "broadcast", PORT, pkt)
end

local function addLine(line, fg)
    table.insert(history, { text = line, fg = fg or colors.white })
    if #history > MAX_HISTORY then
        table.remove(history, 1)
    end
end

local function redrawChat()
    -- Draw border between chat and input
    term.setCursor(1, chatH + 1)
    term.setForeground(colors.gray)
    term.setBackground(0x222222)
    io.write(string.rep("─", w))
    term.setBackground(0x000000)
    
    -- Draw last chatH lines of history
    local start = math.max(1, #history - chatH + 1)
    for row = 1, chatH do
        term.setCursor(1, row)
        local entry = history[start + row - 1]
        if entry then
            term.setForeground(entry.fg)
            -- Truncate to fit width
            local line = entry.text
            if #line > w then line = line:sub(1, w - 1) end
            io.write(line .. string.rep(" ", w - #line))
        else
            io.write(string.rep(" ", w))
        end
    end
end

local function redrawStatus(inputBuf)
    term.setCursor(1, statusY)
    term.setForeground(colors.black)
    term.setBackground(0x44AAFF)
    local ulist = {}
    for u in pairs(users) do table.insert(ulist, u) end
    table.sort(ulist)
    local status = string.format(" [#%d] %s | Online: %s", CHANNEL, NICK, table.concat(ulist, ", "))
    if #status > w then status = status:sub(1, w) end
    io.write(status .. string.rep(" ", w - #status))
    term.setBackground(0x000000)
    term.setForeground(colors.white)
    
    -- Redraw input prompt
    term.setCursor(1, inputY)
    local prompt = "> " .. (inputBuf or "")
    if #prompt > w then prompt = prompt:sub(#prompt - w + 1) end
    io.write(prompt .. " ")
    term.setCursor(math.min(#prompt + 1, w), inputY)
end

-- Clear screen and draw initial UI
term.clear()
term.setBackground(0x000000)

local now = math.floor(computer.uptime())
addLine(string.format("[ Connected to channel #%d as %s ]", CHANNEL, NICK), colors.cyan)
addLine("[ Type /quit or Ctrl+C to leave. /help for commands. ]", colors.gray)
addLine("", 0)

redrawChat()

-- Announce join
send(MSG_JOIN, { time = now })
users[NICK] = true

redrawStatus("")

-- -------------------------------------------------------------------------
-- Input loop
-- -------------------------------------------------------------------------
local inputBuf = ""
local inputCur = 0  -- cursor offset from start (0 = at end)
local running  = true
local inputHistory = {}
local inputHistPos = 0

local function handleMessage(srcAddr, srcPort, srcDist, msg)
    if srcPort ~= PORT then return end
    local pkt = serialization.unserialize(msg)
    if type(pkt) ~= "table" then return end
    
    local nt = pkt.type
    local nick = pkt.nick or "?"
    local data = pkt.data or {}
    local ts   = string.format("[%02d:%02d]", math.floor(computer.uptime() / 60) % 60, math.floor(computer.uptime()) % 60)
    
    if nt == MSG_CHAT then
        local line = ts .. " <" .. nick .. "> " .. tostring(data.text or "")
        local fg = (nick == NICK) and colors.yellow or colors.white
        addLine(line, fg)
    elseif nt == MSG_JOIN then
        users[nick] = true
        addLine(ts .. " * " .. nick .. " joined the channel", colors.green)
    elseif nt == MSG_LEAVE then
        users[nick] = nil
        addLine(ts .. " * " .. nick .. " left the channel", colors.red)
    elseif nt == MSG_PING then
        -- Respond to ping
        send(MSG_PONG, {})
    elseif nt == MSG_PONG then
        users[nick] = true
    end
    
    redrawChat()
    redrawStatus(inputBuf)
end

local function handleCommand(cmd)
    cmd = cmd:gsub("^%s+", ""):gsub("%s+$", "")
    
    if cmd == "/quit" or cmd == "/exit" or cmd == "/q" then
        running = false
    elseif cmd == "/help" then
        addLine("Commands: /quit /nick <name> /who /ping /clear", colors.cyan)
    elseif cmd:match("^/nick%s+") then
        local newNick = cmd:match("^/nick%s+(.+)$")
        if newNick and #newNick > 0 and #newNick <= 16 then
            send(MSG_LEAVE, {})
            users[NICK] = nil
            NICK = newNick
            users[NICK] = true
            send(MSG_JOIN, {})
            addLine("* You are now known as " .. NICK, colors.yellow)
        else
            addLine("Invalid nick (1-16 chars)", colors.red)
        end
    elseif cmd == "/who" then
        local ulist = {}
        for u in pairs(users) do table.insert(ulist, u) end
        table.sort(ulist)
        addLine("Online: " .. table.concat(ulist, ", "), colors.cyan)
    elseif cmd == "/ping" then
        send(MSG_PING, {})
        addLine("Ping sent.", colors.gray)
    elseif cmd == "/clear" then
        history = {}
    elseif cmd:sub(1,1) == "/" then
        addLine("Unknown command: " .. cmd, colors.red)
    else
        -- Regular chat message
        send(MSG_CHAT, { text = cmd })
        local ts = string.format("[%02d:%02d]", math.floor(computer.uptime() / 60) % 60, math.floor(computer.uptime()) % 60)
        addLine(ts .. " <" .. NICK .. "> " .. cmd, colors.yellow)
    end
end

-- Main event loop
while running do
    -- Draw UI
    redrawChat()
    redrawStatus(inputBuf)
    
    -- Pull event with short timeout for responsiveness
    local ev, a, b, c, d = event.pull(0.5)
    
    if ev == "modem_message" then
        -- a=localAddr, b=remoteAddr, c=port, d=dist, e=msg
        local localA, remoteA, port, dist, msg = a, b, c, d, select(5, ev, a, b, c, d)
        -- Re-pull properly
        -- Actually event.pull returns all args after the name
        -- event = { "modem_message", localAddr, remoteAddr, port, dist, msg }
        -- But we got them split, so reconstruct:
        handleMessage(b, c, d, select(5, a, b, c, d))
    elseif ev == "key_down" then
        -- b = keyboard address, c = char code, d = key code
        local char = c
        local key  = d
        
        if key == 28 or key == 156 then
            -- Enter
            if #inputBuf > 0 then
                table.insert(inputHistory, 1, inputBuf)
                if #inputHistory > 50 then inputHistory[51] = nil end
                inputHistPos = 0
                handleCommand(inputBuf)
                inputBuf = ""
                inputCur = 0
            end
        elseif key == 14 then
            -- Backspace
            if #inputBuf > 0 then
                inputBuf = inputBuf:sub(1, -2)
            end
        elseif key == 200 then
            -- Up arrow - history
            if inputHistPos < #inputHistory then
                inputHistPos = inputHistPos + 1
                inputBuf = inputHistory[inputHistPos]
            end
        elseif key == 208 then
            -- Down arrow - history
            if inputHistPos > 0 then
                inputHistPos = inputHistPos - 1
                inputBuf = inputHistPos > 0 and inputHistory[inputHistPos] or ""
            end
        elseif key == 1 then
            -- Escape - clear input
            inputBuf = ""
            inputCur = 0
            inputHistPos = 0
        elseif char and char >= 32 and char < 256 then
            local ch = string.char(char)
            if #inputBuf < w - 4 then
                inputBuf = inputBuf .. ch
                inputHistPos = 0
            end
        end
    elseif ev == "interrupted" then
        running = false
    end
end

-- Announce leave
send(MSG_LEAVE, {})
component.invoke(modemAddr, "close", PORT)

-- Restore terminal
term.clear()
term.setCursor(1, 1)
term.setForeground(0xFFFFFF)
term.setBackground(0x000000)
print("Disconnected from channel #" .. CHANNEL)
