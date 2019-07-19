_G.parsecmd = function(str)
    local parsetable = {}
    local quotemode = false
    local wordbuffer = ""

    for c = 1, #str do
        local char = str:byte(c)
        if not quotemode and char == 32 then -- quotestack is empty and char is a space
            table.insert(parsetable, wordbuffer)
            wordbuffer = ""
        elseif char == 34 then -- "
            quotemode = not quotemode
        else
            wordbuffer = wordbuffer..string.char(char)
        end
    end

    if #wordbuffer ~= 0 then
        table.insert(parsetable, wordbuffer)
    end

    return parsetable
end

_G.TODO = function(str)
    error("Not implemented: "..str or "TODO", 2)
end
