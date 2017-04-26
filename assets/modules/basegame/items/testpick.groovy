
/**
 * Created by SKYHi14 on 2017-04-26.
 */


import net.torvald.point.Point2d
import net.torvald.terrarum.Terrarum
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.gameactors.ActorWithPhysics
import net.torvald.terrarum.itemproperties.Calculate
import net.torvald.terrarum.itemproperties.InventoryItem
import net.torvald.terrarum.itemproperties.Material
import net.torvald.terrarum.tileproperties.Tile
import org.jetbrains.annotations.NotNull
import org.newdawn.slick.GameContainer



static InventoryItem invoke(int id) {
    return new TestPick(id)
}


class TestPick extends InventoryItem {

    int originalID
    int dynamicID
    String originalName = "GROOVY_PICK"
    double baseMass = 10.0
    Double baseToolSize = 10.0
    boolean stackable = true
    int maxDurability = 147
    float durability = maxDurability
    int equipPosition = 9 //EquipPosition.HAND_GRIP
    String inventoryCategory = "tool" //Category.TOOL

    // !! TEST MATERIAL !!
    Material material = new Material(0,0,0,0,0,0,0,0,1,0.0)

    TestPick(int id) {
        originalID = id
        dynamicID = id
        name = "Groovy Pickaxe"
    }

    @Override
    boolean isUnique() {
        return false
    }

    @Override
    boolean isDynamic() {
        return true
    }

    @Override
    boolean primaryUse(@NotNull GameContainer gc, int delta) {
        int mouseTileX = Terrarum.getMouseTileX()
        int mouseTileY = Terrarum.getMouseTileY()

        def mousePoint = new Point2d(mouseTileX, mouseTileY)
        def actorvalue = Terrarum.ingame.player.actorValue

        using = true

        // linear search filter (check for intersection with tilewise mouse point and tilewise hitbox)
        // return false if hitting actors
        Terrarum.ingame.actorContainer.forEach({
            if (it instanceof ActorWithPhysics && it.tilewiseHitbox.intersects(mousePoint))
                return false
        })

        // return false if here's no tile
        if (Tile.AIR == Terrarum.ingame.world.getTileFromTerrain(mouseTileX, mouseTileY))
            return false

        // filter passed, do the job
        double swingDmgToFrameDmg = delta.toDouble() / actorvalue.getAsDouble(AVKey.ACTION_INTERVAL)

        Terrarum.ingame.world.inflictTerrainDamage(
                mouseTileX, mouseTileY,
                Calculate.pickaxePower(Terrarum.ingame.player, material) * swingDmgToFrameDmg
        )

        return true
    }

    @Override
    boolean endPrimaryUse(@NotNull GameContainer gc, int delta) {
        using = false
        // reset action timer to zero
        Terrarum.ingame.player.actorValue.set(AVKey.__ACTION_TIMER, 0.0)
        return true
    }
}

