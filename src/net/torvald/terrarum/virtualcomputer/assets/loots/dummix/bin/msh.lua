local args = {...}
local _APPVERSION = 0.3
--[[
MOONSHELL: basically just lua.lua

SYNOPSIS
    msh [file]

msh: Runs shell in interactive mode
msh [file]: Try to execute file as Lua script

]]

-- run interpreter and quit
if (args[1]) then
	local f = fs.open(args[1], "r")
	local line = ""
    local s = ""

    -- treat interpreter key (#!) properly
    -- I'm assuming I was called because I'm the right one
    -- I have a full trust on "shell.run()" that it rightfully redirected to me 
    repeat
        line = f.readLine()
        if line == nil then break end
        if line:sub(1,2) ~= "#!" then -- ignore line that contains hashbang
            s = s.." "..line
        end
    until line == nil

    f.close()

    xpcall(
        function() _G.runscript(s, "="..args[1]) end,
        function(err) print(DLE..err) end
    )

    goto terminate
end

-- interactive mode. This is a copy of BOOT.lua
run = shell.run

print("Moonshell "..DC2.._APPVERSION..DC4..", running "..DC2.._VERSION..DC4)
print("Lua is copyrighted (C) 1994-2013 Lua.org, PUC-Rio")
print("Run run(path) to execute program on 'path'.")
print("Run exit() to quit.")

while not machine.isHalted() do
    term.setCursorBlink(true)

    io.write(DC3.."lua"..computer.prompt)

    local s = input.readLine()

    if s == "exit()" then break end

    xpcall(
        function()
        	if s:byte(1) == 61 then -- print out value
    			s1 = string.sub(s, 2)
                _G.runscript("print(tostring("..s1.."))\n", "=stdin")
    		else
        		_G.runscript(s, "=stdin")
        	end
        end,
        function(err) print(DLE..err) end -- it catches logical errors
    )
end




::terminate::
collectgarbage()
return EXIT_SUCCESS
