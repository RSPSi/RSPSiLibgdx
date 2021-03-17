package com.rspsi.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.utils.ScreenUtils
import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import com.kotcrab.vis.ui.VisUI
import com.rspsi.ext.*
import com.rspsi.opengl.PixelBufferObject
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.ashley.*
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.log.info
import ktx.scene2d.Scene2DSkin
import space.earlygrey.shapedrawer.ShapeDrawer
import java.io.File


open class GameScreen : KtxScreen {

    var mapBoundary: BoundingBox? = null
    var visiblePlane = 0

    var pbo: PixelBufferObject? = null

    lateinit var collisionConfig: btDefaultCollisionConfiguration
    lateinit var dispatcher: btCollisionDispatcher
    lateinit var broadphase: btDbvtBroadphase
    lateinit var constraintSolver: btSequentialImpulseConstraintSolver
    lateinit var dynamicsWorld: btDiscreteDynamicsWorld
    lateinit var contactListener: MyContactListener

    val engine: Engine = context.inject()
    val spriteBatch = SpriteBatch()
    var globalEnvironment = Environment()
    var highlightedEnvironment = Environment()
    var camera = PerspectiveCamera()
    val modelBatch = ModelBatch(DefaultShaderProvider(), MaterialSorter())

    var directionalLight = DirectionalLight()

    val shapeRenderer = ShapeDrawer(spriteBatch, emptyTextureRegion())

    init {

        context.bindSingleton { this }


    }

