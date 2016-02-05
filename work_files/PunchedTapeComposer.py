#!/usr/bin/python

# Terrarum Punched tape music roll composer
# Copyright 2013 SKYHi14
# See SKYHi14.nfo for license.
#
#  Punched tape music roll format
#  Range: 0 - 2**32 - 1
#  A1 ---- A2 ---- A3 ---- A4 ---- A5 ---- A6 AS6 B6
# 2**0 ---- 12 ---- 24 ---- 36 ---- 48 ---- 60 61  62
#
# Byte 0-7F 80-FF  100-103 104+
# Desc Name Author Tempo   Data
#
# Output format: signed long (little endian)
# If you are not capable of Python, there is Lua version available

A_1 = 2**0
AS1 = 2**1
B_1 = 2**2
C_1 = 2**3
CS1 = 2**4
D_1 = 2**5
DS1 = 2**6
E_1 = 2**7
F_1 = 2**8
FS1 = 2**9
G_1 = 2**10
GS1 = 2**11

A_2 = 2**12
AS2 = 2**13
B_2 = 2**14
C_2 = 2**15
CS2 = 2**16
D_2 = 2**17
DS2 = 2**18
E_2 = 2**19
F_2 = 2**20
FS2 = 2**21
G_2 = 2**22
GS2 = 2**23

A_3 = 2**24
AS3 = 2**25
B_3 = 2**26
C_3 = 2**27
CS3 = 2**28
D_3 = 2**29
DS3 = 2**30
E_3 = 2**31
F_3 = 2**32
FS3 = 2**33
G_3 = 2**34
GS3 = 2**35

A_4 = 2**36
AS4 = 2**37
B_4 = 2**38
C_4 = 2**39
CS4 = 2**40
D_4 = 2**41
DS4 = 2**42
E_4 = 2**43
F_4 = 2**44
FS4 = 2**45
G_4 = 2**46
GS4 = 2**47

A_5 = 2**48
AS5 = 2**49
B_5 = 2**50
C_5 = 2**51
CS5 = 2**52
D_5 = 2**53
DS5 = 2**54
E_5 = 2**55
F_5 = 2**56
FS5 = 2**57
G_5 = 2**58
GS5 = 2**59

A_6 = 2**60
AS6 = 2**61
B_6 = 2**62

def main():
	musicRoll = [C_2, E_2, G_2, C_2 + E_2 + G_2, 0] # Add your music roll!
	SongName = "ANNYEONG SAESANG" # within 127 characters
	SongAuthor = "PYTHON" # within 127 characters
	tempo = 90 # greater than 0!

	print("Processing your music roll. Hold on.")

	tapePuncher(SongName, SongAuthor, tempo, musicRoll)
	
	print("Wrote file to " + SongName + ".")

def tapePuncher(name, author, tempo, roll):
	binArray = []
	
	SaveDirectory = "/DirectoryToBeSaved\\WithoutFilename"
		
	file = open(SaveDirectory + name, "wb")
	
	file.write( chr(0) )
	
	for i in range(1, 128):
		if i <= len(name):
			file.write( chr( ord(name[i - 1]) ) )
		else:
			file.write( chr(0) )
	
	file.write( chr(0) )
	
	for i in range(1, 128):
		if i <= len(author):
			file.write( chr( ord(author[i - 1]) ) )
		else:
			file.write( chr(0) )

	file.write( chr(tempo % 256) )
	file.write( chr( tempo - ( tempo % 256 ) ) )
	file.write( chr(0) )
	file.write( chr(0) )
	
	for i in range(1, len(roll)):
		binArray = toBin( roll[i - 1] )
		
		for i in range(1, 9):
			if i <= len(binArray):
				file.write( binArray[len(binArray) - i] )
			else:
				file.write( chr(0) )
		
	file.close()

def toBin(num):
	output = []
	
	sHexNum = hex(num)[2:]
	if len(sHexNum) % 2 == 1:
		sHexNum = "0" + sHexNum
		
	for i in range(1, len(sHexNum)):
		# print( hexToInt(ord(sHexNum[i])) * 16 + hexToInt(ord(sHexNum[i + 1])) )
		output.append( chr( hexToInt(ord(sHexNum[i - 1])) * 16 + hexToInt(ord(sHexNum[i])) ) )
	
	return output

def hexToInt(num):
	char = chr(num)
	hexTable = {"0":0, "1":1, "2":2, "3":3, "4":4, "5":5, "6":6, "7":7, "8":8, "9":9, "a":10, "b":11, "c":12, "d":13, "e":14, "f":15}
	return hexTable[char]


main()