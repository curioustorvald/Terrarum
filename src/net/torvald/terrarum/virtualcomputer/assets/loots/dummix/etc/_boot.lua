--[[
   Bootloader for Operation System

   Created by minjaesong on 2016-09-21
]]

-- check directories
dirlist = {
	"/boot",
	"/bin", -- crucial binaries (e.g. cat, ls, sh(ell), cp, rm, mkdir), it's loosely an UNIX system
	"/usr", 
	"/usr/bin", -- more utilities and binaries (e.g. less/more, nano)
	"/home", -- home directory for user
	"/home/bin", -- user-installed apps
	"/media" -- auto mounts (e.g. "/media/fd1", "/media/hdb", "/media/sda")
}
-- just make them if they don't exist
for _, dir in ipairs(dirlist) do
	fs.mkdir(dir)
end


if not _G.os then _G.os = {} end
os.version = "0.0"
os.EXIT_SUCCESS = 0
os.workingDir = {"home"}
os.path = "home/bin/;/usr/bin/;/bin/" -- infamous $path
os.fullWorkPath = function()
	local ret = table.concat(os.workingDir, "/") -- there's nothing wrong with this.

    if computer.verbose then
        machine.println("workingDir size: "..#os.workingDir)
        machine.println("fullWorkPath: "..ret)
    end
    return ret
end
os.setWorkingDir = function(s)
    if s:byte(#s) == 47 then
        s = string.sub(s, 1, #s - 1)
    end
    if s:byte(1) == 47 then
        s = string.sub(s, 2, #s)
    end

    if computer.verbose then
        machine.println("renew working dir; '"..s.."'")
    end

    local oldWorkingDir = {table.unpack(os.workingDir)}

    -- renew working directory, EVEN IF s STARTS WITH '/'
    local t = {}
    for word in string.gmatch(s, "[^/]+") do
        table.insert(t, word)
    end

    os.workingDir = t

    -- check if the directory exists
    if not fs.isDir(s) then
        os.errorNoSuchFileOrDir("cd: "..s)
        os.workingDir = oldWorkingDir
        return
    end
end
os.pushWorkingDir = function(s)
    if (s == "..") then
        error("cannot push '..' to working directory.")
    else
        table.insert(os.workingDir, s)

        if computer.verbose then
            machine.println("pushing '"..s.."' to working directory.")
        end
    end
end
os.popWorkingDir = function()
    if (#os.workingDir > 1) then
        table.remove(os.workingDir)
    end
end
-- @param  "path/of/arbitrary"
-- @return /working/dir/path/of/arbitrary
--         input path's trailing '/' is PRESERVED.
os.expandPath = function(p)
	-- not applicable if the path starts with /
	if p:byte(1) == 47 or p:byte(1) == 92 then
		return p
	end

	return os.fullWorkPath().."/"..p
end
os.defaultshell = "/bin/dsh.lua"
os.clock = function() return machine.milliTime() / 1000 end -- uptime of the computer, in seconds


function os.errorCmdNotFound(cmd)
    print(cmd..": command not found")
end

function os.errorNoSuchFile(cmd)
    print(cmd..": No such file")
end

function os.errorNoSuchFileOrDir(cmd)
    print(cmd..": No such file or directory")
end
function os.errorIsDir(cmd)
    print(cmd.." is a directory")
end


-- run default shell
fs.dofile(os.defaultshell)

-- quit properly
shell.status = shell.halt
