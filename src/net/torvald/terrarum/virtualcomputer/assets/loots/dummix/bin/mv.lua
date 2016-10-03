local args = {...}
if (#args ~= 2) then
print([[usage: mv source target
       mv source ... directory]])
return end
fs.mv(os.expandPath(args[1]), os.expandPath(args[2]))