--[[
   Created by minjaesong on 16-09-15.
--]]

-------------
-- ALIASES --
-------------

_G.io = {} -- we make our own sandbox'd system

--[[fs.dofile = function(p, ...)
    local f = fs.open(p, "r")
    local s = f.readAll()
    _G.runscript(s, "="..p, ...)
end]] -- implementation moved to BOOT.lua

_G.loadstring = _G.load

--_G.dofile = function(f) fs.dofile(f) end


fs.fetchText = function(p)
    local file = fs.open(p, "r")
    local text = file.readAll()
    file.close()
    return text
end

-----------------------------------------
-- INPUTSTREAM AND SCANNER (java-like) --
-----------------------------------------
--[[
In whatever code that actually runs everything (computer),
  there must be:

override fun keyPressed(key: Int, c: Char) {
    super.keyPressed(key, c)
    vt.keyPressed(key, c)

    if (key == Key.RETURN) {
        val input = vt.closeInputString()
    }
}

...it basically says to close the input if RETURN is hit,
  and THIS exact part will close the input for this function.
]]
_G.__scanforline__ = function(echo) -- pass '1' to not echo; pass nothing to echo
    machine.closeInputString()
    machine.openInput(echo or 0)
    _G.__scanMode__ = "line"
    local s
    repeat -- we can do this ONLY IF lua execution process is SEPARATE THREAD
        s = machine.getLastStreamInput()
    until s
    -- input is closed when RETURN is hit. See above comments.
    return s
end

-- use Keys API to identify the keycode
--[[_G.__scanforkey__ = function(echo) -- pass '1' to not echo; pass nothing to echo
    machine.closeInputString()
    machine.openInput(echo or 0)
    _G.__scanMode__ = "a_key"
    local key
    repeat -- we can do this ONLY IF lua execution process is SEPARATE THREAD
        key = machine.getLastKeyPress()
    until key
    -- input is closed when any key is hit. See above comments.
    return key
end]] -- DELETED: use _G.input.isKeyDown(keycode)

---                 ---
-- IO IMPLEMENTATION --
---                 ---


input.readLine = _G.__scanforline__

io.__openfile__ = "stdin"
io.stdin = "stdin"
io.stdout = "stdout"
io.stderr = "stderr"

io.open = fs.open

io.input = function(luafile)
    io.__openfile__ = luafile
end

io.read = function(option)
    if io.__openfile__ == "stdin" then
        return _G.__scanforline__()
    end

    function _readAll()
        return io.__openfile__.readAll()
    end

    function _readLine()
        return io.__openfile__.readLine()
    end

    options = {}
    options["*n"] = function() error("Read number is not supported, yet!") end--_readNumber
    options["*a"] = _readAll
    options["*l"] = _readLine
end

-----------------
-- PRINTSTREAM --
-----------------

io.write = function(...)
    local args = {...}
    for _, v in ipairs(args) do
        local s
        if v == nil then
            s = "nil"
        else
            s = tostring(v)
        end
        term.write(s)
    end
