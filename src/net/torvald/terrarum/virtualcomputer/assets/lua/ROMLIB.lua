--[[
   Created by minjaesong on 16-09-15.
--]]


fs.run = function(p)
    local f = fs.open(p, "r")
    local s = f.readAll()
    load(s)()
end

_G.loadstring = _G.load

--------------
-- HEXUTILS --
--------------

_G.hexutils = {}

_G.hexutils.toHexString = function(byteString)
    assert(type(byteString) == "string", error("Expected string."))

    -- speedup
    local function iToHex(i)
        if i == 0 then return "0"
        elseif i == 1 then return "1"
        elseif i == 2 then return "2"
        elseif i == 3 then return "3"
        elseif i == 4 then return "4"
        elseif i == 5 then return "5"
        elseif i == 6 then return "6"
        elseif i == 7 then return "7"
        elseif i == 8 then return "8"
        elseif i == 9 then return "9"
        elseif i == 10 then return "a"
        elseif i == 11 then return "b"
        elseif i == 12 then return "c"
        elseif i == 13 then return "d"
        elseif i == 14 then return "e"
        elseif i == 15 then return "f"
        else error("unrepresentable: " .. i)
        end
    end

    local ret = ""

    for i = 1, #byteString do
        local c = byteString:byte(i)
        local msb = iToHex(bit32.rshift(c, 4) % 16)
        local lsb = iToHex(c % 16)

        ret = ret .. (msb .. lsb)
    end

    return ret
end


---------------
-- SHELL API --
---------------

_G.shell = {}
shell.status = shell.ok

shell.run = function(p) fs.run(p) end


shell.ok = 0
shell.halt = 127


--------------
-- KEYS API --
--------------
-- ComputerCraft compliant
local keycodeDic = {
["30"] = "a",
["48"] = "b",
["46"] = "c",
["32"] = "d",
["18"] = "e",
["33"] = "f",
["34"] = "g",
["35"] = "h",
["23"] = "i",
["36"] = "j",
["37"] = "k",
["38"] = "l",
["50"] = "m",
["49"] = "n",
["24"] = "o",
["25"] = "p",
["16"] = "q",
["19"] = "r",
["31"] = "s",
["20"] = "t",
["22"] = "u",
["47"] = "v",
["17"] = "w",
["45"] = "x",
["21"] = "y",
["44"] = "z",
["2"] = "one",
["3"] = "two",
["4"] = "three",
["5"] = "four",
["6"] = "five",
["7"] = "six",
["8"] = "seven",
["9"] = "eight",
["10"] = "nine",
["11"] = "zero",
["12"] = "minus",
["13"] = "equals",
["14"] = "backspace",
["15"] = "tab",
["26"] = "leftBracket",
["27"] = "rightBracket",
["28"] = "enter",
["29"] = "leftCtrl",
["39"] = "semiColon",
["40"] = "apostrophe",
["41"] = "grave",
["42"] = "leftShift",
["43"] = "backslash",
["51"] = "comma",
["52"] = "period",
["53"] = "slash",
["54"] = "rightShift",
["55"] = "multiply",
["56"] = "leftAlt",
["57"] = "space",
["58"] = "capsLock",
["59"] = "f1",
["60"] = "f2",
["61"] = "f3",
["62"] = "f4",
["63"] = "f5",
["64"] = "f6",
["65"] = "f7",
["66"] = "f8",
["67"] = "f9",
["68"] = "f10",
["69"] = "numLock",
["70"] = "scollLock",
["71"] = "numPad7",
["72"] = "numPad8",
["73"] = "numPad9",
["74"] = "numPadSubtract",
["75"] = "numPad4",
["76"] = "numPad5",
["77"] = "numPad6",
["78"] = "numPadAdd",
["79"] = "numPad1",
["80"] = "numPad2",
["81"] = "numPad3",
["82"] = "numPad0",
["83"] = "numPadDecimal",
["87"] = "f11",
["88"] = "f12",
["89"] = "f13",
["90"] = "f14",
["91"] = "f15",
["-1"] = "kana",
["-1"] = "convert",
["-1"] = "noconvert",
["-1"] = "yen",
["-1"] = "numPadEquals",
["144"] = "cimcumflex",
["145"] = "at",
["146"] = "colon",
["147"] = "underscore",
["-1"] = "kanji",
["-1"] = "stop",
["-1"] = "ax",
["156"] = "numPadEnter",
["157"] = "rightCtrl",
["-1"] = "numPadComma",
["181"] = "numPadDivide",
["184"] = "rightAlt",
["197"] = "pause",
["199"] = "home",
["200"] = "up",
["201"] = "pageUp",
["203"] = "left",
["208"] = "right",
["207"] = "end",
["205"] = "down",
["209"] = "pageDown",
["210"] = "insert",
["211"] = "delete",
}

_G.keys = {}
_G.keys.getName = function(code) return keycodeDic[tostring(code)] end
