package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.spriteanimation.SheetSpriteAnimation
import net.torvald.terrarum.INGAME
import net.torvald.terrarum.Point2d
import net.torvald.terrarum.TerrarumAppConfiguration
import net.torvald.terrarum.gameactors.AVKey
import net.torvald.terrarum.langpack.Lang
import net.torvald.terrarum.modulebasegame.gameitems.FixtureItemBase
import net.torvald.terrarum.toInt
import net.torvald.terrarumsansbitmap.gdx.TextureRegionPack
import org.dyn4j.geometry.Vector2

/**
 * Created by minjaesong on 2024-03-01.
 */
class FixtureLogicSignalSwitchManual : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_SWITCH"] }
    )

    private val variant = (Math.random() * 8).toInt()
    private var state = false // false = off

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_switch.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_switch_glow.tga")

        density = 1400.0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,8)
            it.currentFrame = variant
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }
        makeNewSpriteGlow(TextureRegionPack(itemImage2.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,8)
            it.currentFrame = variant
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS


        setWireEmitterAt(0, 0, "digital_bit")
    }

    override fun reload() {
        super.reload()

        (sprite as SheetSpriteAnimation).currentFrame = variant
        (spriteGlow as SheetSpriteAnimation).currentFrame = variant

        (sprite as SheetSpriteAnimation).currentRow = state.toInt()
        (spriteGlow as SheetSpriteAnimation).currentRow = state.toInt()
        setWireEmissionAt(0, 0, Vector2(state.toInt().toDouble(), 0.0))
    }

    override fun onInteract(mx: Double, my: Double) {
        state = !state
        (sprite as SheetSpriteAnimation).currentRow = state.toInt()
        setWireEmissionAt(0, 0, Vector2(state.toInt().toDouble(), 0.0))
    }
}

/**
 * Created by minjaesong on 2024-09-27.
 */
class FixtureLogicSignalPushbutton : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = true

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 1, 1),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_PUSHBUTTON"] }
    )

    private var triggeredTime: Long? = null // null = off; number: TIME_T that the button was held down

    private val state: Int
        get() = (triggeredTime != null).toInt()

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_pushbutton.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_pushbutton_emsv.tga")

        density = 1400.0
        setHitboxDimension(TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS


        setWireEmitterAt(0, 0, "digital_bit")
    }

    override fun updateSignal() {
        // decide when to un-trigger
        if (INGAME.world.worldTime.TIME_T - (triggeredTime ?: 0L) >= 60) {
            triggeredTime = null
        }

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
        setWireEmissionAt(0, 0, Vector2(state.toDouble(), 0.0))
    }

    override fun reload() {
        super.reload()

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
        setWireEmissionAt(0, 0, Vector2(state.toDouble(), 0.0))
    }

    override fun onInteract(mx: Double, my: Double) {
        triggeredTime = INGAME.world.worldTime.TIME_T
    }
}

class FixtureLogicSignalPressurePlate : Electric {

    @Transient override val spawnNeedsFloor = true
    @Transient override val spawnNeedsWall = false

    constructor() : super(
        BlockBox(BlockBox.NO_COLLISION, 2, 1),
        nameFun = { Lang["ITEM_LOGIC_SIGNAL_PRESSURE_PLATE"] }
    )

    @Transient open val minMass = 2.0 // different types of switches can have different minimal mass?
    @Transient open val holdTime = 30 // ticks


    private var triggeredTime: Long? = null // null = off; number: TIME_T that the button was held down

    private val state: Int
        get() = (triggeredTime != null).toInt()

    init {
        val itemImage = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_pressure_plate.tga")
        val itemImage2 = FixtureItemBase.getItemImageFromSingleImage("basegame", "sprites/fixtures/signal_pressure_plate_emsv.tga")

        density = 1400.0
        setHitboxDimension(2*TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE, 0, 1)

        makeNewSprite(TextureRegionPack(itemImage.texture, 2*TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }
        makeNewSpriteEmissive(TextureRegionPack(itemImage2.texture, 2*TerrarumAppConfiguration.TILE_SIZE, TerrarumAppConfiguration.TILE_SIZE)).let {
            it.setRowsAndFrames(2,1)
            it.delays = floatArrayOf(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        }

        actorValue[AVKey.BASEMASS] = FixtureLogicSignalEmitter.MASS


        setWireEmitterAt(0, 0, "digital_bit")
        // right side of the plate can be used to route signal wire
    }

    private fun updateState() {
        val pointStart = Point2d(hitbox.startX + 5, hitbox.endY - 6)
        val pointEnd = Point2d(hitbox.startX + 5 + 22, hitbox.endY)

        val massSum = INGAME.getActorsAt(pointStart, pointEnd).filter {
            it.physProp.usePhysics
        }.sumOf { it.mass }

        if (massSum >= minMass)
            triggeredTime = INGAME.world.worldTime.TIME_T

    }

    override fun updateSignal() {
        // detect and measure weight of actors
        updateState()

        // decide when to un-trigger
        if (INGAME.world.worldTime.TIME_T - (triggeredTime ?: 0L) >= holdTime) {
            triggeredTime = null
        }

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
        setWireEmissionAt(0, 0, Vector2(state.toDouble(), 0.0))
    }

    override fun reload() {
        super.reload()

        (sprite as SheetSpriteAnimation).currentRow = state
        (spriteEmissive as SheetSpriteAnimation).currentRow = state
        setWireEmissionAt(0, 0, Vector2(state.toDouble(), 0.0))
    }
}