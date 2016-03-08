MUL = 40
MUL_2 = MUL ** 2
MAX_STEP = MUL - 1

def getch(eightbit):
	return int(round(eightbit / 255.0 * MAX_STEP))


def getR(rgb24):
	return (rgb24 >> 16) & 0xFF
def getG(rgb24):
	return (rgb24 >> 8) & 0xFF
def getB(rgb24):
	return rgb24 & 0xFF


def getR40(raw):
	return raw / MUL_2
def getG40(raw):
	return (raw % MUL_2) / MUL
def getB40(raw):
	return raw % MUL


def intFromCol(r, g, b):
	return int(r * MUL_2 + g * MUL + b)
def intFromRGB24(r24, g24, b24):
	roundR = round(r24 / 255.0 * 39)
	roundG = round(g24 / 255.0 * 39)
	roundB = round(b24 / 255.0 * 39)
	return intFromCol(roundR, roundG, roundB)
def colFromNum(raw):
	return getR40(raw), getG40(raw), getB40(raw)

print(intFromCol(28, 0, 36))
print(colFromNum(57618))
