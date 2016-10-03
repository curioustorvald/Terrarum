local args = {...}

os.dshenv = {}

--[[
DUMBSHELL: semi sh-compatible language interpreter

SYNOPSIS
    dsh [option] [file]
    sh [option] [file]

OPTIONS
	-c string	If the -c option is present, then commands are read from
				string. If there are arguments after the string, they are
				assigned to the positional parameters, starting with $0.


]]

-- returns full path. if p starts with "/", only the p is returned
local function expandPath(p)
    return (p:byte(1) == 47) and p or os.expandPath(p)
end

local function startsFromRoot(p)
    return p:byte(1) == 47
end

local function endsWithSlash(p)
    return p:byte(#p) == 47
end

__DSHDEBUG__ = 0x51621D

local function debug(msg)
    if __DSHDEBUG__ then print("DEBUG", msg) end
end

local function printErr(msg)
    print(DLE..msg)
end

-- BUILTINS -------------------------------------------------------------------

local function cd(tArgs)
    local dir = tArgs[1]

    if (dir == nil or #dir < 1) then return end

    -- check if the directory exists

    local chkdir = expandPath(dir)

    if not fs.exists(chkdir) then
        os.errorNoSuchFileOrDir("cd: "..dir)
        return
    end

    -- parse dir by delimeter '/'
    if (dir:byte(1) == 47) then -- if dir begins with '/'
        os.workingDir = {""}
    end

    for word in string.gmatch(dir, "[^/]+") do
        -- 'execute' directory
        -- Rules: '..' pops os.workingDir
        --        if dir begins with '/', re-build os.workingDir
        --        otherwise, push the 'word' to os.workingDir
        if (word == "..") then
            if (#os.workingDir > 1) then
                os.workingDir[#os.workingDir] = nil -- pops an element to oblivion
            end
        elseif (word == ".") then
            -- pass
        else
            table.insert(os.workingDir, word)
        end
    end
end

local function exit(tArgs)
    exitshell = true
end

local function exec(tArgs)
    debug("EXECARGS\t"..table.concat(tArgs, ", "))

    if (tArgs[1] == nil or #tArgs[1] < 1) then return end

    local filePath = tArgs[1]
    local fullFilePath = expandPath(tArgs[1])
    local execArgs = {}
    for i, v in ipairs(tArgs) do 
        if (i >= 2) then table.insert(execArgs, v) end
    end
    local execByPathFileExists = false
    local execByPathArg = ""

    -- do some sophisticated file-matching
    -- step 1: exact file
    if fs.exists(fullFilePath) and fs.isFile(fullFilePath) then 
        shell.run(fullFilePath, execArgs)
    -- step 2: try appending ".lua"
    elseif fs.exists(fullFilePath..".lua") and fs.isFile(fullFilePath..".lua") then 
        shell.run(fullFilePath..".lua", execArgs)
    -- step 3: parse os.path (just like $PATH)
    -- step 3.1: exact file; step 3.2: append ".lua"
    elseif not startsFromRoot(filePath) then
        for path in string.gmatch(os.path, "[^;]+") do
            -- check if 'path' ends with '/'
            if not endsWithSlash(path) then path = path.."/" end

            debug(path..filePath)

            if fs.exists(path..filePath) and fs.isFile(path..filePath) then
                execByPathArg = path..filePath
                execByPathFileExists = true
                break
            elseif fs.exists(path..filePath..".lua") and fs.isFile(path..filePath..".lua") then
                execByPathArg = path..filePath..".lua"
                execByPathFileExists = true
                break
            end
        end
    end

    -- step else: file not found
    if execByPathFileExists then
        shell.run(execByPathArg, execArgs)
        return EXIT_SUCCESS
    else
        if filePath:byte(1) == 46 or filePath:byte(1) == 47 then
            os.errorNoSuchFile(filePath)
        else
            os.errorCmdNotFound(filePath)
        end
    end

    return false
end

-- SYNTAX PARSER --------------------------------------------------------------

-- tables with functions
local builtins = {
    cd = cd,
    exit = exit,
    exec = exec,
    clear = term.clear
}

local function runcommand(str, recurse)
    if #str < 1 then return end

    local cmdFound = false
    
    -- simple cmd parse: WORD ARG1 ARG2 ARG3 ...
    local args = {}
    local command = ""
    for word in string.gmatch(str, "[^ ]+") do
        if #command < 1 then command = word -- first word will be a name of command
        else                 table.insert(args, word) end
    end

    if builtins[command] then -- try for builtins table
        builtins[command](args)
        cmdFound = true
        return true
    else

        -- FIXME: 'exec programname args' works, but not 'programname args'

        -- try for os.dshenv.aliases
        if os.dshenv.aliases[command] then
            --builtins[os.dshenv.aliases[command]](args)
            if not recurse then
                cmdFound = runcommand(os.dshenv.aliases[command], true)
            end
        else
            -- try to launch as program
            if not recurse then
                cmdFound = runcommand("exec "..str, true)
            end
        end
    end

    -- command not found (really)
    if not cmdFound then
        os.errorCmdNotFound(command)
    end
end

-- END OF SYNTAX PARSER -------------------------------------------------------
-- INIT SHELL -----------------------------------------------------------------

exitshell = false

-- load up aliases
if fs.exists("/etc/.dshrc") then fs.dofile("/etc/.dshrc") end


-- END OF INIT SHELL ----------------------------------------------------------

-- run interpreter and quit
if (args[1]) then
	local f = fs.open(args[1], "r")
	local line = ""
    local s = ""

    -- treat interpreter key (#!) properly
    -- I'm assuming I was called because I'm the right one
    -- I have a full trust on "shell.run()" that it rightfully redirected to me 
    -- 
    -- NOTE: shell redirection should only apply in interactive mode AND the input
    --       was like "./filename", or else I'm the right one. Period.
    --       (and that's how BASH works)
    repeat
        line = f.readLine()
        if line == nil then break end
        if line:sub(1,2) ~= "#!" then -- ignore line that contains hashbang
            s = s.." "..line
        end
    until line == nil

    f.close()

    runcommand(s)

    exitshell = true
end


function getPromptText()
    --return DC4..os.workingDir[#os.workingDir]..DC3.."# "..DC4 -- we're root! omgwtf
    return DC4..os.fullWorkPath()..DC3.."# "..DC4 -- we're root! omgwtf
end

-- interactive mode
local time = os.date()
print(time)

repeat
    term.setCursorBlink(true)
    io.write(getPromptText())
    local s = input.readLine()
    runcommand(s)
until exitshell

collectgarbage()
return EXIT_SUCCESS