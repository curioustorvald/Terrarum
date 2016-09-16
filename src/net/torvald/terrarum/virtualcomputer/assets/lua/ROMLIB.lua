--[[
   Created by minjaesong on 16-09-15.
--]]


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
