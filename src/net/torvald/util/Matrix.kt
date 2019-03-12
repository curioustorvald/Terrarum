package net.torvald.util

/**
 * Taken and improved from https://introcs.cs.princeton.edu/java/95linear/Matrix.java.html
 *
 * Created by minjaesong on 2018-08-01.
 */
class Matrix {
    val rows: Int             // number of rows; M
    val cols: Int             // number of columns; N
    val data: Array<DoubleArray>   // M-by-N array

    // create M-by-N matrix of 0's
    constructor(M: Int, N: Int) {
        this.rows = M
        this.cols = N
        data = Array(M) { DoubleArray(N) }
    }

    // create matrix based on 2d array
    constructor(data: Array<DoubleArray>) {
        rows = data.size
        cols = data[0].size
        this.data = Array(rows) { DoubleArray(cols) }
        for (i in 0 until rows)
            for (j in 0 until cols)
                this.data[i][j] = data[i][j]
    }

    // copy constructor
    private constructor(A: Matrix) : this(A.data) {}

    // swap rows i and j
    private fun swap(i: Int, j: Int) {
        val temp = data[i]
        data[i] = data[j]
        data[j] = temp
    }

    // create and return the transpose of the invoking matrix
    fun transpose(): Matrix {
        val A = Matrix(cols, rows)
        for (i in 0 until rows)
            for (j in 0 until cols)
                A.data[j][i] = this.data[i][j]
        return A
    }

    // return C = A + B
    operator fun plus(B: Matrix): Matrix {
        val A = this
        if (B.rows != A.rows || B.cols != A.cols) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(rows, cols)
        for (i in 0 until rows)
            for (j in 0 until cols)
                C.data[i][j] = A.data[i][j] + B.data[i][j]
        return C
    }


    // return C = A - B
    operator fun minus(B: Matrix): Matrix {
        val A = this
        if (B.rows != A.rows || B.cols != A.cols) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(rows, cols)
        for (i in 0 until rows)
            for (j in 0 until cols)
                C.data[i][j] = A.data[i][j] - B.data[i][j]
        return C
    }

    // does A = B exactly?
    override fun equals(B: Any?): Boolean {
        if (B !is Matrix) throw RuntimeException("Not a Matrix.")

        val A = this
        if (B.rows != A.rows || B.cols != A.cols) throw RuntimeException("Illegal matrix dimensions.")
        for (i in 0 until rows)
            for (j in 0 until cols)
                if (A.data[i][j] != B.data[i][j]) return false
        return true
    }

    // return C = A * B
    operator fun times(B: Matrix): Matrix {
        val A = this
        if (A.cols != B.rows) throw RuntimeException("Illegal matrix dimensions.")
        val C = Matrix(A.rows, B.cols)
        for (i in 0 until C.rows)
            for (j in 0 until C.cols)
                for (k in 0 until A.cols)
                    C.data[i][j] += A.data[i][k] * B.data[k][j]
        return C
    }


    // return x = A^-1 b, assuming A is square and has full rank
    fun solve(rhs: Matrix): Matrix {
        if (rows != cols || rhs.rows != cols || rhs.cols != 1)
            throw RuntimeException("Illegal matrix dimensions.")

        // create copies of the data
        val A = Matrix(this)
        val b = Matrix(rhs)

        // Gaussian elimination with partial pivoting
        for (i in 0 until cols) {

            // find pivot row and swap
            var max = i
            for (j in i + 1 until cols)
                if (Math.abs(A.data[j][i]) > Math.abs(A.data[max][i]))
                    max = j
            A.swap(i, max)
            b.swap(i, max)

            // singular
            if (A.data[i][i] == 0.0) throw RuntimeException("Matrix is singular.")

            // pivot within b
            for (j in i + 1 until cols)
                b.data[j][0] -= b.data[i][0] * A.data[j][i] / A.data[i][i]

            // pivot within A
            for (j in i + 1 until cols) {
                val m = A.data[j][i] / A.data[i][i]
                for (k in i + 1 until cols) {
                    A.data[j][k] -= A.data[i][k] * m
                }
                A.data[j][i] = 0.0
            }
        }

        // back substitution
        val x = Matrix(cols, 1)
        for (j in cols - 1 downTo 0) {
            var t = 0.0
            for (k in j + 1 until cols)
                t += A.data[j][k] * x.data[k][0]
            x.data[j][0] = (b.data[j][0] - t) / A.data[j][j]
        }
        return x

    }

    /**
     * for idioms of ```element = mat[row][column]```
     */
    operator fun get(i: Int): DoubleArray = data[i]

    companion object {

        // create and return a random M-by-N matrix with values between 0 and 1
        fun random(M: Int, N: Int): Matrix {
            val A = Matrix(M, N)
            for (i in 0 until M)
                for (j in 0 until N)
                    A.data[i][j] = Math.random()
            return A
        }

        // create and return the N-by-N identity matrix
        fun identity(N: Int): Matrix {
            val I = Matrix(N, N)
            for (i in 0 until N)
                I.data[i][i] = 1.0
            return I
        }

    }
}