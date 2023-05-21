package net.torvald.terrarum.gameactors

import net.torvald.terrarum.KVHashMap

/**
 * For the dictionary of recognised ActorValues, see the source code of [net.torvald.terrarum.gameactors.AVKey]
 *
 * Created by minjaesong on 2017-04-28.
 */
class ActorValue : KVHashMap {

    @Transient lateinit var actor: Actor
        internal set

    constructor()

    constructor(actor: Actor) : this() {
        this.actor = actor
    }

    private constructor(actor: Actor, newMap: HashMap<String, Any>): this() {
        this.actor = actor
        hashMap = newMap
    }

    override fun set(key: String, value: Any) {
        super.set(key, value)
        actor.onActorValueChange(key, value) // fire the event handler
    }

    override fun remove(key: String) {
        if (hashMap[key] != null) {
            hashMap.remove(key, hashMap[key]!!)
            actor.onActorValueChange(key, null)
        }
    }

    fun clone(newActor: Actor): ActorValue {
        return ActorValue(newActor, hashMap)
    }
}
