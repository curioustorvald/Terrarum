package net.torvald.terrarum

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.FrameBuffer

import net.torvald.dataclass.CircularArray
import net.torvald.terrarum.blockproperties.BlockPropUtil
import net.torvald.terrarum.blockstats.BlockStats
import net.torvald.terrarum.concurrent.ThreadParallel
import net.torvald.terrarum.console.*
import net.torvald.terrarum.gameactors.*
import net.torvald.terrarum.gameactors.physicssolver.CollisionSolver
import net.torvald.terrarum.gamecontroller.IngameController
import net.torvald.terrarum.gamecontroller.KeyToggler
import net.torvald.terrarum.gameworld.GameWorld
import net.torvald.terrarum.gameworld.WorldSimulator
import net.torvald.terrarum.weather.WeatherMixer
import net.torvald.terrarum.worlddrawer.BlocksDrawer
import net.torvald.terrarum.worlddrawer.FeaturesDrawer
import net.torvald.terrarum.worlddrawer.LightmapRenderer
import net.torvald.terrarum.worlddrawer.WorldCamera

import java.util.ArrayList
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

import javax.swing.JOptionPane

import com.badlogic.gdx.graphics.OrthographicCamera
import net.torvald.random.HQRNG
import net.torvald.terrarum.gameworld.fmod
import net.torvald.terrarum.ui.*
import net.torvald.terrarum.worldgenerator.RoguelikeRandomiser
import net.torvald.terrarum.worldgenerator.WorldGenerator


/**
 * Created by minjaesong on 2017-06-16.
 */

class Ingame(val batch: SpriteBatch) : Screen {


    private val ACTOR_UPDATE_RANGE = 4096

    lateinit var world: GameWorld
    lateinit var historicalFigureIDBucket: ArrayList<Int>

    /**
     * list of Actors that is sorted by Actors' referenceID
     */
    val ACTORCONTAINER_INITIAL_SIZE = 64
    val PARTICLES_MAX = Terrarum.getConfigInt("maxparticles")
    val actorContainer = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val actorContainerInactive = ArrayList<Actor>(ACTORCONTAINER_INITIAL_SIZE)
    val particlesContainer = CircularArray<ParticleBase>(PARTICLES_MAX)
    val uiContainer = ArrayList<UICanvas>()

