import sun.misc.Unsafe
import java.io.InputStream
import java.io.OutputStream

/**
 * Just to make things slow down
 *
 * This version of Brainfuck fills memory with sanitised input program, and initialises
 * memory pointer to be just right after your input program. This brings three major improvements:
 *
 *  1. Possibility of Self-modifying code
 *  2. Fucks your brain even more
 *  3. Forces you to enhance your calm
 *
 * Also note that program counter and memory pointer will wrap around when commands are executed,
 * but not when program is being loaded (will throw OutOfMemoryException).
 *
 * If memory at Program Counter is equal to 0xFF, it is interpreted as termination. (0xFF is NOT a
 * valid opcode for input program, however)
 *
 * Created by minjaesong on 2017-04-29.
 */

class BFVM(
        val memSize: Int = 65536,
        val stdout: OutputStream = System.out,
        val stdin: InputStream = System.`in`
) {
    annotation class Unsigned

    private val unsafe: Unsafe
    init {
        val unsafeConstructor = Unsafe::class.java.getDeclaredConstructor()
        unsafeConstructor.isAccessible = true
        unsafe = unsafeConstructor.newInstance()
    }

    private val DEBUG = true


    private val ZERO = 0.toByte()

    private val INP = '>'.toByte()
    private val DEP = '<'.toByte()
    private val INC = '+'.toByte()
    private val DEC = '-'.toByte()
    private val PRN = '.'.toByte()
    private val RDI = ','.toByte()
    private val JPZ = '['.toByte()
    private val JNZ = ']'.toByte()

    private val CYA = 0xFF.toByte()

    private val LDZ = '0'.toByte()
    private val ADM = 'M'.toByte()
    private val ADP = 'P'.toByte()
    private val SBM = 'm'.toByte()
    private val SBP = 'p'.toByte()


    private val bfOpcodes = hashSetOf<Byte>(43,44,45,46,60,62,91,93)

    private val instSet = hashMapOf<Byte, () -> Unit>(
            Pair(INP, { INP() }),
            Pair(DEP, { DEP() }),
            Pair(INC, { INC() }),
            Pair(DEC, { DEC() }),
            Pair(PRN, { PRN() }),
            Pair(RDI, { RDI() }),
            Pair(JPZ, { JPZ() }),
            Pair(JNZ, { JPN() }),
            Pair(LDZ, { LDZ() })
    )

    private val instOneArg = hashMapOf<@Unsigned Byte, (Int) -> Unit>(
            Pair(ADM, { i -> ADM(i) }),
            Pair(ADP, { i -> ADP(i) }),
            Pair(SBM, { i -> SBM(i) }),
            Pair(SBP, { i -> SBP(i) })
    )

    private var r1: Byte = ZERO // Register One (Data register)
    private var r2 = 0 // Register Two (Scratchpad); theoretically I can use R1 but it limits bracket depth to 254
    private var mp = 0 // Memory Pointer
    private var pc = 0 // Program Counter
    private var ir = 0 // Instruction Register; does lookahead ahd lookbehind

    private val mem = UnsafePtr(unsafe.allocateMemory(memSize.toLong()), memSize.toLong(), unsafe)

    init {
        mem.fillWith(0.toByte())
    }

    /*
    Input program is loaded into the memory from index zero.

    Interrupts are hard-coded, 'cause why not?

    Code Mnemo. Desc.
    ----|------|-----
    INP  >      Increment pointer
    DEP  <      Decrement pointer
    INC  +      Increment memory
    DEC  -      Decrement memory
    PRN  .      Print as text
    RDI  ,      Read from input
    JPZ  [      Jump past to matching ] when mem is zero
    JNZ  ]      Jump back to matching [ when mem is non-zero

    [ Internal operations ]
    CYA  0xFF   Marks end of the input program

    [ Optimise operations ]
    LDZ  0      Set memory to zero
    ADM  M      Add immediate to memory          (RLEing +)
    ADP  P      Add immediate to memory pointer  (RLEing >)
    SBM  m      Subtract immediate to memory          (RLEing -)
    SBP  p      Subtract immediate to memory pointer  (RLEing <)

     */

    // NOTE: INC_PC is implied
    private fun INP() {
        INC_MP()
    }
    private fun DEP() {
        DEC_MP()
    }
    private fun INC() {
        r1 = mem[mp]
        r1++
        mem[mp] = r1
    }
    private fun DEC() {
        r1 = mem[mp]
        r1--
        mem[mp] = r1
    }
    private fun PRN() {
        stdout.write(mem[mp].toUint())
    }
    private fun RDI() {
        r1 = stdin.read().toByte()
        mem[mp] = r1
    }
    private fun JPZ() {
        if (mem[mp] == ZERO) {
            // lookahead
            ir = pc
            r2 = 0

            while (r2 != -1) {
                ir++
                if (JPZ == mem[ir]) {
                    r2++
                }
                else if (JNZ == mem[ir]) {
                    r2--
                }
            }

            pc = ir
        }
    }
    private fun JPN() {
        if (mem[mp] != ZERO) {
            // lookbehind
            ir = pc
            r2 = 0

            while (r2 != -1) {
                ir--
                if (JNZ == mem[ir]) {
                    r2++
                }
                else if (JPZ == mem[ir]) {
                    r2--
                }
            }

            pc = ir
        }
    }
    // non-standard
    private fun LDZ() {
        mem[mp] = 0
    }
    private fun ADM(i: Int) {
        mem[mp] = (mem[mp] + i).toByte()
    }
    private fun SBM(i: Int) {
        mem[mp] = (mem[mp] - i).toByte()
    }
    private fun ADP(i: Int) {
        mp = (mp + i) mod memSize
    }
    private fun SBP(i: Int) {
        mp = (mp - i) mod memSize
    }
    // END OF NOTE (INC_PC is implied)


    fun quit() {
        mem.destroy()
    }

    fun execute() {
        dbgp("Now run...")
        while (mem[pc] != CYA) {
            //dbgp("pc = $pc, mp = $mp, inst = ${mem[pc].toChar()}  ${mem[pc+1]}, mem = ${mem[mp]}")

            r1 = mem[pc]

            if (r1 in instSet) {
                instSet[r1]!!.invoke() // fetch-decode-execute in one line
            }
            else if (r1 in instOneArg) {
                INC_PC()
                r2 = mem[pc].toUint()

                instOneArg[r1]?.invoke(r2)
            }
            else {
                dbgp("invalid: $r1")
            }

            INC_PC()
        }
    }

    fun loadProgram(program: String, optimizeLevel: Int = NO_OPTIMISE) {
        dbgp("Now load...")

        fun putOp(op: Byte) {

            //dbgp("${op.toChar()}    ${op.toUint()}")

            mem[mp] = op
            INC_MP()
        }

        val program = program.toByteArray(charset = Charsets.US_ASCII)

        r1 = 0 // currently reading operation
        pc = 0 // FOR NOW it's PC for input program
        mp = 0 // where to dump input bytes
        r2 = 0 // scratchpad
        ir = 0 // lookahead pointer

        while (pc < program.size) {
            if (pc >= memSize - 1) {
                throw OutOfMemoryError("Virtual Machine Out of Memory")
            }

            r1 = program[pc]

            if (r1 in bfOpcodes) {
                if (optimizeLevel >= 1) {
                    // [-] or [+]
                    if (r1 == JPZ) {
                        if ((program[pc + 1] == DEC || program[pc + 1] == INC) && program[pc + 2] == JNZ) {
                            pc += 3
                            putOp(LDZ)
                            continue
                        }
                    }
                    // RLEing +
                    else if (INC == r1 && program[pc + 1] == r1) {
                        ir = 2
                        while (INC == program[pc + ir] && ir < 255) ir++

                        pc += ir
                        putOp(ADM)
                        putOp(ir.toByte())
                        continue
                    }
                    // RLEing -
                    else if (DEC == r1 && program[pc + 1] == r1) {
                        ir = 2
                        while (DEC == program[pc + ir] && ir < 255) ir++

                        pc += ir
                        putOp(SBM)
                        putOp(ir.toByte())
                        continue
                    }
                    // RLEing >
                    else if (INP == r1 && program[pc + 1] == r1) {
                        ir = 2
                        while (INP == program[pc + ir] && ir < 255) ir++

                        pc += ir
                        putOp(ADP)
                        putOp(ir.toByte())
                        continue
                    }
                    // RLEing <
                    else if (DEP == r1 && program[pc + 1] == r1) {
                        ir = 2
                        while (DEP == program[pc + ir] && ir < 255) ir++

                        pc += ir
                        putOp(SBP)
                        putOp(ir.toByte())
                        continue
                    }
                }

                putOp(r1)
            }

            pc += 1
        }


        mem[mp] = CYA

        dbgp("Prg size: $mp")

        INC_MP()
        pc = 0
        ir = 0
    }


    private fun INC_PC() { pc = (pc + 1) mod memSize }
    private fun INC_MP() { mp = (mp + 1) mod memSize }
    private fun DEC_MP() { mp = (mp - 1) mod memSize }
    private infix fun Int.mod(other: Int) = Math.floorMod(this, other)
    private fun Byte.toUint() = java.lang.Byte.toUnsignedInt(this)

    private fun dbgp(s: Any) {
        if (DEBUG) println(s)
    }

    val NO_OPTIMISE = 0

    /*
    Optimise level
    1   RLE, Set cell to zero
     */

    private class UnsafePtr(pointer: Long, allocSize: Long, val unsafe: sun.misc.Unsafe) {
        var destroyed = false
            private set

        var ptr: Long = pointer
            private set

        var size: Long = allocSize
            private set

        fun realloc(newSize: Long) {
            ptr = unsafe.reallocateMemory(ptr, newSize)
        }

        fun destroy() {
            if (!destroyed) {
                unsafe.freeMemory(ptr)
                destroyed = true
            }
        }

        private inline fun checkNullPtr(index: Int) { // ignore what IDEA says and do inline this
            // commenting out because of the suspected (or minor?) performance impact.
            // You may break the glass and use this tool when some fucking incomprehensible bugs ("vittujen vitun bugit")
            // appear (e.g. getting garbage values when it fucking shouldn't)
            assert(!destroyed) { throw NullPointerException("The pointer is already destroyed ($this)") }

            // OOB Check: debugging purposes only -- comment out for the production
            //if (index !in 0 until size) throw IndexOutOfBoundsException("Index: $index; alloc size: $size")
        }

        operator fun get(index: Int): Byte {
            checkNullPtr(index)
            return unsafe.getByte(ptr + index)
        }

        operator fun set(index: Int, value: Byte) {
            checkNullPtr(index)
            unsafe.putByte(ptr + index, value)
        }

        fun fillWith(byte: Byte) {
            unsafe.setMemory(ptr, size, byte)
        }

        override fun toString() = "0x${ptr.toString(16)} with size $size"
        override fun equals(other: Any?) = this.ptr == (other as UnsafePtr).ptr
    }
}


