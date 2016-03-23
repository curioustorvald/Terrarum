MUL = 40
MUL_2 = MUL ** 2
MAX_STEP = MUL - 1
MAX_F = 39.0

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
	roundR = round(r24 / 255.0 * MAX_STEP)
	roundG = round(g24 / 255.0 * MAX_STEP)
	roundB = round(b24 / 255.0 * MAX_STEP)
	return intFromCol(roundR, roundG, roundB)
def colFromNum(raw):
	return getR40(raw), getG40(raw), getB40(raw)
def to24B(num):
	return int((getR40(num)) / MAX_F * 255.0), \
	int((getG40(num)) / MAX_F * 255.0),\
	int((getB40(num)) / MAX_F * 255.0)
def to24BHex(num):
	r, g, b = to24B(num)
	return hex(r)+hex(g)+hex(b)
def to24BInt(num):
	r, g, b = to24B(num)
	return r << 16 | g << 8 | b

print(to24BInt(27239))
