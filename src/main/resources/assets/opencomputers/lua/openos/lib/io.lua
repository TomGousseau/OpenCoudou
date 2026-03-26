-- io.lua - Standard I/O library for OpenOS

local fs = require("filesystem")
local process = require("process")
local term = require("term")

local io = {}

-- ------- Stream Metatable -------

local Stream = {}
Stream.__index = Stream

function Stream:write(...)
    if self.closed then
        return nil, "stream is closed"
    end
    local args = { ... }
    for i, v in ipairs(args) do
        local str = tostring(v)
        if self._write then
            local ok, err = self._write(self, str)
            if not ok then return nil, err end
        end
    end
    return self
end

function Stream:read(fmt)
    if self.closed then
        return nil, "stream is closed"
    end
    fmt = fmt or "*l"
    if type(fmt) == "number" then
        return self._readn(self, fmt)
    elseif fmt == "*l" or fmt == "l" then
        return self._readline(self)
    elseif fmt == "*n" or fmt == "n" then
        local line = self._readline(self)
        if not line then return nil end
        return tonumber(line)
    elseif fmt == "*a" or fmt == "a" then
        return self._readall(self)
    end
    return nil, "invalid format"
end

function Stream:lines(fmt)
    fmt = fmt or "*l"
    return function()
        return self:read(fmt)
    end
end

function Stream:seek(whence, offset)
    if self.closed then return nil, "stream is closed" end
    if self._seek then
        return self._seek(self, whence, offset)
    end
    return nil, "stream not seekable"
end

function Stream:flush()
    if self._flush then self._flush(self) end
    return self
end

function Stream:close()
    if self.closed then return end
    self.closed = true
    if self._close then self._close(self) end
    process.unregisterHandle(self)
end

function Stream:setvbuf(mode, size)
    -- Buffering control — basic implementation
    self._bufmode = mode
    self._bufsize = size
    return self
end

local function newStream(spec)
    local s = setmetatable({}, Stream)
    s.closed = false
    for k, v in pairs(spec) do
        s[k] = v
    end
    return s
end

-- ------- File Stream -------

local function openFileStream(path, mode)
    mode = mode or "r"
    local m = mode:gsub("b", "") -- binary flag not relevant in Lua 5.4 str mode
    
    local handle, err = fs.open(path, m)
    if not handle then
        return nil, err or "cannot open file"
    end
    
    local stream = newStream({
        _handle = handle,
        _path = path,
        _mode = mode,
        
        _write = function(self, str)
            return handle:write(str)
        end,
        
        _readline = function(self)
            local result = ""
            while true do
                local chunk, err = handle:read(1)
                if not chunk then return result ~= "" and result or nil end
                if chunk == "\n" then return result end
                result = result .. chunk
            end
        end,
        
        _readn = function(self, n)
            return handle:read(n)
        end,
        
        _readall = function(self)
            local result = ""
            while true do
                local chunk = handle:read(4096)
                if not chunk then break end
                result = result .. chunk
            end
            return result
        end,
        
        _seek = function(self, whence, offset)
            return handle:seek(whence, offset)
        end,
        
        _close = function(self)
            handle:close()
        end,
    })
    
    process.registerHandle(stream)
    return stream
end

-- ------- Terminal Streams -------

local stdinBuffer = ""

local stdin = newStream({
    _readline = function(self)
        local line = term.read()
        return line
    end,
    _readn = function(self, n)
        while #stdinBuffer < n do
            local char = computer.pullSignal()
            -- collect chars until we have enough
        end
        local result = stdinBuffer:sub(1, n)
        stdinBuffer = stdinBuffer:sub(n + 1)
        return result
    end,
    _readall = function(self)
        return term.read()
    end,
})

local stdout = newStream({
    _write = function(self, str)
        term.write(str)
        return true
    end,
})

local stderr = newStream({
    _write = function(self, str)
        local fg = term.getForeground()
        term.setForeground(0xFF4444)
        term.write(str)
        term.setForeground(fg)
        return true
    end,
})

io.stdin = stdin
io.stdout = stdout
io.stderr = stderr

-- Default files per process (can be redirected)
local defaultInput = stdin
local defaultOutput = stdout

-- ------- io functions -------

function io.open(path, mode)
    return openFileStream(path, mode or "r")
end

function io.lines(path, fmt)
    if path then
        local f, err = io.open(path, "r")
        if not f then error(err, 2) end
        return function()
            local line = f:read(fmt or "*l")
            if not line then
                f:close()
            end
            return line
        end
    else
        return io.input():lines(fmt)
    end
end

function io.input(file)
    if file then
        if type(file) == "string" then
            defaultInput = io.open(file, "r")
        elseif type(file) == "table" then
            defaultInput = file
        end
    end
    return defaultInput
end

function io.output(file)
    if file then
        if type(file) == "string" then
            defaultOutput = io.open(file, "w")
        elseif type(file) == "table" then
            defaultOutput = file
        end
    end
    return defaultOutput
end

function io.close(file)
    file = file or defaultOutput
    if file then
        file:close()
    end
end

function io.flush()
    if defaultOutput then
        defaultOutput:flush()
    end
end

function io.read(...)
    return defaultInput:read(...)
end

function io.write(...)
    local args = { ... }
    for _, v in ipairs(args) do
        defaultOutput:write(v)
    end
    return defaultOutput
end

function io.tmpfile()
    local tmpPath = "/tmp/__tmp__" .. tostring(math.random(100000, 999999))
    return io.open(tmpPath, "w+")
end

function io.type(file)
    if type(file) ~= "table" then return nil end
    if getmetatable(file) == Stream then
        if file.closed then
            return "closed file"
        end
        return "file"
    end
    return nil
end

-- Override print to use io.write
function _G.print(...)
    local args = { ... }
    for i, v in ipairs(args) do
        io.write(tostring(v))
        if i < #args then io.write("\t") end
    end
    io.write("\n")
end

return io
