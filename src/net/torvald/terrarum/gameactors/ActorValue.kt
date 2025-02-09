package net.torvald.terrarum.gameactors

import net.torvald.terrarum.App
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.KVHashMap
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import net.torvald.terrarum.savegame.*
import java.io.IOException
import java.util.*
import kotlin.collections.HashMap

/**
 * For the dictionary of recognised ActorValues, see the source code of [net.torvald.terrarum.gameactors.AVKey]
 *
 * Created by minjaesong on 2017-04-28.
 */
class ActorValue : KVHashMap {

    @Transient lateinit var actor: Actor
        internal set

    @Transient private val BLOB = "blob://" // typical filename will be like "blob://-2147483648"

    constructor()

    constructor(actor: Actor) : this() {
        this.actor = actor
    }

    private constructor(actor: Actor, newMap: HashMap<String, Any>): this() {
        this.actor = actor
        hashMap = newMap
    }

    override fun set(key: String, value: Any) {
        // check if the key exists and is a blob
        if (getAsString(key.toLowerCase())?.startsWith(BLOB) == true) {
            throw IllegalStateException("Cannot write plain values to the blob object")
        }
        else
            super.set(key, value)

        actor.onActorValueChange(key, value) // fire the event handler
    }

    fun setBlob(key: String, ref: Long, value: ByteArray64) {
        checkActorBlobRef(ref)

        // if setBlob is invoked on non-existing key, adds random empty blob to the key
        // filename0 will be whatever value stored on the actorvalue, cast to String
        val preexisted = hashMap.containsKey(key.toLowerCase())

        val filename0 = hashMap.getOrPut(key.toLowerCase()) { "$BLOB$ref" } as String
        if (filename0.startsWith(BLOB)) {
            writeBlobBin(ref, value)
        }
        else {
            // rollback key addition, if applicable
            if (preexisted) hashMap.remove(key.toLowerCase())
            throw TypeCastException("ActorValue is not blob")
        }
    }

    fun getAsBlob(key: String): ByteArray64? {
        val uri = getAsString(key) ?: return null
        if (uri.startsWith(BLOB)) {
            return readBlobBin(uri.removePrefix(BLOB).toLong())
        }
        else throw TypeCastException("ActorValue is not blob")
    }

    override fun remove(key: String) {
        if (hashMap[key] != null) {
            val value = hashMap[key]!!
            hashMap.remove(key, value)

            // remove blob
            (value as? String)?.let {
                if (it.startsWith(BLOB)) {
                    deleteBlobBin(it.removePrefix(BLOB).toLong())
                }
            }

            actor.onActorValueChange(key, null)
        }
    }

    fun clone(newActor: Actor): ActorValue {
        return ActorValue(newActor, hashMap).also {
            val listOfBlobs = it.hashMap.entries.filter { (it.value as? String)?.startsWith(BLOB) ?: false }
            listOfBlobs.forEach {

            }
        }
        // TODO clone blobs
    }

    private fun readBlobBin(ref: Long): ByteArray64? {
        checkActorBlobRef(ref)

        if (INGAME.actorNowPlaying == actor) {
            val disk = INGAME.playerDisk
            return disk.getFile(ref)?.bytes
        }
        else if (actor is IngamePlayer) {
            val diskSkimmer = DiskSkimmer(Terrarum.getPlayerSaveFiledesc((actor as IngamePlayer).uuid.toString()))
            return diskSkimmer.getFile(ref)?.bytes
        }
        else throw IllegalStateException("Actor is not a player")
    }

    private fun writeBlobBin(ref: Long, contents: ByteArray64) {
        checkActorBlobRef(ref)

        val time = App.getTIME_T()
        val file = EntryFile(contents)

        if (INGAME.actorNowPlaying == actor) {
            val disk = INGAME.playerDisk

            disk.getFile(ref).let {
                // create new file if one not exists, then pass it
                if (it == null) {
                    VDUtil.addFile(disk, DiskEntry(ref, 0, time, time, file))
                    file
                }
                else it
            }.bytes = contents

            disk.getEntry(ref)!!.modificationDate = time
        }
        else if (actor is IngamePlayer) {
            val diskSkimmer = DiskSkimmer(Terrarum.getPlayerSaveFiledesc((actor as IngamePlayer).uuid.toString()))
            val oldCreationDate = diskSkimmer.getEntry(ref)?.creationDate

            diskSkimmer.appendEntry(DiskEntry(ref, 0, oldCreationDate ?: time, time, file))
        }
        else throw IllegalStateException("Actor is not a player")
    }

    private fun deleteBlobBin(ref: Long) {
        checkActorBlobRef(ref)

        if (INGAME.actorNowPlaying == actor) {
            val disk = INGAME.playerDisk
            VDUtil.deleteFile(disk, ref)
        }
        else if (actor is IngamePlayer) {
            val diskSkimmer = DiskSkimmer(Terrarum.getPlayerSaveFiledesc((actor as IngamePlayer).uuid.toString()))
            diskSkimmer.deleteEntry(ref)
        }
        else throw IllegalStateException("Actor is not a player")
    }

    private fun checkActorBlobRef(ref: Long) {
        if (ref > -2147483648L) throw IllegalArgumentException("File Ref $ref is not a valid ActorValue blob")
    }

}
