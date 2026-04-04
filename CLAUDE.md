# Terrarum

A modular 2D side-scrolling tilemap platformer engine and game, built on LibGDX with Kotlin/Java. GPL-3.0.

## Build & Run

- **IDE**: IntelliJ IDEA (project files: `Terrarum_renewed.iml`, `TerrarumBuild.iml`)
- **JDK**: 21
- **Language**: Kotlin + Java mixed; Kotlin is primary, `App.java` and `FrameBufferManager.java` are Java
- **Dependencies**: All libraries are in `lib/` (fat-jar approach, no Maven/Gradle). Includes LibGDX, GraalVM JS (modified), dyn4j (Vector2 only), custom bitmap font lib
- **Entry point**: `net.torvald.terrarum.Principii.main()` (Java) which launches `App` (the LibGDX `ApplicationListener`)
- **Distribution**: `buildapp/` scripts produce per-platform bundles with jlink'd runtimes
- **Assets**: `assets/` for development, `assets_release/` for distribution. Custom `.tevd` archive format for release assets (`AssetCache.kt`)

## Project Structure

```
src/net/torvald/terrarum/
  App.java                  -- Main application class (LibGDX ApplicationListener). GL thread, render loop, splash screen
  CommonResourcePool.kt     -- Thread-safe GL resource loading with dispatch queues
  ModMgr.kt                 -- Module manager. Scans/loads game modules, provides getGdxFile/getJavaClass
  IngameInstance.kt          -- Base class for game screens (show/hide/render/resize lifecycle)
  Terrarum.kt               -- Game-level singleton (ingame instance management, extension functions)
  GameUpdateGovernor.kt      -- Update/render tick governors (ConsistentUpdateRate, Anarchy)
  MusicService.kt            -- Music playback management

  modulebasegame/
    EntryPoint.kt            -- Basegame module entry point (registers items, fixtures, blocks, weather)
    TitleScreen.kt           -- Title screen (demo world rendering, async loading)
    TerrarumIngame.kt        -- Main gameplay screen
    IngameRenderer.kt        -- World rendering pipeline (FBO composition, lightmap, blur, shadows)
    BuildingMaker.kt         -- Building editor screen

  worlddrawer/
    WorldCamera.kt           -- Camera position tracking (follows player, interpolated movement)
    LightmapRenderer.kt      -- Per-tile RGB+UV light calculation and rasterisation
    BlocksDrawer.kt          -- Tile rendering
    FeaturesDrawer.kt        -- Wire/feature overlay rendering

  gamecontroller/
    IME.kt                   -- Input Method Engine (GraalVM JS keyboard layouts, loaded on daemon thread)

  weather/
    WeatherMixer.kt          -- Weather state machine, skybox rendering
    SkyboxModelHosek.kt      -- Physically-based sky model
```

## Architecture: Threading & GL Dispatch

All OpenGL calls (Texture, ShaderProgram, FrameBuffer creation) **must** happen on the GL thread. The codebase uses a dispatch mechanism to safely create GL resources from background threads.

### CommonResourcePool (the dispatch hub)

```
Background Thread                          GL Thread (App.render)
     |                                          |
     |-- addToLoadingList("name", { Texture() })|
     |-- loadAll()                              |
     |     |                                    |
     |     +-- if on GL thread: run directly    |
     |     +-- if not: enqueue to               |
     |         glDispatchQueue + latch.await()  |
     |                                     CommonResourcePool.update()
     |                                          |-- poll glDispatchQueue
     |                                          |   run loadfun, latch.countDown()
     |                                          |-- poll glRunnableQueue
     |                                          |   run block, latch.countDown()
     |                                          |-- poll slowLoadingQueue (one per tick)
     |                                          |
     +-- latch released, continues <------------+
```

Key methods:
- `loadAll()` -- batch load; blocks background thread until GL thread processes
- `loadAllSlowly()` -- timesliced; one resource per tick via `update()`
- `runOnGLThread { }` -- run arbitrary block on GL thread, blocking caller
- `update()` -- called every tick from `App.render()`, processes all queues
- `getOrPut(name, loadfun, killfun)` -- thread-safe get-or-create with GL dispatch

### App.java Boot Sequence

```
create()
  -> postInit()          [GL thread, synchronous]
       |-- load shaders, fonts, audio device, IME
       |-- CommonResourcePool.setGLThread(currentThread)
       |-- spawn Terrarum-PostInitLoader thread:
       |     ModMgr.invoke()           // module entry points (dispatched to GL via runOnGLThread)
       |     CommonResourcePool.loadAllSlowly()  // timesliced resource loading
       |     loadingThreadDone = true
       +-- return (non-blocking)

render() loop [GL thread]
  while currentScreen == null:
       |-- drawSplash()
       |-- CommonResourcePool.update()   // process GL dispatch queues
       |-- when loadingThreadDone && loaded:
       |     postLoadInit()              // tile atlas, audio mixer, Terrarum.initialise()
       |     setScreen(titleScreen)      // transitions to title screen
```

### TitleScreen Async Loading

