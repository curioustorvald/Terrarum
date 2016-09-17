--[[
-- ComputerCraft API compatibility layer
   Usage: require("CCAPI")

   Created by minjaesong on 16-09-16.
--]]

--------------
-- PREAMBLE --
--------------

if term.isTeletype() then error("This is a teletype; cannot use CCAPI layer") end


table.insert(_COMPUTER.loadedCLayer, "CCAPI")


local function intLog2(i)
    if i == 0 then return 0 end
    local log = 0
    if bit32.band(i, 0xffff0000) ~= 0 then i = bit32.rshift(i, 16) log = 16 end
    if i >= 256 then i = bit32.rshift(i, 8) log = log + 8 end
    if i >= 16  then i = bit32.rshift(i, 8) log = log + 4 end
    if i >= 4   then i = bit32.rshift(i, 8) log = log + 2 end
    return log + bit32.rshift(i, 1)
end


-------------
-- BIT API --
-------------

_G.bit = {} -- CC's weird BIT API

bit.blshift = function(n, bits) bit32.lshift(n, bits) end
bit.brshift = function(n, bits) bit32.arshift(n, bits) end
bit.blogic_rshift = function(n, bits) bit32.brshift(n, bits) end
bit.bxor = function(m, n) bit32.bxor(m, n) end
bit.bor = function(m, n) bit32.bor(m, n) end
bit.band = function(m, n) bit32.band(m, n) end
bit.bnot = function(n) bit32.bnot(n) end


----------------
-- COLORS API --
----------------

_G.colors = {}

colors.white = 0x1
colors.orange = 0x2
colors.magenta = 0x4
colors.lightBlue = 0x8
colors.yellow = 0x10
colors.lime = 0x20
colors.pink = 0x40
colors.gray = 0x80
colors.lightGray = 0x100
colors.cyan = 0x200
colors.purple = 0x400
colors.blue = 0x800
colors.brown = 0x1000
colors.green = 0x2000
colors.red = 0x4000
colors.black = 0x8000

colors.combine = function(...)
    local ret = 0
    for _, c in ipairs(...) do
        ret = bor(ret, c)
    end
    return ret
end

local function containsCol(target, cccol)
    return bit32.band(target, cccol) > 0
end

colors.subtract = function(cccol, ...)
    for _, c in ipairs(...) do
        if not containsCol(cccol, c) then
            cccol = bit32.bxor(cccol, c)
        end
    end
    return cccol
end


local function normaliseCCcol(cccol)
    if cccol >= 0x1 and cccol <= 0x8FFF then
        return intLog2(cccol)
    else
        error("invalid CC Colors: "..cccol)
    end
end


--------------
-- TERM API --
--------------

-- paint_index -> Terminal colour index
local ccToGameCol = {--pink
    1, 5, 7, 10, 4, 11, 15, 2, 3, 10, 8, 9, 14, 12, 6, 0
}

-- "a" -> 10, "3" -> 3
local function cHexToInt(c)
    if type(c) == "number" then -- char
        if c >= 48 and c <= 57 then
            return c - 48
        elseif c >= 65 and c <= 70 then
            return c - 65
        elseif c >= 97 and c <= 102 then
            return c - 97
        else
            return 0
        end
    elseif type(c) == "string" then -- single-letter string
        if c:byte(1) >= 48 and c:byte(1) <= 57 then
            return c:byte(1) - 48
        elseif c:byte(1) >= 65 and c:byte(1) <= 70 then
            return c:byte(1) - 65
        elseif c:byte(1) >= 97 and c:byte(1) <= 102 then
            return c:byte(1) - 97
        else
            --error("unrepresentable: " .. c)
            -- return black, as defined in http://www.computercraft.info/wiki/Term.blit
            return 0
        end
    else
        error("bad argument (string or number expected, got "..type(c)..")")
    end
end

--                    str,     str,     str
term.blit = function(text, foreCol, backCol)
    assert(
        type(text) == "string" and type(backCol) == "string" and type(foreCol) == "string",
        "bad argument: (string, string, string expected, got "..type(text)..", "..type(foreCol)..", "..type(backCol)..")"
    )
    if #text ~= #foreCol or #text ~= #backCol or #foreCol ~= #backCol then
        error("arguments must be the same length")
    end

    for i = 1, #text do
        term.setForeCol(cHexToInt(foreCol:byte(i)))
        term.setBackCol(cHexToInt(backCol:byte(i)))
        term.emit(text:byte(i))
        term.moveCursor(term.getX() + 1, term.getY())
    end
end

term.getCursorPos = function() return term.getCursor() end
term.setCursorPos = function(x, y) term.moveCursor(x, y) end
term.setCursorBlink = function(b) term.blink(b) end
term.isColor = function() return term.isCol() end
term.getSize = function() return term.size() end
term.setTextColor = function(cccol) term.setForeCol(ccToGameCol[normaliseCCcol(cccol)]) end
term.getTextColor = function() return term.getForeCol() end
term.setBackgroundColor = function(cccol) term.setBackCol(ccToGameCol[normaliseCCcol(cccol)]) end
term.getBackgroundColor = function() return term.getBackCol() end


--------------------
-- FILESYSTEM API --
--------------------

fs.makeDir = function(p) fs.mkdir(p) end
fs.move = function(a, b) fs.mv(a, b) end
fs.copy = function(a, b) fs.cp(a, b) end
fs.delete = function(p) fs.rm(p) end
fs.combine = function(a, b) return fs.concat(a, b) end
fs.getDir = function(p) return fs.parent(p) end


------------------
-- DOWN AND OUT --
------------------

if _COMPUTER.verbose then print("ComputerCraft compatibility layer successfully loaded.") end
