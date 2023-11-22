# Verum-Nimply Logic

Verum-Nimply Logic is a logic system built upon a two gates which are functionally complete.

## Atoms
### Verum (⊤)
Verum is a logic-gate modelling of the "Signal Source".
| Output |
|----|
| 1 |

### Falsum (⊥)
Verum is a logic-gate modelling of the "Lack of Signal" or "No Connection".
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

### Diode
= (p ↛ ⊥)

### NOT p
= (⊤ ↛ p)

### p AND q
= (p ↛ (NOT q))
= (p ↛ (⊤ ↛ q))

### p NAND q
= NOT (p AND q)
= NOT (p ↛ (⊤ ↛ q))
= (⊤ ↛ (p ↛ (⊤ ↛ q)))

### p OR q
= NOT ( NOT(p) AND NOT(q) )
= NOT ( (⊤ ↛ p) AND (⊤ ↛ q) )
= NOT ( ( (⊤ ↛ p) ↛ (⊤ ↛ (⊤ ↛ p)) ) )
= (⊤ ↛ ( ( (⊤ ↛ p) ↛ (⊤ ↛ (⊤ ↛ p)) ) ))
Note: For the most cases, an OR gate can be substituted using merging wires and diodes.
