# usage: pypy scriptname inputfile outputfile

import sys


def bandlimit(i):
	key = (i & 0xF0) >> 4
	return (key << 4) | key


outcontents = []

infile = open(sys.argv[1], "rb").read()

for i in infile:
	#print(ord(i))
	outcontents.append(bandlimit(ord(i)))

outfile = open(sys.argv[2], "wb")
outfile.write(bytearray(outcontents))
outfile.close()