--[[
   Must be loaded VERY FIRST!

   Created by minjaesong on 16-09-13.
--]]

-- path for any ingame libraries
package.path = "/net/torvald/terrarum/virtualcomputer/assets/lua/?.lua;" .. package.path

-- global variables
_G.MONEYSYM = string.char(0x9D) -- currency sign
_G.MIDDOT = string.char(0xFA) -- middle dot sign
_COMPUTER = {} -- standard console colours
_COMPUTER.DC1 = string.char(17) -- black
_COMPUTER.DC2 = string.char(18) -- white
_COMPUTER.DC3 = string.char(19) -- dim grey
_COMPUTER.DC4 = string.char(20) -- light grey
_COMPUTER.prompt = function()
    io.write(_COMPUTER.DC3 .. "> " .. _COMPUTER.DC4)
end
_COMPUTER.verbose = true -- print debug info
_COMPUTER.loadedCLayer = {} -- list of loaded compatibility layers
_COMPUTER.bootloader = "/boot/efi"
_COMPUTER.OEM = ""

-- load libraries that coded in Lua
require("ROMLIB")

-- load bios, if any
if fs.exists(_COMPUTER.bootloader) then shell.run(_COMPUTER.bootloader) end
if shell.status == shell.halt then
    __haltsystemexplicit__()
else
    -- load Lua prompt, if bios is not found
    if (#_COMPUTER.OEM > 0) then print(_COMPUTER.OEM) end
    print("Rom basic " .. _COMPUTER.DC2 .. _VERSION .. _COMPUTER.DC4)
    -- print(_COMPUTER.DC2 .. freemem .. _COMPUTER.DC4 .. " bytes free"
    print("Ok")
end
