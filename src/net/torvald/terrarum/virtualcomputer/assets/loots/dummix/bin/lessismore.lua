--[[
LESS IS MORE

SYNOPSIS:
	lessismore [filename]
	less [filename]
	more [filename]
]]
local args = {...}
local prompt = function()
	term.setForeCol(3)
	term.emitString("scroll", 4, term.height())
	term.emitString("quit", 15, term.height())
	term.setForeCol(1)
	term.emit(18, 1, term.height())
	term.emit(29, 2, term.height())
	term.emit(81, 13, term.height())
	term.setForeCol(3)
end

local function printUsage()
	print("More: no file specified.")
	print("Usage: more [filename]")
end

if args[1] == nil or #args[1] <= 0 then printUsage() return end

----------------
-- fetch text --
----------------
local lines = {}
local displayHeight = term.height() - 1 -- bottom one line for prompt

local file = fs.open(args[1], "r")
local line = ""
repeat
	line = file.readLine()
	table.insert(lines, line)
until line == nil

-------------
-- display --
-------------
if term.isTeletype() then
	for _, l in ipairs(line) do
		term.print(l)
	end
else
	term.clear()
	term.setCursorBlink(false)
	
	local key = 0
	repeat
		prompt()
		
		for i, line in ipairs(lines) do
			if (i > displayHeight) then break end
	
			term.emitString(line, 1, i)
		end
	
		term.setCursor(1, term.height())
		if input.isKeyDown(keys.q) then break end
	until false
end

term.newLine()
return
