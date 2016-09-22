--[[
   Must be loaded VERY FIRST!

   Created by minjaesong on 16-09-13.
--]]

-- path for any ingame libraries
package.path = "/net/torvald/terrarum/virtualcomputer/assets/lua/?.lua;" .. package.path

-- global variables
_G._VERSION = "Luaj-jse 5.2"
_G.MONEYSYM = string.char(0x9D) -- currency sign
_G.MIDDOT = string.char(0xFA) -- middle dot sign
_G.DC1 = string.char(17) -- black
_G.DC2 = string.char(18) -- white
_G.DC3 = string.char(19) -- dim grey
_G.DC4 = string.char(20) -- light grey
_G.DLE = string.char(16) -- default error colour
_G.getMem = function() collectgarbage() return collectgarbage("count") * 1024 end
-- getTotalMem: implemented in Kotlin class
_G.getFreeMem = function() return getTotalMem() - getMem() end
_G.runscript = function(s, source, ...)
    local code, reason = load(s, source)

    if _G.getFreeMem() <= 0 then
        print("out of memory")
        __haltsystemexplicit__()
        return
    end

    if code then
        xpcall(code(...), eprint)
    else
        print(DLE..tostring(reason)) -- it catches syntax errors
    end
end
_G.__scanMode__ = "UNINIT" -- part of inputstream implementation

_COMPUTER = {} -- standard console colours
_COMPUTER.prompt = DC3.."> "..DC4
_COMPUTER.verbose = true -- print debug info
_COMPUTER.loadedCLayer = {} -- list of loaded compatibility layers
_COMPUTER.bootloader = "/boot/efi"
_COMPUTER.OEM = ""

-- failsafe
if getTotalMem() == 0 then print("no RAM installed") __haltsystemexplicit__() return end
if _G.getFreeMem() <= 0 then print("out of memory") __haltsystemexplicit__() return end

-- load libraries that coded in Lua
require("ROMLIB")

-- load bios, if any
if fs.exists(_COMPUTER.bootloader) then shell.run(_COMPUTER.bootloader) end
-- halt/run luaprompt upon the termination of bios.
-- Valid BIOS should load OS and modify 'shell.status' to 'shell.halt' before terminating itself.
if shell.status == shell.halt then
    __haltsystemexplicit__()
end

-- load Lua prompt, if bios is not found
print("Rom basic "..DC2.._VERSION..DC4)
print("Copyright (C) 1994-2015 Lua.org, PUC-Rio")
print(DC2..tostring(math.floor(getFreeMem()/1024+0.5))..DC4.." Kbytes free")
print("To boot your system, run 'boot()'")
print("Ok")

while not native.isHalted() do
    while not native.isHalted() do
        io.write(_COMPUTER.prompt)
        local s = io.read()
        xpcall(
            function() _G.runscript(s, "=stdin") end,
            function(s) print(DLE..s) end -- it catches logical errors
        )
    end
end
native.closeInputString()
__haltsystemexplicit__()
return
