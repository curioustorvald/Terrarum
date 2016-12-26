package net.torvald.terrarum.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.ai.scripts.PokemonNPCAI
import net.torvald.terrarum.mapdrawer.MapDrawer

/**
 * Created by minjaesong on 16-03-25.
 */
object PlayerBuilderCynthia {

    operator fun invoke(): ActorWithBody {
        //val p: Player = Player(GameDate(100, 143)) // random value thrown
        val p: HumanoidNPC = HumanoidNPC(PokemonNPCAI(), GameDate(100, 143)) // random value thrown
        InjectCreatureRaw(p.actorValue, "CreatureHuman.json")

        p.actorValue[AVKey.__PLAYER_QUICKBARSEL] = 0
        p.actorValue[AVKey.NAME] = "Cynthia"


        p.makeNewSprite(26, 42)
        p.sprite!!.setSpriteImage("assets/graphics/sprites/test_player_2.png")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 9, 0)

        p.setPosition(4096.0 * MapDrawer.TILE_SIZE, 300.0 * MapDrawer.TILE_SIZE)

        return p
    }


}