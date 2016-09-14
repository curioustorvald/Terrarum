-- global variables
_COMPUTER = {} -- standard console colours
_COMPUTER["DC1"] = string.char(17) -- black
_COMPUTER["DC2"] = string.char(18) -- white
_COMPUTER["DC3"] = string.char(19) -- dim grey
_COMPUTER["DC4"] = string.char(20) -- light grey
_COMPUTER["prompt"] = function()
    io.write(_COMPUTER.DC3 .. "> " .. _COMPUTER.DC4)
end
-- greet user
print("Rom basic " .. _COMPUTER.DC2 .. _VERSION .. _COMPUTER.DC4)
-- print(_COMPUTER.DC2 .. freemem .. _COMPUTER.DC4 .. " bytes free"
print("Ok")
-- prompt start
--_COMPUTER.prompt()
