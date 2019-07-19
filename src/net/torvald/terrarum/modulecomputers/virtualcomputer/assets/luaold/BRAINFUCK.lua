--[[
From  https://github.com/prapin/LuaBrainFuck/blob/master/brainfuck.lua

LuaBrainFuck License
--------------------

LuaBrainFuck is placed under the same license as Lua itself,
so licensed under terms of the MIT license reproduced below.
This means that the library is free software and can be used for both academic
and commercial purposes at absolutely no cost.

===============================================================================

Copyright (C) 2012 Patrick Rapin, CH-1543 Grandcour

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

===============================================================================

(end of COPYRIGHT)

Example usage: require "brainfuck" "+++>>> your BF code here <<<---"
 ]]
return function(s)
    local subst = {["+"]="v=v+1 ", ["-"]="v=v-1 ", [">"]="i=i+1 ", ["<"]="i=i-1 ",
        ["."] = "w(v)", [","]="v=r()", ["["]="while v~=0 do ", ["]"]="end "}
    local env = setmetatable({ i=0, t=setmetatable({},{__index=function() return 0 end}),
        r=function() return io.read(1):byte() end, w=function(c) io.write(string.char(c)) end },
        {__index=function(t,k) return t.t[t.i] end, __newindex=function(t,k,v) t.t[t.i]=v end })
    load(s:gsub("[^%+%-<>%.,%[%]]+",""):gsub(".", subst), "brainfuck", "t", env)()
end