package net.torvald.terrarum.gameactors

import net.torvald.terrarum.KVHashMap

/**
 * Created by minjaesong on 2017-04-28.
 */
class ActorValue(@Transient val actor: Actor) : KVHashMap() {

    private constructor(actor: Actor, newMap: HashMap<String, Any>): this(actor) {
        hashMap = newMap
    }

    override fun set(key: String, value: Any) {
        /*if (key == AVKey.__PLAYER_QUICKSLOTSEL) {
            printStackTrace(this)
        }*/

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