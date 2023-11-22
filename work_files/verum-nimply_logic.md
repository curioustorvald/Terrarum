# Verum-Nimply Logic

Verum-Nimply Logic is a logic system built upon a two gates which are functionally complete.

## Atoms
### Verum (⊤)
Verum is a logic-gate modelling of the "Signal Source".
| Output |
|----|
| 1 |

### Falsum (⊥)
Falsum is a logic-gate modelling of the "Lack of Signal" or "No Connection".
| Output |
|----|
| 0 |

### Nimply (↛)
Nimply is a logic-gate modelling of the "Signal Blocker".

The source signal is connected to the Collector, and the blocking signal is connected to the Gate. The Nimply gate is not commutative.

This document will use the convention where the Collector is placed on the lefthand side.

| Collector | Gate | Emitter |
| ---- | ---- | ---- |
| 0 | 0 | 0 |
| 0 | 1 | 0 |
| 1 | 0 | 1 |
| 1 | 1 | 0 |

## Derivations

### Falsum
⊥ = (⊤ ↛ ⊤)

### NOT p
= (⊤ ↛ p)

### Buffer
p = (p ↛ ⊥)
= NOT (NOT p)
= (⊤ ↛ (⊤ ↛ p))

### p AND q
= (p ↛ (NOT q))
= (p ↛ (⊤ ↛ q))

### p NAND q
= NOT (p AND q)
= NOT (p ↛ (NOT q))
= NOT (p ↛ (⊤ ↛ q))
= (⊤ ↛ (p ↛ (⊤ ↛ q)))

### p OR q
Method 1:
= NOT (NOT(p) AND NOT(q))
= NOT ((⊤ ↛ p) AND (⊤ ↛ q))
= NOT ((⊤ ↛ p) ↛ (⊤ ↛ (⊤ ↛ q)))
= NOT ((⊤ ↛ p) ↛ q)
= (⊤ ↛ ((⊤ ↛ p) ↛ q))
Method 2:
= (NOT p) NAND (NOT q)
= (⊤ ↛ ((NOT p) ↛ (⊤ ↛ (NOT p))))
= (⊤ ↛ ((⊤ ↛ p) ↛ (⊤ ↛ (⊤ ↛ q))))
= (⊤ ↛ ((⊤ ↛ p) ↛ q))
Note: For the most cases, an OR gate can be substituted using merging wires and buffers.

### p NOR q
= NOT (p OR q)
= NOT (NOT(p) AND NOT(q))
= NOT(p) AND NOT(q)
= ((⊤ ↛ p) ↛ (⊤ ↛ (⊤ ↛ q)))
= ((⊤ ↛ p) ↛ q)

### p XOR q
= (p OR q) AND NOT(p AND q)
= (p OR q) AND (p NAND q)
= (⊤ ↛ ((⊤ ↛ p) ↛ q)) AND (⊤ ↛ (p ↛ (⊤ ↛ q)))
= ((⊤ ↛ ((⊤ ↛ p) ↛ q)) ↛ (⊤ ↛ (⊤ ↛ (p ↛ (⊤ ↛ q)))))
= ((⊤ ↛ ((⊤ ↛ p) ↛ q)) ↛ (p ↛ (⊤ ↛ q)))

## Proof by Simulation
```kotlin
val T = true
val F = false
infix fun Boolean.nimply(other: Boolean) = (this && !other)
fun Boolean.toInt() = if (this) 1 else 0
fun printTruthTable(fn: (Boolean, Boolean) -> Boolean) {
    println("p | q | out")
    println("--+---+----")
    for (p0 in 0..1) {
        for (q0 in 0..1) {
            val p = (p0 == 1)
            val q = (q0 == 1)
            println("${p.toInt()} | ${q.toInt()} | ${fn(p, q).toInt()}")
        }
    }
    println()
}
fun printTruthTable2(fn: (Boolean) -> Boolean) {
    println("p | out")
    println("--+----")
    for (p0 in 0..1) {
        val p = (p0 == 1)
        println("${p.toInt()} | ${fn(p).toInt()}")
    }
    println()
}
fun main() {
    println("NIMPLY")
    printTruthTable { p, q -> p nimply q }
    println("FALSUM")
    printTruthTable2 { p -> T nimply T }
    println("NOT")
    printTruthTable2 { p -> T nimply p }
    println("BUFFER")
    printTruthTable2 { p -> T nimply (T nimply p) }
    printTruthTable2 { p -> p nimply F }
    println("AND")
    printTruthTable { p, q -> p nimply (T nimply q) }
    println("NAND")
    printTruthTable { p, q -> T nimply (p nimply (T nimply q)) }
    println("OR")
    printTruthTable { p, q -> T nimply ((T nimply p) nimply q) }
    println("NOR")
    printTruthTable { p, q -> (T nimply p) nimply q }
    println("XOR")
    printTruthTable { p, q -> (T nimply ((T nimply p) nimply q)) nimply (p nimply (T nimply q)) }
}
```