val vm = BFVM()

val factorials = """
+++++++++++
>+>>>>++++++++++++++++++++++++++++++++++++++++++++
>++++++++++++++++++++++++++++++++<<<<<<[>[>>>>>>+>
+<<<<<<<-]>>>>>>>[<<<<<<<+>>>>>>>-]<[>++++++++++[-
<-[>>+>+<<<-]>>>[<<<+>>>-]+<[>[-]<[-]]>[<<[>>>+<<<
-]>>[-]]<<]>>>[>>+>+<<<-]>>>[<<<+>>>-]+<[>[-]<[-]]
>[<<+>>[-]]<<<<<<<]>>>>>[+++++++++++++++++++++++++
+++++++++++++++++++++++.[-]]++++++++++<[->-<]>++++
++++++++++++++++++++++++++++++++++++++++++++.[-]<<
<<<<<<<<<<[>>>+>+<<<<-]>>>>[<<<<+>>>>-]<-[>>.>.<<<
[-]]<<[>>+>+<<<-]>>>[<<<+>>>-]<<[<+>-]>[<+>-]<<<-]
"""

// expected output: 1, 1, 2, 3, 5, 8, 13, 21, 34, 55, 89

vm.loadProgram(factorials, optimizeLevel = 1)
vm.execute()
vm.quit()
