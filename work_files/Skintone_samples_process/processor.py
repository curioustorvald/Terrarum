# fianised 2016-03-08
# by minjaesong

import math


def RGBToHSV(r, g, b):
	rgbMin = min(r, g, b)
	rgbMax = max(r, g, b)

	h = 0.0
	s = 0.0
	v = float(rgbMax) / 255.0

	delta = float(rgbMax - rgbMin)

	if (rgbMax != 0):
		s = delta / rgbMax
	else:
		s = 0
		h = 0
		return h, s, v

	if (r == rgbMax):
		h = (g - b) / delta
	elif (g == rgbMax):
		h = 2 + (b - r) / delta
	else:
		h = 4 + (r - g) / delta

	h *= 60
	if (h < 0):
		h += 360

	return int(round(h)), int(round(s * 100)), int(math.ceil(v * 100))


infile = open("/Users/minjaesong/Desktop/Calculators/Skintone_samples_process/samples.raw", "rb").read()

rgbTable = [[0], [0], [0]]

fileReadCounter = 0
CHANNEL_R = 0
CHANNEL_G = 1
CHANNEL_B = 2

for i in infile:
	rgbTable[fileReadCounter % 3].append(ord(i))
	fileReadCounter += 1


for i in range(len(rgbTable[0])):
	h, s, v = RGBToHSV(rgbTable[CHANNEL_R][i], rgbTable[CHANNEL_G][i], rgbTable[CHANNEL_B][i])
	if (h != 0 and s != 0 and v != 0):
		print(h, s, v)