    private val actorsRenderBehind = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMiddle = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderMidTop = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)
    private val actorsRenderFront  = ArrayList<ActorWithBody>(ACTORCONTAINER_INITIAL_SIZE)

    lateinit var playableActorDelegate: PlayableActorDelegate // player must exist; use dummy player if there is none (required for camera)
        private set
    inline val player: ActorHumanoid // currently POSSESSED actor :)
        get() = playableActorDelegate.actor

    var screenZoom = 1.0f
    val ZOOM_MAXIMUM = 4.0f
    val ZOOM_MINIMUM = 0.5f

    companion object {
        val lightmapDownsample = 4f //2f: still has choppy look when the camera moves but unnoticeable when blurred


        /** Sets camera position so that (0,0) would be top-left of the screen, (width, height) be bottom-right. */
        fun setCameraPosition(batch: SpriteBatch, camera: Camera, newX: Float, newY: Float) {
            camera.position.set((-newX + Terrarum.HALFW).round(), (-newY + Terrarum.HALFH).round(), 0f)
            camera.update()
            batch.projectionMatrix = camera.combined
        }
    }


    private val worldFBOformat = if (Terrarum.environment == RunningEnvironment.MOBILE) Pixmap.Format.RGBA4444 else Pixmap.Format.RGBA8888
    private val lightFBOformat = Pixmap.Format.RGB888

    var worldDrawFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)
    var worldGlowFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)
    var worldBlendFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)
    // RGB elements of Lightmap for Color  Vec4(R, G, B, 1.0)  24-bit
    private lateinit var lightmapFboA: FrameBuffer
    private lateinit var lightmapFboB: FrameBuffer


    init {
    }



    private val useShader: Boolean = false
    private val shaderProgram = 0

    val KEY_LIGHTMAP_RENDER = Input.Keys.F7



    lateinit var consoleHandler: UICanvas
    lateinit var debugWindow: UICanvas
    lateinit var notifier: UICanvas

    lateinit var uiPieMenu: UICanvas
    lateinit var uiQuickBar: UICanvas
    lateinit var uiInventoryPlayer: UICanvas
    lateinit var uiInventoryContainer: UICanvas
    lateinit var uiVitalPrimary: UICanvas
    lateinit var uiVitalSecondary: UICanvas
    lateinit var uiVitalItem: UICanvas // itemcount/durability of held block or active ammo of held gun. As for the block, max value is 500.

    lateinit var uiWatchBasic: UICanvas
    lateinit var uiWatchTierOne: UICanvas


    // UI aliases
    lateinit var uiAliases: ArrayList<UICanvas>
        private set
    lateinit var uiAlasesPausing: ArrayList<UICanvas>
        private set

    inline val paused: Boolean
        get() = uiAlasesPausing.map { if (it.isOpened) return true else 0 }.isEmpty() // isEmply is always false, which we want
    /**
     * Set to false if UI is opened; set to true  if UI is closed.
     */
    inline val canPlayerControl: Boolean
        get() = !paused // FIXME temporary behab (block movement if the game is paused or paused by UIs)

    var particlesActive = 0
        private set



    private lateinit var ingameUpdateThread: ThreadIngameUpdate
    private lateinit var updateThreadWrapper: Thread
    //private val ingameDrawThread: ThreadIngameDraw // draw must be on the main thread


    var gameInitialised = false
        private set
    var gameFullyLoaded = false
        private set


    private val TILE_SIZEF = FeaturesDrawer.TILE_SIZE.toFloat()

    //////////////
    // GDX code //
    //////////////

    var camera = OrthographicCamera(Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())


    // invert Y
    fun initViewPort(width: Int, height: Int) {
        // Set Y to point downwards
        camera.setToOrtho(true, width.toFloat(), height.toFloat())

        // Update camera matrix
        camera.update()

        // Set viewport to restrict drawing
        Gdx.gl20.glViewport(0, 0, width, height)
    }


    lateinit var gameLoadMode: GameLoadMode
    lateinit var gameLoadInfoPayload: Any

    enum class GameLoadMode {
        CREATE_NEW, LOAD_FROM
    }

    override fun show() {
        //initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)

        // gameLoadMode and gameLoadInfoPayload must be set beforehand!!

        when (gameLoadMode) {
            GameLoadMode.CREATE_NEW -> enter(gameLoadInfoPayload as NewWorldParameters)
            GameLoadMode.LOAD_FROM -> enter(gameLoadInfoPayload as GameSaveData)
        }

        LightmapRenderer.world = this.world
        //BlocksDrawer.world = this.world
        FeaturesDrawer.world = this.world

        gameInitialised = true
    }

    data class GameSaveData(
            val world: GameWorld,
            val historicalFigureIDBucket: ArrayList<Int>,
            val realGamePlayer: ActorHumanoid
    )

    data class NewWorldParameters(
            val width: Int,
            val height: Int,
            val worldGenSeed: Long
            // other worldgen options
    )

    /**
     * Init instance by loading saved world
     */
    private fun enter(gameSaveData: GameSaveData) {
        if (gameInitialised) {
            println("[Ingame] loaded successfully.")
        }
        else {
            LoadScreen.addMessage("Loading world from save")


            world = gameSaveData.world
            historicalFigureIDBucket = gameSaveData.historicalFigureIDBucket
            playableActorDelegate = PlayableActorDelegate(gameSaveData.realGamePlayer)
            addNewActor(player)



            //initGame()
        }
    }

    /**
     * Init instance by creating new world
     */
    private fun enter(worldParams: NewWorldParameters) {
        if (gameInitialised) {
            println("[Ingame] loaded successfully.")
        }
        else {
            LoadScreen.addMessage("${Terrarum.NAME} version ${TerrarumAppLoader.getVERSION_STRING()}")
            LoadScreen.addMessage("Creating new world")


            // init map as chosen size
            world = GameWorld(worldParams.width, worldParams.height)

            // generate terrain for the map
            WorldGenerator.attachMap(world)
            WorldGenerator.SEED = worldParams.worldGenSeed
            WorldGenerator.generateMap()


            historicalFigureIDBucket = ArrayList<Int>()


            RoguelikeRandomiser.seed = HQRNG().nextLong()


            // add new player and put it to actorContainer
            //playableActorDelegate = PlayableActorDelegate(PlayerBuilderSigrid())
            //playableActorDelegate = PlayableActorDelegate(PlayerBuilderTestSubject1())
            //addNewActor(player)


            // test actor
            //addNewActor(PlayerBuilderCynthia())


            //initGame()
        }
    }

    private val ingameController = IngameController(this)

    /** Load rest of the game with GL context */
    fun postInit() {
        //LightmapRenderer.world = this.world
        BlocksDrawer.world = this.world
        //FeaturesDrawer.world = this.world


        Gdx.input.inputProcessor = ingameController


        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)


        // init console window
        consoleHandler = ConsoleWindow()
        consoleHandler.setPosition(0, 0)


        // init debug window
        debugWindow = BasicDebugInfoWindow()
        debugWindow.setPosition(0, 0)

        // init notifier
        notifier = Notification()
        notifier.setPosition(
                (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)




        // >- queue up game UIs that should pause the world -<
        // inventory
        /*uiInventoryPlayer = UIInventory(player,
                width = 900,
                height = Terrarum.HEIGHT - 160,
                categoryWidth = 210,
                toggleKeyLiteral = Terrarum.getConfigInt("keyinventory")
        )*/
        /*uiInventoryPlayer.setPosition(
                -uiInventoryPlayer.width,
                70
        )*/
        uiInventoryPlayer = UIInventoryFull(player,
                toggleKeyLiteral = Terrarum.getConfigInt("keyinventory")
        )
        uiInventoryPlayer.setPosition(0, 0)

        // >- lesser UIs -<
        // quick bar
        uiQuickBar = UIQuickBar()
        uiQuickBar.isVisible = true
        uiQuickBar.setPosition(0, 0)

        // pie menu
        uiPieMenu = UIPieMenu()
        uiPieMenu.setPosition(Terrarum.HALFW, Terrarum.HALFH)

        // vital metre
        // fill in getter functions by
        //      (uiAliases[UI_QUICK_BAR]!!.UI as UIVitalMetre).vitalGetterMax = { some_function }
        //uiVitalPrimary = UIVitalMetre(player, { 80f }, { 100f }, Color.red, 2, customPositioning = true)
        //uiVitalPrimary.setAsAlwaysVisible()
        //uiVitalSecondary = UIVitalMetre(player, { 73f }, { 100f }, Color(0x00dfff), 1) customPositioning = true)
        //uiVitalSecondary.setAsAlwaysVisible()
        //uiVitalItem = UIVitalMetre(player, { null }, { null }, Color(0xffcc00), 0, customPositioning = true)
        //uiVitalItem.setAsAlwaysVisible()

        // basic watch-style notification bar (temperature, new mail)
        uiWatchBasic = UIBasicNotifier(player)
        uiWatchBasic.setAsAlwaysVisible()
        uiWatchBasic.setPosition(Terrarum.WIDTH - uiWatchBasic.width, 0)

        uiWatchTierOne = UITierOneWatch(player)
        uiWatchTierOne.setAsAlwaysVisible()
        uiWatchTierOne.setPosition(Terrarum.WIDTH - uiWatchTierOne.width, uiWatchBasic.height - 2)


        // batch-process uiAliases
        uiAliases = arrayListOf(
                // drawn first
                //uiVitalPrimary,
                //uiVitalSecondary,
                //uiVitalItem,

                uiPieMenu,
                uiQuickBar,
                uiWatchBasic,
                uiWatchTierOne
                // drawn last
        )
        uiAlasesPausing = arrayListOf(
                uiInventoryPlayer,
                //uiInventoryContainer,
                consoleHandler
        )
        uiAlasesPausing.forEach { addUI(it) } // put them all to the UIContainer
        uiAliases.forEach { addUI(it) } // put them all to the UIContainer



        ingameUpdateThread = ThreadIngameUpdate(this)
        updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")


        LightmapRenderer.fireRecalculateEvent()
    }// END enter


    protected var updateDeltaCounter = 0.0
    protected val updateRate = 1.0 / Terrarum.TARGET_INTERNAL_FPS

    private var firstTimeRun = true

    ///////////////
    // prod code //
    ///////////////
    private class ThreadIngameUpdate(val ingame: Ingame): Runnable {
        override fun run() {
            var updateTries = 0
            while (ingame.updateDeltaCounter >= ingame.updateRate) {
                ingame.updateGame(Terrarum.deltaTime)
                ingame.updateDeltaCounter -= ingame.updateRate
                updateTries++

                if (updateTries >= Terrarum.UPDATE_CATCHUP_MAX_TRIES) {
                    break
                }
            }
        }
    }

    override fun render(delta: Float) {
        // Q&D solution for LoadScreen and Ingame, where while LoadScreen is working, Ingame now no longer has GL Context
        // there's still things to load which needs GL context to be present
        if (!gameFullyLoaded) {

            if (gameLoadMode == GameLoadMode.CREATE_NEW) {
                playableActorDelegate = PlayableActorDelegate(PlayerBuilderSigrid())

                // go to spawn position
                player.setPosition(
                        world.spawnX * FeaturesDrawer.TILE_SIZE.toDouble(),
                        world.spawnY * FeaturesDrawer.TILE_SIZE.toDouble()
                )

                addNewActor(player)
            }

            postInit()


            gameFullyLoaded = true
        }





        Gdx.graphics.setTitle(TerrarumAppLoader.GAME_NAME +
                              " — F: ${Gdx.graphics.framesPerSecond} (${Terrarum.TARGET_INTERNAL_FPS})" +
                              " — M: ${Terrarum.memInUse}M / ${Terrarum.memTotal}M / ${Terrarum.memXmx}M"
        )

        // ASYNCHRONOUS UPDATE AND RENDER //


        /** UPDATE CODE GOES HERE */
        updateDeltaCounter += delta



        if (false && Terrarum.getConfigBoolean("multithread")) { // NO MULTITHREADING: camera don't like concurrent modification (jittery actor movements)
            if (firstTimeRun || updateThreadWrapper.state == Thread.State.TERMINATED) {
                updateThreadWrapper = Thread(ingameUpdateThread, "Terrarum UpdateThread")
                updateThreadWrapper.start()

                if (firstTimeRun) firstTimeRun = false
            }
            // else, NOP;
        }
        else {
            var updateTries = 0
            while (updateDeltaCounter >= updateRate) {
                updateGame(delta)
                updateDeltaCounter -= updateRate
                updateTries++

                if (updateTries >= Terrarum.UPDATE_CATCHUP_MAX_TRIES) {
                    break
                }
            }
        }



        /** RENDER CODE GOES HERE */
        renderGame(batch)
    }

    protected fun updateGame(delta: Float) {
        particlesActive = 0


        KeyToggler.update()
        ingameController.update(delta)


        if (!paused) {

            ///////////////////////////
            // world-related updates //
            ///////////////////////////
            BlockPropUtil.dynamicLumFuncTickClock()
            world.updateWorldTime(delta)
            //WorldSimulator(player, delta)
            WeatherMixer.update(delta, player)
            BlockStats.update()
            if (!(CommandDict["setgl"] as SetGlobalLightOverride).lightOverride)
                world.globalLight = WeatherMixer.globalLightNow


            ////////////////////////////
            // camera-related updates //
            ////////////////////////////
            FeaturesDrawer.update(delta)
            WorldCamera.update(world, player)



            ///////////////////////////
            // actor-related updates //
            ///////////////////////////
            repossessActor()

            // determine whether the inactive actor should be activated
            wakeDormantActors()
            // determine whether the actor should keep being activated or be dormant
            KillOrKnockdownActors()
            updateActors(delta)
            particlesContainer.forEach { if (!it.flagDespawn) particlesActive++; it.update(delta) }
            // TODO thread pool(?)
            CollisionSolver.process()
        }


        ////////////////////////
        // ui-related updates //
        ////////////////////////
        uiContainer.forEach { it.update(delta) }
        debugWindow.update(delta)
        notifier.update(delta)

        // update debuggers using javax.swing //
        if (Authenticator.b()) {
            AVTracker.update()
            ActorsList.update()
        }
    }


    private fun renderGame(batch: SpriteBatch) {
        Gdx.gl.glClearColor(.094f, .094f, .094f, 0f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)

        //camera.position.set(-WorldCamera.x.toFloat(), -WorldCamera.y.toFloat(), 0f) // make camara work
        //camera.position.set(0f, 0f, 0f) // make camara work
        //batch.projectionMatrix = camera.combined



        LightmapRenderer.fireRecalculateEvent()



        worldBlendFrameBuffer.inAction(null, null) {
            Gdx.gl.glClearColor(0f,0f,0f,0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        worldDrawFrameBuffer.inAction(null, null) {
            Gdx.gl.glClearColor(0f,0f,0f,0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }
        worldGlowFrameBuffer.inAction(null, null) {
            Gdx.gl.glClearColor(0f,0f,0f,1f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
            Gdx.gl.glEnable(GL20.GL_TEXTURE_2D)
            Gdx.gl.glEnable(GL20.GL_BLEND)
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        }


        fun moveCameraToWorldCoord() {
            // using custom code for camera; this is obscure and tricky
            camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
            camera.update()
            batch.projectionMatrix = camera.combined
        }


        ///////////////////////////
        // draw world to the FBO //
        ///////////////////////////
        processBlur(lightmapFboA, lightmapFboB, LightmapRenderer.DRAW_FOR_RGB)

        worldDrawFrameBuffer.inAction(camera, batch) {


            // draw-with-poly doesn't want to co-op with peasant spriteBatch... (it hides sprites)

            batch.inUse {
                batch.shader = null
                batch.color = Color.WHITE
                blendNormal()
            }



            setCameraPosition(0f, 0f)
            BlocksDrawer.renderWall(batch)



            batch.inUse {
                moveCameraToWorldCoord()
                actorsRenderBehind.forEach { it.drawBody(batch) }
                particlesContainer.forEach { it.drawBody(batch) }

            }



            setCameraPosition(0f, 0f)
            BlocksDrawer.renderTerrain(batch)



            batch.inUse {
                /////////////////
                // draw actors //
                /////////////////
                moveCameraToWorldCoord()
                actorsRenderMiddle.forEach { it.drawBody(batch) }
                actorsRenderMidTop.forEach { it.drawBody(batch) }
                player.drawBody(batch)
                actorsRenderFront.forEach { it.drawBody(batch) }
                // --> Change of blend mode <-- introduced by children of ActorWithBody //


                /////////////////////////////
                // draw map related stuffs //
                /////////////////////////////
            }



            setCameraPosition(0f, 0f)
            BlocksDrawer.renderFront(batch, false)



            batch.inUse {
                // --> blendNormal() <-- by BlocksDrawer.renderFront
                FeaturesDrawer.drawEnvOverlay(batch)




                // mix lighpmap canvas to this canvas (Colors -- RGB channel)
                if (!KeyToggler.isOn(Input.Keys.F6)) { // F6 to disable lightmap draw
                    setCameraPosition(0f, 0f)
                    batch.shader = Terrarum.shaderBayer
                    batch.shader.setUniformf("rcount", 64f)
                    batch.shader.setUniformf("gcount", 64f)
                    batch.shader.setUniformf("bcount", 64f) // de-banding

                    val lightTex = lightmapFboB.colorBufferTexture // A or B? flipped in Y means you chose wrong buffer; use one that works correctly
                    lightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) // blocky feeling for A E S T H E T I C S

                    if (KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) blendNormal()
                    else blendMul()

                    batch.color = Color.WHITE
                    val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF)
                    val yrem = -(WorldCamera.y.toFloat() fmod TILE_SIZEF)
                    batch.draw(lightTex,
                            xrem,
                            yrem,
                            lightTex.width * Ingame.lightmapDownsample, lightTex.height * Ingame.lightmapDownsample
                            //lightTex.width.toFloat(), lightTex.height.toFloat() // for debugging
                    )

                }


                Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // don't know why it is REALLY needed; it really depresses me
                batch.shader = null


                // move camera back to its former position
                // using custom code for camera; this is obscure and tricky
                camera.position.set(WorldCamera.gdxCamX, WorldCamera.gdxCamY, 0f) // make camara work
                camera.update()
                batch.projectionMatrix = camera.combined

            }
        }


        //////////////////////////
        // draw glow to the FBO //
        //////////////////////////
        processBlur(lightmapFboA, lightmapFboB, LightmapRenderer.DRAW_FOR_ALPHA)

        worldGlowFrameBuffer.inAction(camera, batch) {
            batch.inUse {
                batch.shader = null


                batch.color = Color.WHITE
                blendNormal()



                //////////////////////
                // draw actor glows //
                //////////////////////
                moveCameraToWorldCoord()
                actorsRenderBehind.forEach { it.drawGlow(batch) }
                particlesContainer.forEach { it.drawGlow(batch) }
                actorsRenderMiddle.forEach { it.drawGlow(batch) }
                actorsRenderMidTop.forEach { it.drawGlow(batch) }
                player.drawGlow(batch)
                actorsRenderFront.forEach { it.drawGlow(batch) }
                // --> blendNormal() <-- introduced by childs of ActorWithBody //



                // mix lighpmap canvas to this canvas (UV lights -- A channel written on RGB as greyscale image)
                if (!KeyToggler.isOn(Input.Keys.F6)) { // F6 to disable lightmap draw
                    setCameraPosition(0f, 0f)
                    batch.shader = Terrarum.shaderBayer
                    batch.shader.setUniformf("rcount", 64f)
                    batch.shader.setUniformf("gcount", 64f)
                    batch.shader.setUniformf("bcount", 64f) // de-banding

                    val lightTex = lightmapFboB.colorBufferTexture // A or B? flipped in Y means you chose wrong buffer; use one that works correctly
                    lightTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest) // blocky feeling for A E S T H E T I C S

                    if (KeyToggler.isOn(KEY_LIGHTMAP_RENDER)) blendNormal()
                    else blendMul()

                    batch.color = Color.WHITE
                    val xrem = -(WorldCamera.x.toFloat() fmod TILE_SIZEF)
                    val yrem = -(WorldCamera.y.toFloat() fmod TILE_SIZEF)
                    batch.draw(lightTex,
                            xrem,
                            yrem,
                            lightTex.width * Ingame.lightmapDownsample, lightTex.height * Ingame.lightmapDownsample
                            //lightTex.width.toFloat(), lightTex.height.toFloat() // for debugging
                    )

                }


                blendNormal()
            }
        }


        worldBlendFrameBuffer.inAction(camera, batch) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)


            // draw blended world
            val worldTex = worldDrawFrameBuffer.colorBufferTexture // WORLD: light_color must be applied beforehand
            val glowTex = worldGlowFrameBuffer.colorBufferTexture // GLOW: light_uvlight must be applied beforehand

            worldTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            glowTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)

            worldTex.bind(0)
            glowTex.bind(1)


            Terrarum.shaderBlendGlow.begin()
            Terrarum.shaderBlendGlow.setUniformMatrix("u_projTrans", camera.combined)
            Terrarum.shaderBlendGlow.setUniformi("u_texture", 0)
            Terrarum.shaderBlendGlow.setUniformi("tex1", 1)
            Terrarum.fullscreenQuad.render(Terrarum.shaderBlendGlow, GL20.GL_TRIANGLES)
            Terrarum.shaderBlendGlow.end()



            Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0) // don't know why it is REALLY needed; it really depresses me


            batch.inUse {
                batch.color = Color.WHITE
                blendNormal()

                batch.shader = null
            }
        }


        /////////////////////////
        // draw to main screen //
        /////////////////////////
        camera.setToOrtho(true, Terrarum.WIDTH.toFloat(), Terrarum.HEIGHT.toFloat())
        batch.projectionMatrix = camera.combined
        batch.inUse {

            batch.shader = null

            setCameraPosition(0f, 0f)
            batch.color = Color.WHITE
            blendNormal()


            ///////////////////////////
            // draw skybox to screen //
            ///////////////////////////

            WeatherMixer.render(camera, world)




            /////////////////////////////////
            // draw framebuffers to screen //
            /////////////////////////////////



            val blendedTex = worldBlendFrameBuffer.colorBufferTexture
            blendedTex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            batch.color = Color.WHITE
            batch.shader = null
            blendNormal()
            batch.draw(blendedTex, 0f, 0f, blendedTex.width.toFloat(), blendedTex.height.toFloat())





            // an old code.
            /*batch.shader = null
            val worldTex = worldDrawFrameBuffer.colorBufferTexture // WORLD: light_color must be applied beforehand
            val glowTex = worldGlowFrameBuffer.colorBufferTexture // GLOW: light_uvlight must be applied beforehand


            batch.draw(worldTex, 0f, 0f, worldTex.width.toFloat(), worldTex.height.toFloat())*/



            batch.shader = null

            ////////////////////////
            // debug informations //
            ////////////////////////

            blendNormal()
            // draw reference ID if debugWindow is open
            if (debugWindow.isVisible) {

                actorContainer.forEachIndexed { i, actor ->
                    if (actor is ActorWithBody) {
                        batch.color = Color.WHITE
                        Terrarum.fontSmallNumbers.draw(batch,
                                actor.referenceID.toString(),
                                actor.hitbox.startX.toFloat(),
                                actor.hitbox.canonicalY.toFloat() + 4
                        )
                    }
                }
            }
            // debug physics
            if (KeyToggler.isOn(Input.Keys.F11)) {
                actorContainer.forEachIndexed { i, actor ->
                    if (actor is ActorWithPhysics) {
                        /*shapeRenderer.inUse(ShapeRenderer.ShapeType.Line) {
                            shapeRenderer.color = Color(1f, 0f, 1f, 1f)
                            //shapeRenderer.lineWidth = 1f
                            shapeRenderer.rect(
                                    actor.hitbox.startX.toFloat(),
                                    actor.hitbox.startY.toFloat(),
                                    actor.hitbox.width.toFloat(),
                                    actor.hitbox.height.toFloat()
                            )
                        }*/

                        // velocity
                        batch.color = Color.CHARTREUSE//GameFontBase.codeToCol["g"]
                        Terrarum.fontSmallNumbers.draw(batch,
                                "${0x7F.toChar()}X ${actor.externalForce.x}",
                                actor.hitbox.startX.toFloat(),
                                actor.hitbox.canonicalY.toFloat() + 4 + 8
                        )
                        Terrarum.fontSmallNumbers.draw(batch,
                                "${0x7F.toChar()}Y ${actor.externalForce.y}",
                                actor.hitbox.startX.toFloat(),
                                actor.hitbox.canonicalY.toFloat() + 4 + 8 * 2
                        )
                    }
                }
            }
            // fluidmap debug
            if (KeyToggler.isOn(Input.Keys.F4)) {
                WorldSimulator.drawFluidMapDebug(batch)
            }




            /////////////////////////////
            // draw some overlays (UI) //
            /////////////////////////////

            uiContainer.forEach {
                if (it != consoleHandler) {
                    batch.color = Color.WHITE
                    it.render(batch, camera)
                }
            }

            debugWindow.render(batch, camera)
            // make sure console draws on top of other UIs
            consoleHandler.render(batch, camera)
            notifier.render(batch, camera)


            blendNormal()
        }
    }

    fun processBlur(lightmapFboA: FrameBuffer, lightmapFboB: FrameBuffer, mode: Int) {
        val blurIterations = 5 // ideally, 4 * radius; must be even/odd number -- odd/even number will flip the image
        val blurRadius = 4f / lightmapDownsample // (5, 4f); using low numbers for pixel-y aesthetics

        var blurWriteBuffer = lightmapFboA
        var blurReadBuffer = lightmapFboB


        lightmapFboA.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }
        lightmapFboB.inAction(null, null) {
            Gdx.gl.glClearColor(0f, 0f, 0f, 0f)
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        }


        if (mode == LightmapRenderer.DRAW_FOR_RGB) {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    blendNormal(batch)
                    batch.color = Color.WHITE
                    LightmapRenderer.draw(batch, LightmapRenderer.DRAW_FOR_RGB)
                }
            }
        }
        else {
            // initialise readBuffer with untreated lightmap
            blurReadBuffer.inAction(camera, batch) {
                batch.inUse {
                    blendNormal(batch)
                    batch.color = Color.WHITE
                    LightmapRenderer.draw(batch, LightmapRenderer.DRAW_FOR_ALPHA)
                }
            }
        }



        if (!KeyToggler.isOn(Input.Keys.F8)) {
            for (i in 0 until blurIterations) {
                blurWriteBuffer.inAction(camera, batch) {

                    batch.inUse {
                        val texture = blurReadBuffer.colorBufferTexture

                        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)


                        batch.shader = Terrarum.shaderBlur
                        batch.shader.setUniformf("iResolution",
                                blurWriteBuffer.width.toFloat(), blurWriteBuffer.height.toFloat())
                        batch.shader.setUniformf("flip", 1f)
                        if (i % 2 == 0)
                            batch.shader.setUniformf("direction", blurRadius, 0f)
                        else
                            batch.shader.setUniformf("direction", 0f, blurRadius)


                        batch.color = Color.WHITE
                        batch.draw(texture, 0f, 0f)


                        // swap
                        val t = blurWriteBuffer
                        blurWriteBuffer = blurReadBuffer
                        blurReadBuffer = t
                    }
                }
            }
        }
        else {
            blurWriteBuffer = blurReadBuffer
        }
    }



    private fun repossessActor() {
        // check if currently pocessed actor is removed from game
        if (!theGameHasActor(player)) {
            // re-possess canonical player
            if (theGameHasActor(Player.PLAYER_REF_ID))
                changePossession(Player.PLAYER_REF_ID)
            else
                changePossession(0x51621D) // FIXME fallback debug mode (FIXME is there for a reminder visible in ya IDE)
        }
    }

    private fun changePossession(newActor: PlayableActorDelegate) {
        if (!theGameHasActor(player)) {
            throw IllegalArgumentException("No such actor in the game: $newActor")
        }

        playableActorDelegate = newActor
        WorldSimulator(player, Terrarum.deltaTime)
    }

    private fun changePossession(refid: Int) {
        // TODO prevent possessing other player on multiplayer

        if (!theGameHasActor(refid)) {
            throw IllegalArgumentException(
                    "No such actor in the game: $refid (elemsActive: ${actorContainer.size}, " +
                    "elemsInactive: ${actorContainerInactive.size})")
        }

        // take care of old delegate
        playableActorDelegate!!.actor.collisionType = HumanoidNPC.DEFAULT_COLLISION_TYPE
        // accept new delegate
        playableActorDelegate = PlayableActorDelegate(getActorByID(refid) as ActorHumanoid)
        playableActorDelegate!!.actor.collisionType = ActorWithPhysics.COLLISION_KINEMATIC
        WorldSimulator(player, Terrarum.deltaTime)
    }

    /** Send message to notifier UI and toggle the UI as opened. */
    fun sendNotification(msg: Array<String>) {
        (notifier as Notification).sendNotification(msg)
    }

    fun wakeDormantActors() {
        var actorContainerSize = actorContainerInactive.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainerInactive
            val actor = actorContainerInactive[i]
            if (actor is ActorWithBody && actor.inUpdateRange()) {
                activateDormantActor(actor) // duplicates are checked here
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            i++
        }
    }

    /**
     * determine whether the actor should be active or dormant by its distance from the player.
     * If the actor must be dormant, the target actor will be put to the list specifically for them.
     * if the actor is not to be dormant, it will be just ignored.
     */
    fun KillOrKnockdownActors() {
        var actorContainerSize = actorContainer.size
        var i = 0
        while (i < actorContainerSize) { // loop through actorContainer
            val actor = actorContainer[i]
            val actorIndex = i
            // kill actors flagged to despawn
            if (actor.flagDespawn) {
                removeActor(actor)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            // inactivate distant actors
            else if (actor is ActorWithBody && !actor.inUpdateRange()) {
                if (actor !is Projectile) { // if it's a projectile, don't inactivate it; just kill it.
                    actorContainerInactive.add(actor) // naïve add; duplicates are checked when the actor is re-activated
                }
                actorContainer.removeAt(actorIndex)
                actorContainerSize -= 1
                i-- // array removed 1 elem, so we also decrement counter by 1
            }
            i++
        }
    }

    /**
     * Update actors concurrently.
     *
     * NOTE: concurrency for actor updating is currently disabled because of it's poor performance
     */
    fun updateActors(delta: Float) {
        if (false) { // don't multithread this for now, it's SLOWER //if (Terrarum.MULTITHREAD && actorContainer.size > Terrarum.THREADS) {
            val actors = actorContainer.size.toFloat()
            // set up indices
            for (i in 0..Terrarum.THREADS - 1) {
                ThreadParallel.map(
                        i,
                        ThreadActorUpdate(
                                actors.div(Terrarum.THREADS).times(i).roundInt(),
                                actors.div(Terrarum.THREADS).times(i.plus(1)).roundInt() - 1
                        ),
                        "ActorUpdate"
                )
            }

            ThreadParallel.startAll()

            playableActorDelegate?.update(delta)
        }
        else {
            actorContainer.forEach {
                if (it != playableActorDelegate?.actor) {
                    it.update(delta)

                    if (it is Pocketed) {
                        it.inventory.forEach { inventoryEntry ->
                            inventoryEntry.item.effectWhileInPocket(delta)
                            if (it.equipped(inventoryEntry.item)) {
                                inventoryEntry.item.effectWhenEquipped(delta)
                            }
                        }
                    }
                }
            }
            playableActorDelegate?.update(delta)
            //AmmoMeterProxy(player, uiVitalItem.UI as UIVitalMetre)
        }
    }

    fun Double.sqr() = this * this
    fun Int.sqr() = this * this
    fun min(vararg d: Double): Double {
        var ret = Double.MAX_VALUE
        d.forEach { if (it < ret) ret = it }
        return ret
    }
    private fun distToActorSqr(a: ActorWithBody, p: ActorWithBody) =
            min(// take min of normal position and wrapped (x < 0) position
                    (a.hitbox.centeredX - p.hitbox.centeredX).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    (a.hitbox.centeredX - p.hitbox.centeredX + world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr(),
                    (a.hitbox.centeredX - p.hitbox.centeredX - world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.centeredY - p.hitbox.centeredY).sqr()
            )
    private fun distToCameraSqr(a: ActorWithBody) =
            min(
                    (a.hitbox.startX - WorldCamera.x).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr(),
                    (a.hitbox.startX - WorldCamera.x + world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr(),
                    (a.hitbox.startX - WorldCamera.x - world.width * FeaturesDrawer.TILE_SIZE).sqr() +
                    (a.hitbox.startY - WorldCamera.y).sqr()
            )

    /** whether the actor is within screen */
    private fun ActorWithBody.inScreen() =
            distToCameraSqr(this) <=
            (Terrarum.WIDTH.plus(this.hitbox.width.div(2)).
                    times(1 / Terrarum.ingame!!.screenZoom).sqr() +
             Terrarum.HEIGHT.plus(this.hitbox.height.div(2)).
                     times(1 / Terrarum.ingame!!.screenZoom).sqr())


    /** whether the actor is within update range */
    private fun ActorWithBody.inUpdateRange() = distToCameraSqr(this) <= ACTOR_UPDATE_RANGE.sqr()

    /**
     * actorContainer extensions
     */
    fun theGameHasActor(actor: Actor?) = if (actor == null) false else theGameHasActor(actor.referenceID)

    fun theGameHasActor(ID: Int): Boolean =
            isActive(ID) || isInactive(ID)

    fun isActive(ID: Int): Boolean =
            if (actorContainer.size == 0)
                false
            else
                actorContainer.binarySearch(ID) >= 0

    fun isInactive(ID: Int): Boolean =
            if (actorContainerInactive.size == 0)
                false
            else
                actorContainerInactive.binarySearch(ID) >= 0

    fun removeActor(ID: Int) = removeActor(getActorByID(ID))
    /**
     * get index of the actor and delete by the index.
     * we can do this as the list is guaranteed to be sorted
     * and only contains unique values.
     *
     * Any values behind the index will be automatically pushed to front.
     * This is how remove function of [java.util.ArrayList] is defined.
     */
    fun removeActor(actor: Actor) {
        if (actor.referenceID == player.referenceID || actor.referenceID == 0x51621D) // do not delete this magic
            throw RuntimeException("Attempted to remove player.")
        val indexToDelete = actorContainer.binarySearch(actor.referenceID)
        if (indexToDelete >= 0) {
            actorContainer.removeAt(indexToDelete)

            // indexToDelete >= 0 means that the actor certainly exists in the game
            // which means we don't need to check if i >= 0 again
            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        val i = actorsRenderBehind.binarySearch(actor.referenceID)
                        actorsRenderBehind.removeAt(i)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        val i = actorsRenderMiddle.binarySearch(actor.referenceID)
                        actorsRenderMiddle.removeAt(i)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        val i = actorsRenderMidTop.binarySearch(actor.referenceID)
                        actorsRenderMidTop.removeAt(i)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        val i = actorsRenderFront.binarySearch(actor.referenceID)
                        actorsRenderFront.removeAt(i)
                    }
                }
            }
        }
    }

    /**
     * Check for duplicates, append actor and sort the list
     */
    fun addNewActor(actor: Actor) {
        if (theGameHasActor(actor.referenceID)) {
            throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainer.add(actor)
            insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor

            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        actorsRenderBehind.add(actor); insertionSortLastElemAV(actorsRenderBehind)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        actorsRenderMiddle.add(actor); insertionSortLastElemAV(actorsRenderMiddle)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        actorsRenderMidTop.add(actor); insertionSortLastElemAV(actorsRenderMidTop)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        actorsRenderFront.add(actor); insertionSortLastElemAV(actorsRenderFront)
                    }
                }
            }
        }
    }

    fun activateDormantActor(actor: Actor) {
        if (!isInactive(actor.referenceID)) {
            if (isActive(actor.referenceID))
                throw Error("The actor $actor is already activated")
            else
                throw Error("The actor $actor already exists in the game")
        }
        else {
            actorContainerInactive.remove(actor)
            actorContainer.add(actor)
            insertionSortLastElem(actorContainer) // we can do this as we are only adding single actor

            if (actor is ActorWithBody) {
                when (actor.renderOrder) {
                    Actor.RenderOrder.BEHIND -> {
                        actorsRenderBehind.add(actor); insertionSortLastElemAV(actorsRenderBehind)
                    }
                    Actor.RenderOrder.MIDDLE -> {
                        actorsRenderMiddle.add(actor); insertionSortLastElemAV(actorsRenderMiddle)
                    }
                    Actor.RenderOrder.MIDTOP -> {
                        actorsRenderMidTop.add(actor); insertionSortLastElemAV(actorsRenderMidTop)
                    }
                    Actor.RenderOrder.FRONT  -> {
                        actorsRenderFront.add(actor); insertionSortLastElemAV(actorsRenderFront)
                    }
                }
            }
        }
    }

    fun addParticle(particle: ParticleBase) {
        particlesContainer.add(particle)
    }

    fun addUI(ui: UICanvas) {
        // check for exact duplicates
        if (uiContainer.contains(ui)) {
            throw IllegalArgumentException(
                    "Exact copy of the UI already exists: The instance of ${ui.javaClass.simpleName}"
            )
        }

        uiContainer.add(ui)
    }

    fun getActorByID(ID: Int): Actor {
        if (actorContainer.size == 0 && actorContainerInactive.size == 0)
            throw IllegalArgumentException("Actor with ID $ID does not exist.")

        var index = actorContainer.binarySearch(ID)
        if (index < 0) {
            index = actorContainerInactive.binarySearch(ID)

            if (index < 0) {
                JOptionPane.showMessageDialog(
                        null,
                        "Actor with ID $ID does not exist.",
                        null, JOptionPane.ERROR_MESSAGE
                )
                throw IllegalArgumentException("Actor with ID $ID does not exist.")
            }
            else
                return actorContainerInactive[index]
        }
        else
            return actorContainer[index]
    }

    private fun insertionSortLastElem(arr: ArrayList<Actor>) {
        lock(ReentrantLock()) {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j] > x) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }
    private fun insertionSortLastElemAV(arr: ArrayList<ActorWithBody>) { // out-projection doesn't work, duh
        lock(ReentrantLock()) {
            var j = arr.lastIndex - 1
            val x = arr.last()
            while (j >= 0 && arr[j] > x) {
                arr[j + 1] = arr[j]
                j -= 1
            }
            arr[j + 1] = x
        }
    }

    private fun ArrayList<*>.binarySearch(actor: Actor) = this.binarySearch(actor.referenceID)

    private fun ArrayList<*>.binarySearch(ID: Int): Int {
        // code from collections/Collections.kt
        var low = 0
        var high = this.size - 1

        while (low <= high) {
            val mid = (low + high).ushr(1) // safe from overflows

            val midVal = get(mid)!!

            if (ID > midVal.hashCode())
                low = mid + 1
            else if (ID < midVal.hashCode())
                high = mid - 1
            else
                return mid // key found
        }
        return -(low + 1)  // key not found
    }

    inline fun lock(lock: Lock, body: () -> Unit) {
        lock.lock()
        try {
            body()
        }
        finally {
            lock.unlock()
        }
    }

    override fun pause() {
        // TODO no pause when off-focus on desktop
    }

    override fun resume() {
    }

    override fun hide() {
        dispose()
    }



    private var lightmapInitialised = false // to avoid nullability of lightmapFBO

    /**
     * @param width same as Terrarum.WIDTH
     * @param height same as Terrarum.HEIGHT
     * @see net.torvald.terrarum.Terrarum
     */
    override fun resize(width: Int, height: Int) {

        BlocksDrawer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)
        LightmapRenderer.resize(Terrarum.WIDTH, Terrarum.HEIGHT)

        worldDrawFrameBuffer.dispose()
        worldDrawFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)
        worldGlowFrameBuffer.dispose()
        worldGlowFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)
        worldBlendFrameBuffer.dispose()
        worldBlendFrameBuffer = FrameBuffer(worldFBOformat, Terrarum.WIDTH, Terrarum.HEIGHT, false)

        if (lightmapInitialised) {
            lightmapFboA.dispose()
            lightmapFboB.dispose()
        }
        lightmapFboA = FrameBuffer(
                lightFBOformat,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                false
        )
        lightmapFboB = FrameBuffer(
                lightFBOformat,
                LightmapRenderer.lightBuffer.width * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                LightmapRenderer.lightBuffer.height * LightmapRenderer.DRAW_TILE_SIZE.toInt(),
                false
        )
        lightmapInitialised = true // are you the first time?


        // Set up viewport when window is resized
        initViewPort(Terrarum.WIDTH, Terrarum.HEIGHT)



        if (gameInitialised) {
            LightmapRenderer.fireRecalculateEvent()
        }


        if (gameFullyLoaded) {
            // resize UIs

            notifier.setPosition(
                    (Terrarum.WIDTH - notifier.width) / 2, Terrarum.HEIGHT - notifier.height)

            // inventory
            /*uiInventoryPlayer =
                    UIInventory(player,
                            width = 840,
                            height = Terrarum.HEIGHT - 160,
                            categoryWidth = 210
                    )*/


            // basic watch-style notification bar (temperature, new mail)
            uiWatchBasic.setPosition(Terrarum.WIDTH - uiWatchBasic.width, 0)
            uiWatchTierOne.setPosition(Terrarum.WIDTH - uiWatchTierOne.width, uiWatchBasic.height - 2)
        }


        println("[Ingame] Resize event")
    }

    override fun dispose() {
        worldDrawFrameBuffer.dispose()
        worldGlowFrameBuffer.dispose()
        worldBlendFrameBuffer.dispose()
        lightmapFboA.dispose()
        lightmapFboB.dispose()

        actorsRenderBehind.forEach { it.dispose() }
        actorsRenderMiddle.forEach { it.dispose() }
        actorsRenderMidTop.forEach { it.dispose() }
        actorsRenderFront.forEach { it.dispose() }

        uiAliases.forEach { it.dispose() }
        uiAlasesPausing.forEach { it.dispose() }
    }


    /**
     * WARNING! this function flushes batch; use this sparingly!
     *
     * Camera will be moved so that (newX, newY) would be sit on the top-left edge.
     */
    fun setCameraPosition(newX: Float, newY: Float) {
        Ingame.setCameraPosition(batch, camera, newX, newY)
    }
}
