-- filesystem.lua - Filesystem abstraction library

local component = component

local fs = {}

-- Get the primary filesystem component
local function getPrimary()
    return component.getPrimary("filesystem")
end

-- List files in a directory
function fs.list(path)
    local handle = getPrimary()
    if not handle then return nil, "no filesystem" end
    local ok, result = pcall(component.invoke, handle, "list", path)
    if not ok then return nil, result end
    return result
end

-- Check if path exists
function fs.exists(path)
    local handle = getPrimary()
    if not handle then return false end
    local ok, result = pcall(component.invoke, handle, "exists", path)
    return ok and result
end

-- Check if path is a directory
function fs.isDirectory(path)
    local handle = getPrimary()
    if not handle then return false end
    local ok, result = pcall(component.invoke, handle, "isDirectory", path)
    return ok and result
end

-- Check if path is readonly
function fs.isReadOnly(path)
    local handle = getPrimary()
    if not handle then return true end
    local ok, result = pcall(component.invoke, handle, "isReadOnly", path)
    return ok and result
end

-- Get file size
function fs.size(path)
    local handle = getPrimary()
    if not handle then return 0 end
    local ok, result = pcall(component.invoke, handle, "size", path)
    return ok and result or 0
end

-- Get last modified time
function fs.lastModified(path)
    local handle = getPrimary()
    if not handle then return 0 end
    local ok, result = pcall(component.invoke, handle, "lastModified", path)
    return ok and result or 0
end

-- Create directory
function fs.makeDirectory(path)
    local handle = getPrimary()
    if not handle then return false, "no filesystem" end
    local ok, result = pcall(component.invoke, handle, "makeDirectory", path)
    if not ok then return false, result end
    return result
end

-- Remove file or empty directory
function fs.remove(path)
    local handle = getPrimary()
    if not handle then return false, "no filesystem" end
    local ok, result = pcall(component.invoke, handle, "remove", path)
    if not ok then return false, result end
    return result
end

-- Rename/move
function fs.rename(from, to)
    local handle = getPrimary()
    if not handle then return false, "no filesystem" end
    local ok, result = pcall(component.invoke, handle, "rename", from, to)
    if not ok then return false, result end
    return result
end

-- Open a file
-- Returns a handle table with read/write/seek/close
function fs.open(path, mode)
    mode = mode or "r"
    local comp = getPrimary()
    if not comp then return nil, "no filesystem" end
    
    local ok, fileId = pcall(component.invoke, comp, "open", path, mode)
    if not ok then return nil, fileId end
    if not fileId then return nil, "cannot open: " .. path end
    
    local handle = {
        _comp = comp,
        _id = fileId,
        _closed = false,
    }
    
    function handle:read(n)
        if self._closed then return nil, "handle closed" end
        local ok, data = pcall(component.invoke, self._comp, "read", self._id, n)
        if not ok then return nil, data end
        return data
    end
    
    function handle:write(data)
        if self._closed then return nil, "handle closed" end
        local ok, err = pcall(component.invoke, self._comp, "write", self._id, data)
        if not ok then return nil, err end
        return true
    end
    
    function handle:seek(whence, offset)
        if self._closed then return nil, "handle closed" end
        local ok, pos = pcall(component.invoke, self._comp, "seek", self._id, whence, offset)
        if not ok then return nil, pos end
        return pos
    end
    
    function handle:close()
        if self._closed then return end
        self._closed = true
        pcall(component.invoke, self._comp, "close", self._id)
    end
    
    return handle
end

-- Read entire file
function fs.readAll(path)
    local handle, err = fs.open(path, "r")
    if not handle then return nil, err end
    
    local result = ""
    while true do
        local chunk = handle:read(4096)
        if not chunk then break end
        result = result .. chunk
    end
    handle:close()
    return result
end

-- Write entire file
function fs.writeAll(path, data)
    local handle, err = fs.open(path, "w")
    if not handle then return false, err end
    local ok, werr = handle:write(data)
    handle:close()
    if not ok then return false, werr end
    return true
end

-- Append to file
function fs.append(path, data)
    local handle, err = fs.open(path, "a")
    if not handle then return false, err end
    local ok, werr = handle:write(data)
    handle:close()
    if not ok then return false, werr end
    return true
end

-- Copy a file
function fs.copy(from, to)
    local data, err = fs.readAll(from)
    if not data then return false, err end
    return fs.writeAll(to, data)
end

-- Copy recursively
function fs.copyRecursive(from, to)
    if fs.isDirectory(from) then
        fs.makeDirectory(to)
        local files, err = fs.list(from)
        if not files then return false, err end
        for _, f in ipairs(files) do
            local ok, err2 = fs.copyRecursive(fs.concat(from, f), fs.concat(to, f))
            if not ok then return false, err2 end
        end
        return true
    else
        return fs.copy(from, to)
    end
end

-- Remove recursively
function fs.removeRecursive(path)
    if fs.isDirectory(path) then
        local files = fs.list(path)
        if files then
            for _, f in ipairs(files) do
                fs.removeRecursive(fs.concat(path, f))
            end
        end
    end
    return fs.remove(path)
end

-- Concatenate path segments
function fs.concat(...)
    local parts = { ... }
    local result = ""
    for _, part in ipairs(parts) do
        if part:sub(1, 1) == "/" then
            result = part
        elseif result == "" or result:sub(-1) == "/" then
            result = result .. part
        else
            result = result .. "/" .. part
        end
    end
    return fs.canonical(result)
end

-- Canonicalize path (resolve . and ..)
function fs.canonical(path)
    if not path or path == "" then return "/" end
    
    local absolute = path:sub(1, 1) == "/"
    local parts = {}
    
    for segment in path:gmatch("[^/]+") do
        if segment == ".." then
            table.remove(parts)
        elseif segment ~= "." and segment ~= "" then
            table.insert(parts, segment)
        end
    end
    
    local result = (absolute and "/" or "") .. table.concat(parts, "/")
    return result ~= "" and result or "/"
end

-- Get path components
function fs.path(path)
    return path:match("^(.*)/[^/]*$") or "/"
end

function fs.name(path)
    return path:match("([^/]+)$") or path
end

function fs.extension(path)
    local name = fs.name(path)
    return name:match("%.([^%.]+)$")
end

function fs.stem(path)
    local name = fs.name(path)
    return name:match("^(.-)%.[^%.]*$") or name
end

-- check if path is absolute
function fs.isAbsolute(path)
    return path:sub(1, 1) == "/"
end

-- Get free space
function fs.freeSpace(path)
    local comp = getPrimary()
    if not comp then return 0 end
    local ok, result = pcall(component.invoke, comp, "spaceAvailable")
    return ok and result or 0
end

-- Get total space
function fs.totalSpace(path)
    local comp = getPrimary()
    if not comp then return 0 end
    local ok, result = pcall(component.invoke, comp, "spaceTotal")
    return ok and result or 0
end

-- Combine multiple filesystems (for multi-drive setups)
local mounts = {}

function fs.mount(address, path)
    mounts[path] = address
end

function fs.unmount(path)
    mounts[path] = nil
end

function fs.getMounts()
    local result = {}
    for path, addr in pairs(mounts) do
        result[path] = addr
    end
    return result
end

return fs
