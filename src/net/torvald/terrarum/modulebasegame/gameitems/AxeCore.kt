package net.torvald.terrarum.modulebasegame.gameitems

import com.badlogic.gdx.graphics.Color
import net.torvald.terrarum.*
import net.torvald.terrarum.blockproperties.Block
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithBody
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameitems.ItemID
import net.torvald.terrarum.gameitems.mouseInInteractableRangeTools
import net.torvald.terrarum.gameparticles.createRandomBlockParticle
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.modulebasegame.TerrarumIngame
import net.torvald.terrarum.modulebasegame.gameactors.DroppedItem
import net.torvald.terrarum.worlddrawer.CreateTileAtlas
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2023-11-13.
 */
object AxeCore {

    fun startPrimaryUse(
        actor: ActorWithBody, delta: Float, item: GameItem?, mx: Int, my: Int,
        mw: Int = 1, mh: Int = 1, additionalCheckTags: List<String> = listOf()
    ) = mouseInInteractableRangeTools(actor, item) {
        // un-round the mx
        val ww = INGAME.world.width
        val hpww = ww * TerrarumAppConfiguration.TILE_SIZE / 2
        val apos = actor.centrePosPoint
        val mx = if (apos.x - mx * TerrarumAppConfiguration.TILE_SIZE < -hpww) mx - ww
        else if (apos.x - mx * TerrarumAppConfiguration.TILE_SIZE >= hpww) mx + ww
        else mx

        var xoff = -(mw / 2) // implicit flooring
        var yoff = -(mh / 2) // implicit flooring
        // if mw or mh is even number, make it closer toward the actor
        if (mw % 2 == 0 && apos.x > mx * TerrarumAppConfiguration.TILE_SIZE) xoff += 1
        if (mh % 2 == 0 && apos.y > my * TerrarumAppConfiguration.TILE_SIZE) yoff += 1

        var usageStatus = false

        for (oy in 0 until mh) for (ox in 0 until mw) {
            val x = mx + xoff + ox
            val y = my + yoff + oy

            val actorvalue = actor.actorValue
            val tile = INGAME.world.getTileFromTerrain(x, y)

            item?.using = true

            // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
            // return false if hitting actors
            // ** below is commented out -- don't make actors obstruct the way of digging **
            /*var ret1 = true
            INGAME.actorContainerActive.forEach {
                if (it is ActorWBMovable && it.hIntTilewiseHitbox.intersects(mousePoint))
                    ret1 =  false // return is not allowed here
            }
            if (!ret1) return ret1*/


            // check if tile under mouse is a tree
            if (BlockCodex[tile].hasAllTag(listOf("TREE", "TREETRUNK") + additionalCheckTags)) {
                val actionInterval = actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)!!
                val swingDmgToFrameDmg = delta.toDouble() / actionInterval

                INGAME.world.inflictTerrainDamage(
                    x, y,
                    Calculate.pickaxePower(actor, item?.material) * swingDmgToFrameDmg
                ).let { tileBroken ->
                    // tile busted
                    if (tileBroken != null) {
                        // make tree fell by scanning upwards
                        TODO()
                        var drop = ""
                        if (drop.isNotBlank()) {
                            INGAME.queueActorAddition(
                                DroppedItem(
                                    drop,
                                    (x + 0.5) * TerrarumAppConfiguration.TILE_SIZED,
                                    (y + 1.0) * TerrarumAppConfiguration.TILE_SIZED
                                )
                            )
                        }
                        PickaxeCore.makeDust(tile, x, y, 9)
                    }
                    // tile not busted
                    if (Math.random() < actionInterval) {
                        PickaxeCore.makeDust(tile, x, y, 1)
                    }
                }

                usageStatus = usageStatus or true
            }
            // return false if here's no tile
            else {
                usageStatus = usageStatus or false
                continue
            }
        }


        usageStatus
    }

    private val pixelOffs = intArrayOf(2, 7, 12) // hard-coded assuming TILE_SIZE=16
    fun makeDust(tile: ItemID, x: Int, y: Int, density: Int = 9, drawCol: Color = Color.WHITE) {
        val pw = 3
        val ph = 3
        val xo = App.GLOBAL_RENDER_TIMER and 1
        val yo = App.GLOBAL_RENDER_TIMER.ushr(1) and 1

        val renderTag = App.tileMaker.getRenderTag(tile)
        val baseTilenum = renderTag.tileNumber
        val representativeTilenum = when (renderTag.maskType) {
            CreateTileAtlas.RenderTag.MASK_47 -> 17
            CreateTileAtlas.RenderTag.MASK_PLATFORM -> 7
            else -> 0
        }
        val tileNum = baseTilenum + representativeTilenum // the particle won't match the visible tile anyway because of the seasons stuff

        val indices = (0..8).toList().shuffled().subList(0, density)
        for (it in indices) {
            val u = pixelOffs[it % 3]
            val v = pixelOffs[it / 3]
            val pos = Vector2(
                TerrarumAppConfiguration.TILE_SIZED * x + u + xo + 0.5,
                TerrarumAppConfiguration.TILE_SIZED * y + v + yo + 2,
            )
            val veloMult = Vector2(
                1.0 * (if (Math.random() < 0.5) -1 else 1),
                (2.0 - (it / 3)) / 2.0 // 1, 0.5, 0
            )
            createRandomBlockParticle(tileNum, pos, veloMult, u, v, pw, ph).let {
                it.despawnUponCollision = true
                it.drawColour.set(drawCol)
                (Terrarum.ingame as TerrarumIngame).addParticle(it)
            }
        }
    }

    fun endPrimaryUse(actor: ActorWithBody, delta: Float, item: GameItem): Boolean {

        item.using = false
        // reset action timer to zero
        actor.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
        return true
    }

    const val BASE_MASS_AND_SIZE = 10.0 // of iron pick
    const val TOOL_DURABILITY_BASE = 350 // of iron pick

}