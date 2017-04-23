from random import randint

size = 128

names = []
keys = range(1, size + 1)


with open("language_DWARF.txt") as f:
	names = [line.rstrip('\n') for line in f]


for i in range(0, size):
	r = randint(0, size - 1)
	t = keys[i]
	keys[i] = keys[r]
	keys[r] = t


for i in range(0, size):
	print('"NAMESET_DWARVEN_%03d";"%s";') % (i + 1, names[keys[i]])