`TitleScreen.show()` returns immediately after starting a background thread. The splash screen continues displaying until all loading completes.

```
show()
  |-- quick GL setup (viewport, input processor, FBO, savegame list)
  |-- spawn Terrarum-TitleScreenLoader thread:
  |     load demo world, compute camera nodes, bogoflops, audio reset
  |     backgroundLoadDone = true
  +-- return

renderImpl() [GL thread, every frame]
  if !loadDone:
       |-- App.drawSplash()
       |-- processGLLoadStep()  // state machine, one step per frame:
       |     0: wait for backgroundLoadDone
       |     1: SkyboxModelHosek.loadlut()
       |     2: IngameRenderer.setRenderedWorld + WeatherMixer init
       |     3: load halfgrad texture
       |     4: UIFakeGradOverlay
       |     5: UIFakeBlurOverlay
       |     6: UIRemoCon
       |     7: MusicService.enterScene, gameUpdateGovernor.reset(), loadDone=true
  else:
       normal title screen rendering (world + UI via IngameRenderer)
```

### IME Loading

`IME.kt` object `init {}` spawns a `Terrarum-IMELoader` daemon thread for GraalVM JS context binding and key layout/IME file scanning. Icon texture loading remains synchronous (requires GL thread).

### ModMgr Class Initialisation

`ModMgr.kt` object `init {}` only does metadata loading and class instantiation (fast). The heavy `invoke()` method runs entry points wrapped in `CommonResourcePool.runOnGLThread { }` to avoid GL calls on the wrong thread. This separation prevents `<clinit>` lock deadlocks where a background thread holding the class init lock blocks on a GL dispatch latch while the GL thread tries to access the same class.

## Architecture: Rendering Pipeline

### IngameRenderer

Singleton object. Lazily initialised on first `invoke()` call via `invokeInit()`.

Render path (per frame):
1. `LightmapRenderer.recalculate()` -- every 3 frames, computes per-tile RGBA light values
2. `prepLightmapRGBA()` -- Kawase blur on lightmap into `lightmapFbo`
3. `BlocksDrawer.renderData()` -- prepare tile draw data
4. `drawToRGB()` -- render world tiles + actors into `fboRGB` (with shadow FBOs)
5. `drawToA()` -- render alpha/glow channel
6. Composite: sky -> world -> light multiply -> emissive blend -> vibrancy -> UI
7. Output to `App.renderFBO`, then `TerrarumPostProcessor.draw()` applies final effects

**Critical ordering in `resize()`**: `BlocksDrawer.resize()` and `LightmapRenderer.resize()` must be called **before** `lightmapFbo` creation, because `lightmapFbo` dimensions derive from `LightmapRenderer.lightBuffer` size.

### WorldCamera

Follows an `ActorWithBody` (player or camera actor). Position interpolated per frame. `WorldCamera.x/y` = top-left corner of visible area. `gdxCamX/Y` = centre of visible area. `moveCameraToWorldCoord()` positions the IngameRenderer camera for world-space drawing; `setCameraPosition(0,0)` positions it for screen-space drawing.

### FBO Extension Functions

- `FrameBuffer.inAction(camera, batch) { }` -- bind FBO, set camera to FBO dimensions (Y-down), run block, restore camera to screen dimensions
- `FrameBuffer.inActionF(camera, batch) { }` -- same but Y-up (for flipped FBO content)

Both save/restore `camera.position` and call `setToOrtho` with `App.scr.wf/hf` on exit.

## Key Conventions

- **GL thread safety**: Any code creating `Texture`, `TextureRegion`, `TextureRegionPack`, `ShaderProgram`, `FrameBuffer`, or `Pixmap` must run on the GL thread. Use `CommonResourcePool.runOnGLThread { }` when on a background thread.
- **Kotlin `object` singletons**: First access triggers `<clinit>`. Keep `init {}` blocks fast (no blocking, no GL dispatch). Defer heavy work to explicit `invoke()` methods.
- **`ConcurrentHashMap` cannot store null values**. Use `HashMap` for maps that need nullable values (e.g. `poolKillFun`).
- **`@Volatile`** for cross-thread boolean flags (`loadDone`, `backgroundLoadDone`, etc.)
- **`lateinit var` guards**: Use `::prop.isInitialized` checks in `resize()` and `dispose()` for properties set during async loading steps.
- **`ConsistentUpdateRate`**: Accumulates delta time; may call `updateFunction` multiple times before `renderFunction`. Call `.reset()` before transitioning to active rendering to avoid catch-up storms.
- **Textures**: Use TGA format. Images with semitransparency must be TGA; opaque images may be PNG.
- **Coordinate system**: Y-down (camera `setToOrtho(true, ...)`). `setCameraPosition(0, 0)` places origin at screen top-left, which is not LibGDX nor OpenGL works natively, hence the existence of `FlippingSpriteBatch`. If the user reports renders are flipped vertically, try draw()/drawFlipped() accordingly.
- **ROUNDWORLD**: World wraps horizontally. `WorldCamera.x` uses `fmod worldWidth`. Lightmap and tile drawing account for wrapping.
