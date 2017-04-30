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
 * Created by minjaesong on 17-04-29.
 */

class BFVM(
        val memSize: Int = 65536,
        val stdout: OutputStream = System.out,
        val stdin: InputStream = System.`in`
) {
    private val ZERO = 0.toByte()

    private val INP = '>'.toByte()
    private val DEP = '<'.toByte()
    private val INC = '+'.toByte()
    private val DEC = '-'.toByte()
    private val PRN = '.'.toByte()
    private val RDI = ','.toByte()
    private val JPZ = '['.toByte()
    private val JPN = ']'.toByte()
    private val CYA = 0xFF.toByte()

    private val bfOpcodes = hashSetOf<Byte>(43,44,45,46,60,62,91,93)

    private val instSet = hashMapOf<Byte, () -> Unit>(
            Pair(INP, { INP() }),
            Pair(DEP, { DEP() }),
            Pair(INC, { INC() }),
            Pair(DEC, { DEC() }),
            Pair(PRN, { PRN() }),
            Pair(RDI, { RDI() }),
            Pair(JPZ, { JPZ() }),
            Pair(JPN, { JPN() })
    )


    private var r1: Byte = ZERO // Register One (Data register)
    private var r2 = 0 // Register Two (Scratchpad); theoretically I can use R1 but it limits bracket depth to 254
    private var mp = 0 // Memory Pointer
    private var pc = 0 // Program Counter
    private var ir = 0 // Instruction Register; does lookahead ahd lookbehind

    private val mem = ByteArray(memSize)


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
    JPN  ]      Jump back to matching [ when mem is non-zero

    [ Internal operations ]
    CYA  0xFF   Marks end of the input program
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
        stdout.write(mem[mp].toInt())
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
                INC_IR()
                if (JPZ == mem[ir]) {
                    r2++
                }
                else if (JPN == mem[ir]) {
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
                DEC_IR()
                if (JPN == mem[ir]) {
                    r2++
                }
                else if (JPZ == mem[ir]) {
                    r2--
                }
            }

            pc = ir
        }
    }
    // END OF NOTE (INC_PC is implied)


    fun execute() {
        while (mem[pc] != CYA) {
            //println("pc = $pc, mp = $mp, inst = ${mem[pc].toChar()}, mem = ${mem[mp]}")
            instSet[mem[pc]]?.invoke() // fetch-decode-execute in one line
            INC_PC()
        }
    }

    fun loadProgram(program: String) {
        val program = program.toByteArray(charset = Charsets.US_ASCII)

        pc = 0 // FOR NOW it's PC for input program
        mp = 0 // where to dump input bytes

        while (pc < program.size) {
            if (pc >= memSize - 1) {
                throw OutOfMemoryError("Virtual Machine Out of Memory")
            }

            r1 = program[pc]

            if (r1 in bfOpcodes) {
                mem[mp] = r1
                INC_MP()
            }

            INC_PC()
        }


        mem[program.size] = CYA
        mp = (program.size + 1) mod memSize
        pc = 0
        ir = 0
    }


    private fun INC_PC() { pc = (pc + 1) mod memSize }
    private fun INC_IR() { ir = (ir + 1) mod memSize }
    private fun DEC_IR() { ir = (ir - 1) mod memSize }
    private fun INC_MP() { mp = (mp + 1) mod memSize }
    private fun DEC_MP() { mp = (mp - 1) mod memSize }
    private infix fun Int.mod(other: Int) = Math.floorMod(this, other)
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

vm.loadProgram(factorials)
vm.execute()
