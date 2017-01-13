def rawTo10bit(bit):
	b = (bit & 0xff)
	g = (bit & 0xff00) >> 8
	r = (bit & 0xff0000) >> 16
	return to10bit(r, g, b)

def to10bit(r, g, b):
	return (r << 20 | g << 10 | b)


def from10bit(tenbit):
	r = (tenbit >> 20) & 0xff
	g = (tenbit >> 10) & 0xff
	b = tenbit & 0xff
	return (r, g, b)

def to8bit(r, g, b):
	return (b << 16 | g << 8 | r)

print to10bit(255, 163, 0)
print from10bit(226629701)