end
-- for some reason, inputstream above kills 'print' function.
-- So we rewrite it.
_G.print = function(...)
    local args = {...}

    io.write(args[1])

    if (#args > 1) then
        for i = 2, #args do
            io.write("\t")
            io.write(args[i])
        end
    end

    io.write("\n")
end


---------------
-- SHELL API --
---------------

_G.shell = {}
shell.status = shell.ok

-- run a script with path (string) and argstable (table)
shell.run = function(path, argstable)
    -- check for interpreter key "#!"
    local f = fs.open(path, "r")
    local s = f.readAll()
    f.close()

    if s:sub(1,2) == "#!" then
        local interpreter = s:sub(3)
        if not argstable then
            xpcall(function() fs.dofile(interpreter..".lua", path) end, function(err) print(DLE..err) end)
        else
            xpcall(function() fs.dofile(interpreter..".lua", path, table.unpack(argstable)) end, function(err) print(DLE..err) end)
        end
    else
        if not argstable then
            xpcall(function() fs.dofile(path) end, function(err) print(DLE..err) end)
        else
            xpcall(function() fs.dofile(path, table.unpack(argstable)) end, function(err) print(DLE..err) end)
        end
    end
end


shell.ok = 0
shell.halt = 127


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


--------------
-- KEYS API --
--------------
-- ComputerCraft compliant
local keycodeNumToName = {
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
    ["87"] = "f11",
    ["88"] = "f12",
    ["89"] = "f13",
    ["90"] = "f14",
    ["91"] = "f15",
    ["144"] = "cimcumflex",
    ["145"] = "at",
    ["146"] = "colon",
    ["147"] = "underscore",
    ["157"] = "rightCtrl",
    ["184"] = "rightAlt",
    ["197"] = "pause",
    ["199"] = "home",
    ["200"] = "up",
    ["201"] = "pageUp",
    ["203"] = "left",
    ["205"] = "right",
    ["207"] = "end",
    ["208"] = "down",
    ["209"] = "pageDown",
    ["210"] = "insert",
    ["211"] = "delete",
    ["219"] = "leftCommand"
}

_G.keys = {
    ["a"] = 30,
    ["b"] = 48,
    ["c"] = 46,
    ["d"] = 32,
    ["e"] = 18,
    ["f"] = 33,
    ["g"] = 34,
    ["h"] = 35,
    ["i"] = 23,
    ["j"] = 36,
    ["k"] = 37,
    ["l"] = 38,
    ["m"] = 50,
    ["n"] = 49,
    ["o"] = 24,
    ["p"] = 25,
    ["q"] = 16,
    ["r"] = 19,
    ["s"] = 31,
    ["t"] = 20,
    ["u"] = 22,
    ["v"] = 47,
    ["w"] = 17,
    ["x"] = 45,
    ["y"] = 21,
    ["z"] = 44,
    ["one"] = 2,
    ["two"] = 3,
    ["three"] = 4,
    ["four"] = 5,
    ["five"] = 6,
    ["six"] = 7,
    ["seven"] = 8,
    ["eight"] = 9,
    ["nine"] = 10,
    ["zero"] = 11,
    ["minus"] = 12,
    ["equals"] = 13,
    ["backspace"] = 14,
    ["tab"] = 15,
    ["leftBracket"] = 26,
    ["rightBracket"] = 27,
    ["enter"] = 28,
    ["leftCtrl"] = 29,
    ["semiColon"] = 39,
    ["apostrophe"] = 40,
    ["grave"] = 41,
    ["leftShift"] = 42,
    ["backslash"] = 43,
    ["comma"] = 51,
    ["period"] = 52,
    ["slash"] = 53,
    ["rightShift"] = 54,
    ["multiply"] = 55,
    ["leftAlt"] = 56,
    ["space"] = 57,
    ["capsLock"] = 58,
    ["f1"] = 59,
    ["f2"] = 60,
    ["f3"] = 61,
    ["f4"] = 62,
    ["f5"] = 63,
    ["f6"] = 64,
    ["f7"] = 65,
    ["f8"] = 66,
    ["f9"] = 67,
    ["f10"] = 68,
    ["numLock"] = 69,
    ["scollLock"] = 70,
    ["f11"] = 87,
    ["f12"] = 88,
    ["f13"] = 89,
    ["f14"] = 90,
    ["f15"] = 91,
    ["cimcumflex"] = 144,
    ["at"] = 145,
    ["colon"] = 146,
    ["underscore"] = 147,
    ["rightCtrl"] = 157,
    ["rightAlt"] = 184,
    ["pause"] = 197,
    ["home"] = 199,
    ["up"] = 200,
    ["pageUp"] = 201,
    ["left"] = 203,
    ["right"] = 205,
    ["end"] = 207,
    ["down"] = 208,
    ["pageDown"] = 209,
    ["insert"] = 210,
    ["delete"] = 211,
    ["leftCommand"] = 219
}
_G.keys.getName = function(code) return keycodeNumToName[tostring(code)] end