    val modelBuilder = ModelBuilder()
    val emptyMaterial = Material(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))

    fun parseEntities(): List<RenderableGameObject> = engine.entities.map { RenderableGameObject(it) }

    var font = BitmapFont()
    var dirty = false
    var transparentObjects = ModelCache()
    val compoundModels = mutableListOf<RenderableGameObject>()


    fun updateModels() {
        if (dirty) {
            compoundModels.clear()
            terrainInstances =
                ((0 until 4).map { z -> RenderableGameObject(underlayEntities[z]) }
                        + (0 until 4).map { z -> RenderableGameObject(overlayEntities[z]) }
                        ).toTypedArray()

            dynamicsWorld.let {
                terrainInstances.forEach { terrain ->
                    terrain?.rigidBody?.apply {
                        collisionFlags = collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
                        contactCallbackFilter = 0
                        contactCallbackFlag = Companion.GROUND_FLAG
                        activationState = Collision.DISABLE_DEACTIVATION
                        dynamicsWorld.addRigidBody(this)
                    }
                }
            }
            val entities = engine.entities
                .filterNot { it.has(mapperFor<RS2TileMapComponent>()) }
                .associateWith { RenderableGameObject(it) }



            compoundModels.addAll(entities.values)
            compoundModels.forEachIndexed { index, gameObject -> gameObject.entityIndex = index }

            transparentObjects.use(camera) {
                add(entities.values)
            }

            compoundModels.forEach {
                it.rigidBody?.apply {
                    info {
                        "Adding rigid body to world!"
                    }
                    dynamicsWorld.addRigidBody(this)
                    contactCallbackFlag = Companion.OBJECT_FLAG
                    contactCallbackFilter = Companion.GROUND_FLAG
                }
            }




            info { "Added ${compoundModels.size} entities to the render pipeline" }


            dirty = false
        }
    }


    var cachedSprites = arrayOf<Sprite>()

    var dirtySprites = false
    fun getSprites(): Array<Sprite> {
        if (!dirtySprites) {
            return cachedSprites
        }

        val landscapeDecoder: RS2LandscapeDecoder = context.inject()
        val textureProvider: RS2TextureProvider = context.inject()

        val textureAtlas = textureProvider.buildTextureAtlas()

        cachedSprites = textureAtlas.createSprites().toArray()
        cachedSprites.forEach { it.setScale(0.1f, 0.1f) }

        dirtySprites = false
        return cachedSprites
    }

    var terrainInstances: Array<RenderableGameObject?> = arrayOfNulls(8)
    var underlayEntities: Array<Entity?> = arrayOfNulls(4)
    var overlayEntities: Array<Entity?> = arrayOfNulls(4)

    var gameUI: GameUI? = null
    var keyInputController: MainInputController? = null


    var hoverId = -1
    var backgroundColour = ImmutableColor.YELLOW

    override fun render(delta: Float) {


        dynamicsWorld.stepSimulation(delta, 5, 1f / 60f)
        compoundModels.forEach { it.update() }
        keyInputController?.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        ScreenUtils.clear(backgroundColour.toMutable())
        Gdx.gl.glFrontFace(GL20.GL_CCW)
        Gdx.gl.glDisable(GL20.GL_CULL_FACE)


        updateModels()
        getSprites()

        ScreenUtils.clear(backgroundColour.toMutable())

        modelBatch.use(camera) { modelBatch ->


            terrainInstances.forEach { terrainInstance ->
                terrainInstance?.let {

                    modelBatch.render(it, globalEnvironment)
                }
            }
            modelBatch.render(transparentObjects, globalEnvironment)

        }


        if (hoverId in 0 until compoundModels.size) {
            modelBatch.use(camera) {
                modelBatch.render(compoundModels[hoverId], highlightedEnvironment)
            }
        }


        debugDrawer.begin(camera)

        keyInputController?.apply {
           // dynamicsWorld.debugDrawWorld()
            if (mouseMoved && !dragging) {
                hoverId = raycastResult(mouseX, mouseY)
                info {
                    "Hover id is $hoverId"
                }
                rayCastRequest = false

                mouseMoved = false
            }
        }

        /*rayTestCB?.apply {
            var rayFrom = Vector3()
            var rayTo = Vector3()
            getRayFromWorld(rayFrom)
            getRayToWorld(rayTo)
            dynamicsWorld.debugDrawer.drawLine(rayFrom.toImmutable(), rayTo.toImmutable(), ImmutableColor.PURPLE)
        }*/
        debugDrawer.end()

        spriteBatch.use {

            font.draw(
                spriteBatch,
                "x ${camera.position.x.toInt()}  | y ${camera.position.y.toInt()} | z ${camera.position.z.toInt()}",
                camera.viewportWidth - 140f,
                camera.viewportHeight - 30f
            )
            font.draw(
                spriteBatch,
                "x ${(camera.direction.x)}  | y ${(camera.direction.y)} | z ${(camera.direction.z)} | FPS: ${Gdx.graphics.framesPerSecond}",
                10f,
                camera.viewportHeight - 80f
            )

            keyInputController?.apply {
                shapeRenderer.setColor(Color.BLACK)
                shapeRenderer.filledRectangle(mouseX.toFloat() - 3f, Gdx.graphics.height - mouseY.toFloat(), 6f, 1f)
                shapeRenderer.filledRectangle(mouseX.toFloat(), Gdx.graphics.height - mouseY.toFloat() - 3f, 1f, 6f)

            }
        }


        gameUI?.draw(delta)


    }



    var rayTestCB: ClosestRayResultCallback? = null
    fun raycastResult(mouseX: Int, mouseY: Int): Int {



        var ray = camera.getPickRay(mouseX.toFloat(), mouseY.toFloat())//TODO Make ImmutableRay
        var rayFrom = ray.origin.cpy()
        var rayTo = (ray.direction.cpy() * 50f) + rayFrom

        rayTestCB?.apply {
            collisionObject = null
            closestHitFraction = 1f
            setRayFromWorld(rayFrom)
            setRayToWorld(rayTo)

            dynamicsWorld.collisionWorld.rayTest(rayFrom, rayTo, this)
            if (hasHit()) {
                var obj = collisionObject
                //if (!obj.isStaticOrKinematicObject) {
                    var body = obj as? btRigidBody
                    body?.apply {
                        activate()
                        applyCentralImpulse(ray.direction / 20f)
                        return obj.userValue
                    }
               // }
            }
        }

            return -1
    }



    var frustumCulling = false
    private var position = ImmutableVector3()
    protected fun isVisible(cam: Camera, instance: RenderableGameObject): Boolean {

        if (!frustumCulling)
            return true
        instance.transform.getTranslation()
        position += instance.boundingBox.getCenter()
        return cam.frustum.boundsInFrustum(position, instance.boundingBox.getDimensions())
    }

    override fun dispose() {
        clearEntities()
        dynamicsWorld.dispose()
        broadphase.dispose()
        dispatcher.dispose()
        collisionConfig.dispose()
        contactListener.dispose()
        modelBatch.dispose()

    }

    override fun resize(width: Int, height: Int) {
        info {
            "Resized to $width $height"
        }
        Gdx.gl.glViewport(0, 0, width, height)
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()

        gameUI?.resize(width, height)
    }


    fun clearEntities() {
        compoundModels.forEach { it.dispose() }

        engine.removeAllEntities()
    }

    fun setupEnvironment() {


        directionalLight.set(0.6f, 0.6f, 0.6f, -10f, -50f, -10f)
        globalEnvironment.set(ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f))
        globalEnvironment.add(directionalLight)


        highlightedEnvironment.set(ColorAttribute(ColorAttribute.AmbientLight, 0f, 0f, 0.6f, 0.5f))
        highlightedEnvironment.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 0.5f))
        //globalEnvironment.add(shadowLight)

    }

    fun setupCamera() {
        camera.apply {
            viewportWidth = Gdx.graphics.width.toFloat()
            viewportHeight = Gdx.graphics.height.toFloat()
            fieldOfView = 67f
            near = 0.5f
            far = 2000f
            update()
        }

    }

    fun setupPhysics() {
        collisionConfig = btDefaultCollisionConfiguration()
        dispatcher = btCollisionDispatcher(collisionConfig)
        broadphase = btDbvtBroadphase()
        constraintSolver = btSequentialImpulseConstraintSolver()
        dynamicsWorld = btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig)
        dynamicsWorld.gravity = ImmutableVector3(0, 10f, 0).toMutable()

        contactListener = MyContactListener(compoundModels)
    }

    fun setup() {

        setupEnvironment()
        setupCamera()
        setupPhysics()

        pbo = PixelBufferObject(Gdx.graphics.width, Gdx.graphics.height, Pixmap.Format.RGBA8888)
        pbo?.bind()

        info { "Supports: ${Gdx.graphics.supportsExtension("GL_ARB_pixel_buffer_object")}" }

        val fontManager = FontManager()
        fontManager.loadFonts()

        val varbitDecoder = RS2VarbitDecoder()
        val areaDecoder = RS2AreaDecoder()

        varbitDecoder.decode()
        areaDecoder.decode()

        val xteaProvider = XTEAProvider()
        val spriteDecoder = RS2SpriteProvider()
        val textureProvider = RS2TextureProvider()


        val modelDecoder = RS2ModelDecoder()
        val objectDefLoader = OSRSObjectDef()
        val floorDecoder = RS2FloorDecoder()
        val objectsDecoder = RS2ObjectsDecoder()
        val landscapeDecoder = RS2LandscapeDecoder()


        val chatManager = ChatManager()
        gameUI = GameUI(camera)



        keyInputController = MainInputController(camera)

        Gdx.input.inputProcessor = keyInputController

        spriteDecoder.load()
        loadTextures()

        floorDecoder.load()
        xteaProvider.decode(File("C:/Users/James/Desktop/RS/193/xteas.json"))

        chatManager.init()
        gameUI?.init()

    }

    fun loadTextures() {
        val textureProvider: RS2TextureProvider = context.inject()
        textureProvider.load()
    }

    fun loadRegion() {

        clearEntities()

        val cacheLibrary: CacheLibrary = context.inject()
        val landscapeDecoder: RS2LandscapeDecoder = context.inject()
        val objectsDecoder: RS2ObjectsDecoder = context.inject()
        val textureProvider: RS2TextureProvider = context.inject()
        val xteaProvider: XTEAProvider = context.inject()

        val sizeX = 1
        val sizeY = 1

        val startX = 3088
        val startY = 3488

        for (globalX in startX until startX + (64 * sizeX) step 64)
            for (globalY in startY until startY + (64 * sizeY) step 64) {
                val regionId = (globalX / 64 shl 8) or (globalY / 64)

                val landscapeDecoded = landscapeDecoder.request(regionId)

                if (landscapeDecoded) {

                    for (height in 0 until 4) {
                        underlayEntities[height] = engine.entity {
                            with<NameComponent> {
                                name = "landscape_${regionId}_underlay_$height"
                            }
                            with<WorldPositionComponent> {
                                vector3 = ImmutableVector3(globalX - startX, 0f, globalY - startY)
                            }
                            with<MeshProviderComponent> {
                                this.provider =
                                    { mutableMapOf("underlay_$height" to landscapeDecoder.buildUnderlay(height)) }
                            }

                            with<RS2TileMapComponent> {
                                tileMap.putAll(landscapeDecoder.tileMap)
                            }
                        }

                    }


                    for (height in 0 until 4) {
                        overlayEntities[height] = engine.entity {
                            with<NameComponent> {
                                name = "landscape_${regionId}_overlay_$height"
                            }
                            with<WorldPositionComponent> {
                                vector3 = ImmutableVector3(globalX - startX, 0f, globalY - startY)
                            }
                            with<MeshProviderComponent> {
                                this.provider =
                                    { mutableMapOf("overlay_$height" to landscapeDecoder.buildOverlay(height)) }
                            }


                            with<RS2TileMapComponent> {
                                tileMap.putAll(landscapeDecoder.tileMap)
                            }
                        }
                    }


                }
                objectsDecoder.request(regionId)
            }

        engine.entity {
            with<NameComponent> {
                name = "xyzAxis"
            }
            with<WorldPositionComponent>()
            with<RotateComponent>()
            with<MeshComponent> {
                val xyz = modelBuilder.createXYZCoordinates(
                    10f,
                    emptyMaterial,
                    (VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorPacked).toLong()
                )
                this.gdxMeshes["xyzGdx"] = modelBuilder.use {
                    xyz.meshes.forEachIndexed { index, mesh ->
                        modelBuilder.part("xyz_$index", mesh, GL20.GL_TRIANGLES, emptyMaterial)
                    }
                }
            }
        }


        dirtySprites = true
        dirty = true
    }

    companion object {
        val OBJECT_FLAG = (1 shl 9)
        val ALL_FLAG = -1
        val GROUND_FLAG = (1 shl 8)
    }

}


class GameView : KtxGame<Screen>() {

    val engine = Engine()

    init {
        context.bindSingleton(engine)

        val progressListener = object : ProgressListener {
            override fun notify(progress: Double, message: String?) {
                // info {
                //    "$progress $message"
                //}
            }
        }
        val cachePath = "C:/Users/James/Desktop/RS/193/cache/"
        context.bindSingleton(CacheLibrary(cachePath, false, progressListener))

    }

    override fun create() {

        Bullet.init(false, true)
        VisUI.load(VisUI.SkinScale.X2)
        Scene2DSkin.defaultSkin = VisUI.getSkin()
        KtxAsync.initiate()

        val gameScreen = GameScreen()
        info { "eat me" }


        addScreen(gameScreen)
        setScreen<GameScreen>()

        gameScreen.setup()

        Gdx.gl.glEnable(GL20.GL_CULL_FACE)
        Gdx.gl.glCullFace(GL20.GL_BACK)
    }
}