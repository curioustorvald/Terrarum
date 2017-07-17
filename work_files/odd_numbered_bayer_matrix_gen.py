def printMatrix(mat):
	for n in range(matrixSize):
		outstr = ""
		for k in range(matrixSize):
			outstr += str(mat[n][k])
			
			if (k < matrixSize - 1):
				outstr += ","


		print(outstr)



matrixSize = 9
# Matrix sizes I've tested:
# slanted diagonal (gud!): 7, 8, 9
# ortho pattern: 3, 4, 6
# ortho diagonal: 5


matrix = [x[:] for x in [[-1] * matrixSize] * matrixSize]


# init matrix
cellX = 0
cellY = (matrixSize >> 1)
for num in range(matrixSize * matrixSize):
	if (matrix[cellY][cellX] == -1):
		matrix[cellY][cellX] = num
	else:
		thefuck = matrix[cellY][cellX]
		error("Matrix position ("+str(cellX)+", "+str(cellY)+"is occupied by "+thefuck)

	if (matrix[(cellY - 1) % matrixSize][(cellX + 1) % matrixSize] == -1):
		cellX = (cellX + 1) % matrixSize
		cellY = (cellY - 1) % matrixSize
	else:
		cellY = (cellY + 1) % matrixSize


# vertical shifts
for xpos in range(0, matrixSize):
	lookup = [-1] * matrixSize
	
	for ycursor in range(matrixSize):
		lookup[ycursor] = matrix[ycursor][xpos]

	for ycursor in range(matrixSize):
		matrix[(ycursor - xpos) % matrixSize][xpos] = lookup[ycursor]


# horizontal shifts
for ypos in range(0, matrixSize):
	shift = (matrixSize - 1) - ypos + 1
	lookup = list(matrix[ypos])

	for xcursor in range(matrixSize):
		matrix[ypos][(xcursor + shift) % matrixSize] = lookup[xcursor]




printMatrix(matrix)