package net.torvald.terrarum.gameactors

import net.torvald.spriteanimation.SpriteAnimation
import net.torvald.terrarum.gameactors.ActorHumanoid
import net.torvald.terrarum.gameactors.ai.LuaAIWrapper
import net.torvald.terrarum.mapdrawer.FeaturesDrawer

/**
 * Created by minjaesong on 16-03-25.
 */
object PlayerBuilderCynthia {

    operator fun invoke(): ActorWithSprite {
        //val p: Player = Player(GameDate(100, 143)) // random value thrown
        val p: HumanoidNPC = HumanoidNPC(
                LuaAIWrapper("/net/torvald/terrarum/gameactors/ai/scripts/PokemonNPCAI.lua"),
                GameDate(100, 143)) // random value thrown
        InjectCreatureRaw(p.actorValue, "CreatureHuman.json")
        (p.ai as LuaAIWrapper).attachActor(p)


        p.actorValue[AVKey.__PLAYER_QUICKBARSEL] = 0
        p.actorValue[AVKey.NAME] = "Cynthia"


        p.makeNewSprite(26, 42, "assets/graphics/sprites/test_player_2.tga")
        p.sprite!!.setDelay(200)
        p.sprite!!.setRowsAndFrames(1, 1)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 9, 0)

        p.setPosition(4096.0 * FeaturesDrawer.TILE_SIZE, 300.0 * FeaturesDrawer.TILE_SIZE)




        p.referenceID = 321321321



        return p
    }


}