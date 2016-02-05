#!/usr/local/bin/lua

-- Terrarum Punched tape music roll composer
-- Copyright 2013 SKYHi14
-- See SKYHi14.nfo for license.
--
--  Punched tape music roll format
--  Range: 0 - 2^63 - 1
--  A1 ---- A2 ---- A3 ---- A4 ---- A5 ---- A6 AS6 B6
-- 2^0 ---- 12 ---- 24 ---- 36 ---- 48 ---- 60 61  62
--
-- Byte 0-7F 80-FF  100-103 104+
-- Desc Name Author Tempo   Data
--
-- Output format: signed long (little endian)
-- If you are not capable of Lua, there is Python version available

A_1 = 2^0
AS1 = 2^1
B_1 = 2^2
C_1 = 2^3
CS1 = 2^4
D_1 = 2^5
DS1 = 2^6
E_1 = 2^7
F_1 = 2^8
FS1 = 2^9
G_1 = 2^10
GS1 = 2^11

A_2 = 2^12
AS2 = 2^13
B_2 = 2^14
C_2 = 2^15
CS2 = 2^16
D_2 = 2^17
DS2 = 2^18
E_2 = 2^19
F_2 = 2^20
FS2 = 2^21
G_2 = 2^22
GS2 = 2^23

A_3 = 2^24
AS3 = 2^25
B_3 = 2^26
C_3 = 2^27
CS3 = 2^28
D_3 = 2^29
DS3 = 2^30
E_3 = 2^31
F_3 = 2^32
FS3 = 2^33
G_3 = 2^34
GS3 = 2^35

A_4 = 2^36
AS4 = 2^37
B_4 = 2^38
C_4 = 2^39
CS4 = 2^40
D_4 = 2^41
DS4 = 2^42
E_4 = 2^43
F_4 = 2^44
FS4 = 2^45
G_4 = 2^46
GS4 = 2^47

A_5 = 2^48
AS5 = 2^49
B_5 = 2^50
C_5 = 2^51
CS5 = 2^52
D_5 = 2^53
DS5 = 2^54
E_5 = 2^55
F_5 = 2^56
FS5 = 2^57
G_5 = 2^58
GS5 = 2^59

A_6 = 2^60
AS6 = 2^61
B_6 = 2^62

function main()
	musicRoll = {C_2, E_2, G_2, C_2 + E_2 + G_2, 0} -- Add your music roll!
	SongName = "ANNYEONG SAESANG BY LUA" -- within 127 characters
	SongAuthor = "LUA" -- within 127 characters
	tempo = 90 -- greater than 0!

	print("Processing your music roll. Hold on.")

	tapePuncher(SongName, SongAuthor, tempo, musicRoll)
	
	print("Wrote file to " .. SongName .. ".")
end

function tapePuncher(name, author, tempo, roll)
	SaveDirectory = "/DirectoryToBeSaved\\WithoutFilename"
			
	local file = io.open(SaveDirectory .. name, "wb")
		
	file:write( string.char(0) )
	
	for i = 1, 127, 1 do
		if i <= #name then
			file:write( string.char( string.byte(name, i) ) )
		else
			file:write( string.char(0) )
		end
	end
	
	file:write( string.char(0) )
	
	for i = 1, 127, 1 do
		if i <= #author then
			file:write( string.char( string.byte(author, i) ) )
		else
			file:write( string.char(0) )
		end
	end

	file:write( string.char(tempo % 256) )
	file:write( string.char( tempo - ( tempo % 256 ) ) )
	file:write( string.char(0) )
	file:write( string.char(0) )
	
	for i = 1, #roll, 1 do
		binArray = toBin( roll[i] )
		
		for i = 1, 8, 1 do
			if i <= #binArray then
				file:write( binArray[#binArray + 1 - i] )
			else
				file:write( string.char(0) )
			end
		end
	end
		
	file:close()
end

function toBin(num)
	output = {}
	
	sHexNum = DEC_HEX(num)
	if #sHexNum % 2 == 1 then
		sHexNum = "0" .. sHexNum
	end
		
	for i = 1, #sHexNum, 2 do
		--print( hexToInt(string.byte(sHexNum, i)) * 16 + hexToInt(string.byte(sHexNum, i + 1)) )
		output[#output + 1] = string.char( hexToInt(string.byte(sHexNum, i)) * 16 + hexToInt(string.byte(sHexNum, i + 1)) )
	end
	
	return output
end
	
function DEC_HEX(IN)
    local B,K,OUT,I,D=16,"0123456789ABCDEF","",0
    while IN>0 do
        I=I+1
        IN,D=math.floor(IN/B),math.mod(IN,B)+1
        OUT=string.sub(K,D,D)..OUT
    end
    return OUT
end

function hexToInt(int)
	chr = string.char(int)
	hexTable = {["0"] = 0, ["1"] = 1, ["2"] = 2, ["3"] = 3, ["4"] = 4, ["5"] = 5, ["6"] = 6, ["7"] = 7, ["8"] = 8, ["9"] = 9, ["A"] = 10, ["B"] = 11, ["C"] = 12, ["D"] = 13, ["E"] = 14, ["F"] = 15}
	return hexTable[chr]
end

main()