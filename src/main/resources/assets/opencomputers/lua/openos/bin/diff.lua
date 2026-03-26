-- bin/diff.lua - Compare two files line by line

local shell = require("shell")
local args  = shell.parse(...)

if #args < 2 then
  io.write("Usage: diff <file1> <file2>\n")
  os.exit(1)
end

local path1 = shell.resolve(args[1])
local path2 = shell.resolve(args[2])

local function readLines(path)
  local f, err = io.open(path, "r")
  if not f then
    io.stderr:write("diff: " .. path .. ": " .. (err or "cannot open") .. "\n")
    os.exit(1)
  end
  local lines = {}
  for line in f:lines() do
    table.insert(lines, line)
  end
  f:close()
  return lines
end

local a = readLines(path1)
local b = readLines(path2)

-- Longest common subsequence using dynamic programming
local function lcs(a, b)
  local m, n = #a, #b
  -- Use only two rows to save memory
  local prev = {}
  local curr = {}
  for j = 0, n do prev[j] = 0 end

  for i = 1, m do
    for j = 0, n do curr[j] = 0 end
    for j = 1, n do
      if a[i] == b[j] then
        curr[j] = (prev[j-1] or 0) + 1
      else
        curr[j] = math.max(curr[j-1] or 0, prev[j] or 0)
      end
    end
    prev, curr = curr, prev
  end

  -- Backtrack to find diff
  local diff = {}
  local i, j = m, n
  -- Rebuild full DP table for backtracking (smaller approach)
  local dp = {}
  for ii = 0, m do
    dp[ii] = {}
    for jj = 0, n do
      dp[ii][jj] = 0
    end
  end
  for ii = 1, m do
    for jj = 1, n do
      if a[ii] == b[jj] then
        dp[ii][jj] = dp[ii-1][jj-1] + 1
      else
        dp[ii][jj] = math.max(dp[ii-1][jj], dp[ii][jj-1])
      end
    end
  end

  -- Backtrack
  i, j = m, n
  local ops = {}
  while i > 0 or j > 0 do
    if i > 0 and j > 0 and a[i] == b[j] then
      table.insert(ops, 1, {op="=", line=a[i], ai=i, bi=j})
      i = i - 1
      j = j - 1
    elseif j > 0 and (i == 0 or dp[i][j-1] >= dp[i-1][j]) then
      table.insert(ops, 1, {op="+", line=b[j], bi=j})
      j = j - 1
    else
      table.insert(ops, 1, {op="-", line=a[i], ai=i})
      i = i - 1
    end
  end

  return ops
end

local ops = lcs(a, b)

-- Check if files are identical
local hasDiff = false
for _, op in ipairs(ops) do
  if op.op ~= "=" then
    hasDiff = true
    break
  end
end

if not hasDiff then
  -- No output = files are identical (standard diff behaviour)
  os.exit(0)
end

-- Print unified diff header
io.write("--- " .. args[1] .. "\n")
io.write("+++ " .. args[2] .. "\n")

-- Print diffs with context lines
local CONTEXT = 3
local printed = {}

for idx, op in ipairs(ops) do
  if op.op ~= "=" then
    -- Print context before
    for c = math.max(1, idx - CONTEXT), idx - 1 do
      if not printed[c] and ops[c] then
        printed[c] = true
        io.write(" " .. ops[c].line .. "\n")
      end
    end
    -- Print the changed line
    if not printed[idx] then
      printed[idx] = true
      io.write(op.op .. " " .. op.line .. "\n")
    end
    -- Print context after
    for c = idx + 1, math.min(#ops, idx + CONTEXT) do
      if not printed[c] and ops[c] and ops[c].op == "=" then
        printed[c] = true
        io.write(" " .. ops[c].line .. "\n")
      end
    end
  end
end

os.exit(1) -- diff exits 1 if files differ
