print("")
print("Starting Lunados...")

------------------
--- INITIALISE ---
------------------

require "common"

local prompt = "> "

_G.dos = {}
_G.dos.version = "0.1"
_G.dos.copyright = "Copyright (C) 2019 CuriousTorvald. Distributed under GNU GPL 3."
_G.dos.currentpath = {}

--- appends the directory into the current path
_G.dos.currentpath.push = function(name)
    table.insert(dos.path, name)
end

--- removes the current directory from the current path and returns what has been removed
_G.dos.currentpath.pop = function()
    return table.remove(dos.path)
end

_G.dos.envpath = "C:\\lunados\\bin;" -- must be a sting and not a table












--------------------------
--- LET THE SHOW BEGIN ---
--------------------------

print("Lunados Version "..dos.version)
print(dos.copyright)

--- PARSE AND RUN COMMANDS ---

local exit = false

while not exit do
    io.write(table.concat(dos.path, '\\'))
    io.write(prompt)
    local cmd = io.read()
    local commands = parsecmd(cmd)

    TODO()
end