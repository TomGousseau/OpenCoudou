-- settings.lua - Persistent key-value settings store
-- Provides a simple INI/JSON-like config file for programs
-- Usage as library: local cfg = require("settings"); cfg.set("key", val); cfg.save()
-- Usage as command: settings [list|get key|set key value|delete key|save|load]

local serialization = require("serialization")
local fs            = require("filesystem")

local settings = {}
settings._file    = "/etc/opencomputers.cfg"
settings._data    = {}
settings._defaults= {}
settings._dirty   = false

-- -----------------------------------------------------------------------
-- Core API
-- -----------------------------------------------------------------------

--- Define a default value for a key
function settings.default(key, value, doc)
    settings._defaults[key] = { value = value, doc = doc }
    if settings._data[key] == nil then
        settings._data[key] = value
    end
end

--- Get a value by key (returns default if not set)
function settings.get(key, fallback)
    local v = settings._data[key]
    if v == nil then
        local def = settings._defaults[key]
        return def and def.value or fallback
    end
    return v
end

--- Set a key to a value
function settings.set(key, value)
    settings._data[key] = value
    settings._dirty = true
end

--- Delete a key
function settings.delete(key)
    settings._data[key] = nil
    settings._dirty = true
end

--- Check if key exists
function settings.has(key)
    return settings._data[key] ~= nil
end

--- Get all keys
function settings.keys()
    local keys = {}
    for k in pairs(settings._data) do
        table.insert(keys, k)
    end
    table.sort(keys)
    return keys
end

--- Return the full data table
function settings.all()
    local copy = {}
    for k, v in pairs(settings._data) do
        copy[k] = v
    end
    return copy
end

-- -----------------------------------------------------------------------
-- Persistence
-- -----------------------------------------------------------------------

--- Load settings from a file (default: /etc/opencomputers.cfg)
function settings.load(path)
    path = path or settings._file
    if not fs.exists(path) then
        settings._data  = {}
        settings._dirty = false
        -- Apply defaults
        for k, d in pairs(settings._defaults) do
            settings._data[k] = d.value
        end
        return true
    end
    
    local content = fs.readAll(path)
    if not content then return false, "could not read file" end
    
    local ok, data = pcall(serialization.unserialize, content)
    if not ok or type(data) ~= "table" then
        return false, "invalid settings file"
    end
    
    settings._data  = data
    settings._dirty = false
    
    -- Apply defaults for missing keys
    for k, d in pairs(settings._defaults) do
        if settings._data[k] == nil then
            settings._data[k] = d.value
        end
    end
    
    return true
end

--- Save settings to a file
function settings.save(path)
    path = path or settings._file
    
    local dir = path:match("(.*)/[^/]+$")
    if dir and dir ~= "" then
        fs.makeDirectory(dir)
    end
    
    local content = serialization.serialize(settings._data, { pretty = true })
    local ok, err = fs.writeAll(path, content)
    if ok then
        settings._dirty = false
    end
    return ok, err
end

--- Reset to defaults
function settings.reset()
    settings._data = {}
    for k, d in pairs(settings._defaults) do
        settings._data[k] = d.value
    end
    settings._dirty = true
end

-- -----------------------------------------------------------------------
-- Built-in defaults
-- -----------------------------------------------------------------------

settings.default("shell.showPromptColors", true,  "Color the shell prompt")
settings.default("shell.history",          true,  "Save command history")
settings.default("shell.historySize",      200,   "Max history entries")
settings.default("term.font",              "vga", "Default terminal font")
settings.default("computer.sound",         true,  "Enable startup/error beeps")
settings.default("computer.logLevel",      "info","Log verbosity: debug/info/warn/error")
settings.default("network.autoOpen",       false, "Auto-open port 1 on boot")
settings.default("internet.timeout",       10,    "HTTP request timeout (seconds)")
settings.default("power.shutdownOnLow",    false, "Shutdown when energy < 5%")
settings.default("power.lowThreshold",     0.05,  "Low energy threshold (0.0-1.0)")

-- Auto-load on require
settings.load()

-- -----------------------------------------------------------------------
-- Command-line interface
-- -----------------------------------------------------------------------

local function runCLI(...)
    local args = { ... }
    local subcmd = args[1] or "list"
    
    if subcmd == "list" then
        local keys = settings.keys()
        if #keys == 0 then
            print("No settings.")
            return 0
        end
        local maxK = 0
        for _, k in ipairs(keys) do maxK = math.max(maxK, #k) end
        for _, k in ipairs(keys) do
            local v   = settings._data[k]
            local def = settings._defaults[k]
            local vStr = serialization.serialize(v)
            local flag = (def and def.value == v) and "" or "*"
            print(string.format("%-"..maxK.."s = %s%s", k, vStr, flag))
        end
        return 0
        
    elseif subcmd == "get" then
        local key = args[2]
        if not key then io.stderr:write("Usage: settings get <key>\n"); return 1 end
        local v = settings.get(key)
        if v == nil then
            io.stderr:write("Key not found: " .. key .. "\n")
            return 1
        end
        print(serialization.serialize(v))
        return 0
        
    elseif subcmd == "set" then
        local key   = args[2]
        local value = args[3]
        if not key or not value then
            io.stderr:write("Usage: settings set <key> <value>\n")
            return 1
        end
        -- Try to parse value (number, bool, string)
        local parsed
        if value == "true"  then parsed = true
        elseif value == "false" then parsed = false
        elseif tonumber(value)  then parsed = tonumber(value)
        else                        parsed = value
        end
        settings.set(key, parsed)
        settings.save()
        print("Set " .. key .. " = " .. serialization.serialize(parsed))
        return 0
        
    elseif subcmd == "delete" or subcmd == "del" then
        local key = args[2]
        if not key then io.stderr:write("Usage: settings delete <key>\n"); return 1 end
        settings.delete(key)
        settings.save()
        print("Deleted: " .. key)
        return 0
        
    elseif subcmd == "reset" then
        settings.reset()
        settings.save()
        print("Settings reset to defaults.")
        return 0
        
    elseif subcmd == "save" then
        local ok, err = settings.save()
        if ok then print("Saved to " .. settings._file)
        else io.stderr:write("Save failed: " .. tostring(err) .. "\n"); return 1 end
        return 0
        
    elseif subcmd == "load" then
        local ok, err = settings.load()
        if ok then print("Loaded from " .. settings._file)
        else io.stderr:write("Load failed: " .. tostring(err) .. "\n"); return 1 end
        return 0
        
    else
        io.stderr:write("Usage: settings <list|get|set|delete|reset|save|load>\n")
        return 1
    end
end

-- If run as a script (not required), execute CLI
if type((...)) == "string" then
    local ok, code = pcall(runCLI, ...)
    if not ok then io.stderr:write(tostring(code) .. "\n"); return 1 end
    return code
end

return settings
