package net.torvald.terrarum

import io.github.classgraph.ClassGraph
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitems.GameItem
import kotlin.system.exitProcess

/**
 * Created by minjaesong on 2023-05-22.
 */
fun main() {

    val csiR = "\u001B[31m"
    val csiG = "\u001B[32m"
    val csi0 = "\u001B[m"

    val superClasses = listOf(
        Actor::class.java,
        GameItem::class.java
    )

    val serialisablePrimitives = listOf(
        // comparison: exact match
        "Z",
        "B",
        "C",
        "S",
        "I",
        "J",
        "F",
        "D",
    ).flatMap { listOf(it, "[$it") }

    val serialisableTypes = listOf(
        // arrays: has '[' prepended
        // comparison: startsWith

        // primitives
        "Ljava/lang/String", // includes ZipCodedStr
        "Ljava/lang/Boolean",
        "Ljava/lang/Byte",
        "Ljava/lang/Character",
        "Ljava/lang/Short",
        "Ljava/lang/Integer",
        "Ljava/lang/Long",
        "Ljava/lang/Float",
        "Ljava/lang/Double",
        // has serialiser on net.torvald.terrarum.serialise.Common
        "Ljava/math/BigInteger",
        "Ljava/util/UUID",
        "Ljava/util/HashMap",
        "Ljava/util/HashSet",
        "Ljava/util/ArrayList",
        "Lnet/torvald/terrarum/gameworld/BlockLayer",
        "Lnet/torvald/terrarum/gameworld/WorldTime",
        // complex types
        "Lnet/torvald/gdx/graphics/Cvec",
        "Lnet/torvald/random/HQRNG",
        "Lnet/torvald/spriteanimation/SpriteAnimation",
        "Lnet/torvald/terrarum/Codex",
        "Lnet/torvald/terrarum/Point2d",
        "Lnet/torvald/terrarum/Point2i",
        "Lnet/torvald/terrarum/KVHashMap",
        "Lnet/torvald/terrarum/gameactors/Actor\$RenderOrder",
        "Lnet/torvald/terrarum/gameactors/ActorValue",
        "Lnet/torvald/terrarum/gameactors/Hitbox",
        "Lnet/torvald/terrarum/gameactors/Lightbox",
        "Lnet/torvald/terrarum/gameactors/PhysProperties",
        "Lnet/torvald/terrarum/gameitems/GameItem",
        "Lnet/torvald/terrarum/utils/HashArray",
        "Lnet/torvald/terrarum/utils/HashedWirings",
        "Lnet/torvald/terrarum/utils/HashedWiringGraph",
        "Lnet/torvald/terrarum/utils/WiringGraphMap",
        "Lnet/torvald/terrarum/savegame/ByteArray64",
        "Lnet/torvald/terrarum/modulebasegame/gameactors/ActorInventory",
        "Lnet/torvald/terrarum/modulebasegame/gameactors/BlockBox",
        "Lnet/torvald/terrarum/modulebasegame/gameactors/FixtureInventory",
        "Lkotlin/ranges/IntRange",
        "Lorg/dyn4j/geometry/Vector2",
        "Lcom/badlogic/gdx/graphics/Color",
        "Lnet/torvald/terrarum/modulebasegame/gameactors/InventoryPair",
        // subclasses
        "Lnet/torvald/terrarum/gameactors/BlockMarkerActor",
        "Lnet/torvald/terrarum/gameactors/Actor",
        "Lnet/torvald/terrarum/gameactors/ActorWithBody",
        "Lnet/torvald/terrarum/gameactors/ActorHumanoid",
        // composite types
        "Lkotlin/Pair<Ljava/lang/Integer;Ljava/lang/Integer;>"
    ).flatMap { listOf(it, "[$it") }

    val classaNonGrata = listOf(
        "net.torvald.terrarum.modulebasegame.MovableWorldCamera",
        "net.torvald.terrarum.modulebasegame.TitleScreen\$CameraPlayer"
    )

    val classaNomenNonGrata = listOf(
        "Companion", "bulletDatabase"
    )

    val remarks = mapOf(
        "Ljava/util/List" to "java.util.List has no zero-arg constructor."
    )

    var retcode = 0

    ClassGraph().acceptPackages("net.torvald.terrarum")/*.verbose()*/.enableAllInfo().scan().let { scan ->
        val offendingFields = scan.allClasses.filter { classinfo ->
            superClasses.any { classinfo.extendsSuperclass(it) || classinfo.name == it.canonicalName } &&
            !classaNonGrata.contains(classinfo.name)
        }.flatMap { clazz ->
            clazz.declaredFieldInfo.filter { field ->
                !field.isTransient &&
                !field.isEnum &&
                !serialisablePrimitives.contains(field.typeSignatureOrTypeDescriptorStr) &&
                serialisableTypes.none { field.typeSignatureOrTypeDescriptorStr.startsWith(it) }
            }
        }.filter {
            !classaNomenNonGrata.contains(it.name) && !it.name.startsWith("this") && !it.name.contains("\$delegate")
        }

//        println(offendingFields)

        offendingFields.forEach {
            println("\u001B[1m${it.name}\u001B[m\n" +
                    "\t${csiG}from: ${csi0}${it.className}\n" +
                    "\t${csiG}type: ${csi0}${it.typeSignatureOrTypeDescriptorStr}\n" +
                    "\t${csiG}remarks: ${csi0}${remarks.keys.filter { key -> it.typeSignatureOrTypeDescriptorStr.startsWith(key) }.map { remarks[it] }.joinToString(" ")}")
            retcode = 1
        }
    }

    if (retcode != 0) {
        println("\n${csiR}Having above classes as non-@Transient may cause savegame to not load!$csi0")
    }

    exitProcess(retcode)
}