local args = {...}

local dir = os.fullWorkPath()--(#args < 1) and os.fullWorkPath() or args[1] 

local list = fs.list("/"..dir)
table.sort(list)

for _, v in ipairs(list) do
	print(v)
end