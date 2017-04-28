package net.torvald.terrarum.gameactors

import net.torvald.terrarum.KVHashMap

/**
 * Created by SKYHi14 on 2017-04-28.
 */
class ActorValue(val actor: Actor) : KVHashMap() {

    private constructor(actor: Actor, newMap: HashMap<String, Any>): this(actor) {
        hashMap = newMap
    }

    override fun set(key: String, value: Any) {
        super.set(key, value)
        actor.actorValueChanged(key, value) // fire the event handler
    }

    override fun remove(key: String) {
        if (hashMap[key] != null) {
            hashMap.remove(key, hashMap[key]!!)
            actor.actorValueChanged(key, null)
        }
    }

    fun clone(newActor: Actor): ActorValue {
        return ActorValue(newActor, hashMap)
    }
}