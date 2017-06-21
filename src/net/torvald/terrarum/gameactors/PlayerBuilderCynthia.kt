package net.torvald.terrarum.gameactors

import net.torvald.terrarum.ModMgr
import net.torvald.terrarum.gameactors.ai.LuaAIWrapper
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack

/**
 * Created by minjaesong on 16-03-25.
 */
object PlayerBuilderCynthia {

    operator fun invoke(): ActorWithPhysics {
        //val p: Player = Player(GameDate(100, 143)) // random value thrown
        val p: HumanoidNPC = HumanoidNPC(
                LuaAIWrapper("/net/torvald/terrarum/gameactors/ai/scripts/PokemonNPCAI.lua"),
                GameDate(100, 143)) // random value thrown
        InjectCreatureRaw(p.actorValue, "basegame", "CreatureHuman.json")


        p.actorValue[AVKey.__PLAYER_QUICKSLOTSEL] = 0
        p.actorValue[AVKey.__ACTION_TIMER] = 0.0
        p.actorValue[AVKey.ACTION_INTERVAL] = ActorHumanoid.BASE_ACTION_INTERVAL
        p.actorValue[AVKey.NAME] = "Cynthia"


        p.makeNewSprite(TextureRegionPack(ModMgr.getGdxFile("basegame", "sprites/test_player_2.tga"), 26, 42))
        p.sprite!!.delay = 0.2f
        p.sprite!!.setRowsAndFrames(1, 1)

        p.setHitboxDimension(15, p.actorValue.getAsInt(AVKey.BASEHEIGHT) ?: ActorHumanoid.BASE_HEIGHT, 9, 0)

        p.setPosition(4096.0 * FeaturesDrawer.TILE_SIZE, 300.0 * FeaturesDrawer.TILE_SIZE)




        p.referenceID = 321321321



        return p
    }


}