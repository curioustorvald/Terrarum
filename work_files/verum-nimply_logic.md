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






