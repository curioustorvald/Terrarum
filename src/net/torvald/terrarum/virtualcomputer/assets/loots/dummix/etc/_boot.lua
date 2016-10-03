--[[
   Bootloader for Operation System

   Created by minjaesong on 16-09-21
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
os.workingDir = {"", "home"} -- index 1 must be ""!
os.path = "home/bin/;/usr/bin/;/bin/" -- infamous $path

-- @param  "path/of/arbitrary"
-- @return /working/dir/path/of/arbitrary
--         input path's trailing '/' is PRESERVED.
os.expandPath = function(p)
	-- not applicable if the path starts with /
	if p:byte(1) == 47 or p:byte(1) == 92 then
		return p
	end

	return table.concat(os.workingDir, "/").."/"..p
end
os.fullWorkPath = function()
	return table.concat(os.workingDir, "/")
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
