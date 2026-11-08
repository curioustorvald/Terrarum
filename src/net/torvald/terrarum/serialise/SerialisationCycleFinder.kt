package net.torvald.terrarum.serialise

import com.badlogic.gdx.utils.ArrayMap
import com.badlogic.gdx.utils.IntMap
import com.badlogic.gdx.utils.IntSet
import com.badlogic.gdx.utils.Json
import com.badlogic.gdx.utils.LongMap
import com.badlogic.gdx.utils.ObjectFloatMap
import com.badlogic.gdx.utils.ObjectIntMap
import com.badlogic.gdx.utils.ObjectMap
import com.badlogic.gdx.utils.ObjectSet
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Diagnostic for `StackOverflowError` thrown by libGDX `Json.toJson`.
 *
 * Walks the object graph using the same rules `Json.writeValue`/`Json.writeFields` (gdx 1.12.1) use — non-static,
 * non-transient, non-synthetic fields up the class hierarchy; collections, arrays and maps by element — and
 * reports the field path of the first reference cycle (or, failing that, of an excessively deep chain).
 *
 * The walk is iterative, so it cannot itself overflow the stack.
 *
 * Created by Claude on 2026-11-09.
 */
object SerialisationCycleFinder {

    private const val MAX_DEPTH = 2000

    private val leafTypes = setOf<Class<*>>(
        String::class.java,
        java.lang.Integer::class.java, java.lang.Boolean::class.java, java.lang.Float::class.java,
        java.lang.Long::class.java, java.lang.Double::class.java, java.lang.Short::class.java,
        java.lang.Byte::class.java, java.lang.Character::class.java,
    )

    private val fieldCache = HashMap<Class<*>, List<Field>>()

    private class Frame(val obj: Any, val label: String, val children: Iterator<Pair<String, Any?>>)

    /**
     * @param json the `Json` instance that was used; its registered serialisers are taken into account
     * @return human-readable report of the cycle or deep chain, or null if neither was found
     */
    fun find(root: Any, json: Json? = null): String? {
        val serialisers = json?.let { getSerialiserClasses(it) } ?: emptySet()

        val onPath = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>()) // fully explored, acyclic below
        val path = ArrayList<Frame>()

        fun push(obj: Any, label: String) {
            onPath.add(obj)
            path.add(Frame(obj, label, childrenOf(obj, serialisers)))
        }

        push(root, "<root>")

        while (path.isNotEmpty()) {
            val top = path.last()

            if (!top.children.hasNext()) {
                path.removeAt(path.lastIndex)
                onPath.remove(top.obj)
                visited.add(top.obj)
                continue
            }

            val (label, child) = top.children.next()
            if (child == null || isLeaf(child, serialisers) || child in visited) continue

            if (child in onPath) {
                val loopStart = path.indexOfFirst { it.obj === child }
                return buildString {
                    appendLine("Reference cycle found in the serialised object graph:")
                    appendPath(path)
                    appendLine("  -> $label : ${child.javaClass.name}   <== same instance as step #$loopStart")
                    append("Mark one of the fields in the loop @Transient (or give its class a custom serialiser).")
                }
            }

            if (path.size >= MAX_DEPTH) {
                return buildString {
                    appendLine("No cycle found within depth $MAX_DEPTH, but the object graph is at least this deep:")
                    appendPath(path.takeLast(40))
                    append("(only the last 40 steps shown)")
                }
            }

            push(child, label)
        }

        return null
    }

    private fun StringBuilder.appendPath(frames: List<Frame>) {
        frames.forEachIndexed { i, f -> appendLine("  #$i ${f.label} : ${f.obj.javaClass.name}") }
    }

    private fun getSerialiserClasses(json: Json): Set<Class<*>> = try {
        val f = Json::class.java.getDeclaredField("classToSerializer").also { it.isAccessible = true }
        val map = f.get(json) as ObjectMap<*, *>
        map.keys().map { it as Class<*> }.toSet()
    }
    catch (e: Throwable) {
        emptySet()
    }

    private fun isLeaf(obj: Any, serialisers: Set<Class<*>>): Boolean {
        val type = obj.javaClass
        if (type.isPrimitive || type in leafTypes) return true
        if (obj is Json.Serializable) return true // writes itself; can't be predicted
        if (obj is Enum<*>) return true
        if (obj is ObjectIntMap<*> || obj is ObjectFloatMap<*> || obj is IntSet) return true
        if (type.isArray && type.componentType.isPrimitive) return true
        // custom serialisers are leaves, except the map-based ones (HashArray etc.) which write their values out
        if (type in serialisers && obj !is Map<*, *> && obj !is Collection<*>) return true
        return false
    }

    private fun childrenOf(obj: Any, serialisers: Set<Class<*>>): Iterator<Pair<String, Any?>> {
        if (isLeaf(obj, serialisers)) return Collections.emptyIterator()

        return when (obj) {
            is com.badlogic.gdx.utils.Array<*> -> (0 until obj.size).asSequence().map { "[$it]" to obj[it] }.iterator()
            is com.badlogic.gdx.utils.Queue<*> -> (0 until obj.size).asSequence().map { "[$it]" to obj[it] }.iterator()
            is Collection<*> -> obj.toList().asSequence().mapIndexed { i, v -> "[$i]" to v }.iterator()
            is kotlin.Array<*> -> obj.asSequence().mapIndexed { i, v -> "[$i]" to v }.iterator()
            is ObjectMap<*, *> -> obj.entries().map { "[${it.key}]" to it.value }.iterator()
            is ObjectSet<*> -> obj.toList().asSequence().mapIndexed { i, v -> "[$i]" to v }.iterator()
            is IntMap<*> -> obj.entries().map { "[${it.key}]" to it.value }.iterator()
            is LongMap<*> -> obj.entries().map { "[${it.key}]" to it.value }.iterator()
            is ArrayMap<*, *> -> (0 until obj.size).asSequence().map { "[${obj.keys[it]}]" to obj.values[it] }.iterator()
            is Map<*, *> -> obj.entries.toList().asSequence().map { "[${it.key}]" to it.value }.iterator()
            else -> getFields(obj.javaClass).asSequence().map { f ->
                ".${f.name}" to try { f.get(obj) } catch (e: Throwable) { null }
            }.iterator()
        }
    }

    private fun getFields(type: Class<*>): List<Field> = fieldCache.getOrPut(type) {
        val hierarchy = ArrayList<Class<*>>()
        var c: Class<*>? = type
        while (c != null && c != Any::class.java) { hierarchy.add(c); c = c.superclass }

        hierarchy.asReversed().flatMap { it.declaredFields.toList() }.filter { f ->
            val m = f.modifiers
            !Modifier.isTransient(m) && !Modifier.isStatic(m) && !f.isSynthetic &&
                    try { f.isAccessible = true; true } catch (e: RuntimeException) { false }
        }
    }
}
