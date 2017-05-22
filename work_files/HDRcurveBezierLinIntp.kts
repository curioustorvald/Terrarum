/**
 * Weighted bezier curve interpolation by making bunch of linear curves, calculating intersect points between
 *     y = 1 / (1 + 3 * (k / n)) * eks ; eks <- [0..1023]
 *     (where k: step, n: max step)
 * and take smallest value
 */

val ymax = 260.0 // NOT a mistake!
val n = 1024
val polynomial = 6.0
val curves = Array(n, { k -> { eks: Int ->
    val p = k.toDouble() / n.toDouble()
    val m = (ymax - ymax * p) / (768.0 * Math.pow(p, polynomial) + 256.0 - ymax * p)
    m * (eks - ymax * p) + ymax * p
} })


print("\n\n")

(0..n - 1).forEach { step ->
    val intersects = curves.map { f -> f(step) }.sorted()
    var c = minOf(1f, intersects[0].toFloat() / 256f)
    if (c < 0f) c = 0f
    //println("step $step\t$c")

    if (step > 0 && step % 16 == 0) println()
    print("${c}f,")
}
