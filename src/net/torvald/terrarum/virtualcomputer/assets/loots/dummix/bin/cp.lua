local args = {...}
if (#args ~= 2) then
print([[usage: cp source_file target_file
       cp source_file target_directory]])
return end
fs.cp(os.expandPath(args[1]), os.expandPath(args[2]))