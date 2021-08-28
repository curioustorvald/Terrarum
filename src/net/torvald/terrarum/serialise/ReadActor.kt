package net.torvald.terrarum.serialise

import net.torvald.spriteanimation.HasAssembledSprite
import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.ActorHumanoid
import net.torvald.terrarum.modulebasegame.gameactors.IngamePlayer
import java.io.InputStream
import java.io.Reader

/**
 * Created by minjaesong on 2021-08-27.
 */
class ReadActor(val ingame: TerrarumIngame) {

    open fun invoke(worldDataStream: InputStream) {
        postRead(Common.jsoner.fromJson(IngamePlayer::class.java, worldDataStream))
    }

    open fun invoke(worldDataStream: Reader) {
        IngamePlayer()

        postRead(Common.jsoner.fromJson(IngamePlayer::class.java, worldDataStream))
    }

    private fun postRead(actor: IngamePlayer) {
        // filling in Transients
        actor.actorValue.actor = actor
        actor.inventory.actor = actor
        actor.sprite = SpriteAnimation(actor)
        if (actor.animDescPathGlow != null) actor.spriteGlow = SpriteAnimation(actor)
        actor.reassembleSprite(actor.sprite!!, actor.spriteGlow)
        // replace existing player
        ingame.forceRemoveActor(ingame.actorNowPlaying!!)
        ingame.addNewActor(actor)
        ingame.actorNowPlaying = actor
    }

}