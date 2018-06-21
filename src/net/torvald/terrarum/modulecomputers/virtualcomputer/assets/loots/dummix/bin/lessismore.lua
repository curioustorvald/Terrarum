--[[
LESS IS MORE

SYNOPSIS:
	lessismore [filename]
	less [filename]
	more [filename]
]]
local args = {...}

displayLineNo = true

local prompt = function()
	term.setForeCol(3)

	term.emitString(" scroll   ", 3, term.height())
	term.emitString(" quit", 14, term.height())

	term.setForeCol(1)
	term.emit(18, 1, term.height())
	term.emit(29, 2, term.height())
	term.emit(81, 13, term.height())
	
	term.setForeCol(3)
	term.setBackCol(0)
end

local function printUsage()
	print("More: no file specified.")
	print("Usage: more [filename]")
end

if args[1] == nil or #args[1] <= 0 then printUsage() return end
filepath = os.expandPath(args[1])
if not fs.isFile(filepath) then os.errorNoSuchFile(filepath) return end

function log10(n)
	if n < 1 then return 0
	elseif n < 10 then return 1
	elseif n < 100 then return 2
	elseif n < 1000 then return 3
	elseif n < 10000 then return 4
	elseif n < 100000 then return 5
	elseif n < 1000000 then return 6
	elseif n < 10000000 then return 7
	elseif n < 100000000 then return 8
	elseif n < 1000000000 then return 9
	else return 10
	end
end

----------------
-- fetch text --
----------------
lines = {}
displayHeight = term.height() - 1 -- bottom one line for prompt

local file = fs.open(filepath, "r")
local line = ""
repeat
	line = file.readLine()
	table.insert(lines, line)
until line == nil

lineNoLen = log10(#lines)

-----------
-- input --
-----------
local function scrollDownAction(n)
	term.clearLine() -- prevent prompt to be scrolled
	curY = curY + n

	-- prevent overscroll
	if (curY > #lines - displayHeight) then
		curY = #lines - displayHeight
	end

	term.scroll(n)
	for i = 0, n - 1 do
		drawString(curY + displayHeight - i, displayHeight - i) -- redraw newline
	end
end

local function scrollUpAction(n)
	curY = curY - n

	-- prevent overscroll
	if (curY < 1) then
		curY = 1
	end

	term.scroll(-n)
	for i = 0, n - 1 do
		drawString(curY + i, i + 1) -- redraw prev line
	end
	term.setCursor(n, term.height())
end

local function processInput()
	if input.isKeyDown(keys.q) then quit = true end
	if input.isKeyDown(keys.down) and curY < #lines - displayHeight then 
		scrollDownAction(1)
		prompt()
	elseif input.isKeyDown(keys.pageDown) and curY < #lines - displayHeight then
		scrollDownAction(8)
		prompt()
	elseif input.isKeyDown(keys.up) and curY > 1 then 
		scrollUpAction(1)
		term.clearLine() -- make space for prompt
		prompt()
	elseif input.isKeyDown(keys.pageUp) and curY > 1 then 
		scrollUpAction(8)
		term.clearLine() -- make space for prompt
		prompt()
	end

	machine.sleep(50)
end

-------------
-- display --
-------------
displayWidth = term.width() - 1 - (displayLineNo and lineNoLen or 0)

function drawString(lineNo, y)
	local string = (lineNo > #lines) and "" 
			or lines[lineNo]:sub(curX, curX + displayWidth)

	if (displayLineNo) then
		local lineNoStr = DC3..string.format("%"..lineNoLen.."d", curY + y - 1)..DC4
		string = lineNoStr..string
	end

	local strDrawX = curX
	term.emitString(string, strDrawX, y)
end

function redrawText()
	for i = curY, #lines do
		if (i >= displayHeight + curY) then break end
		drawString(i, i - curY + 1)
	end
end

curY = 1
curX = 1
quit = false

if term.isTeletype() then
	for _, l in ipairs(line) do
		term.print(l)
	end
	quit = true
end


term.clear()
term.setCursorBlink(false)
redrawText()

repeat
	prompt()

	term.setCursor(1, term.height())
	processInput()
until quit

term.clearLine()
return
