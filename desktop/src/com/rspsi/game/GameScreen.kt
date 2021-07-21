package com.rspsi.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g3d.Environment
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.DebugDrawer
import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw
import com.badlogic.gdx.utils.ScreenUtils
import com.displee.cache.CacheLibrary
import com.displee.cache.ProgressListener
import com.kotcrab.vis.ui.VisUI
import com.rspsi.ext.*
import com.rspsi.game.BoundingSphere.Companion.intersects
import com.rspsi.opengl.PixelBufferObject
import ktx.app.KtxGame
import ktx.app.KtxScreen
import ktx.ashley.*
import ktx.assets.disposeSafely
import ktx.async.KtxAsync
import ktx.graphics.use
import ktx.log.info
import ktx.math.plus
import ktx.math.times
import ktx.scene2d.Scene2DSkin
import space.earlygrey.shapedrawer.ShapeDrawer
import java.io.File


open class GameScreen : KtxScreen {

    var rayCastRequest = false
    var mapBoundary: BoundingBox? = null
    var visiblePlane = 0

    var pbo: PixelBufferObject? = null

    lateinit var collisionConfig: btDefaultCollisionConfiguration
    lateinit var dispatcher: btCollisionDispatcher
    lateinit var broadphase: btDbvtBroadphase
    lateinit var constraintSolver: btSequentialImpulseConstraintSolver
    lateinit var dynamicsWorld: btDiscreteDynamicsWorld
    var contactListener: MyContactListener? = null

    lateinit var debugDrawer: DebugDrawer

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
    var transparentObjects = OpenModelCache()
    val compoundModels = mutableListOf<RenderableGameObject>()

    val modelClusters = mutableListOf<GameObjectCluster>()

    fun updateModels() {
        if (dirty) {
            compoundModels.clear()
            terrainInstances.clear()

            terrainInstances.addAll(underlayEntities.map { RenderableGameObject(it) })
            terrainInstances.addAll(overlayEntities.map { RenderableGameObject(it) })

            terrainInstances.forEachIndexed { index, terrain ->
                terrain.apply {
                    buildRenderables()

                    val rs2TileMapComponent = terrain.entity?.get(mapperFor<RS2TileMapComponent>())

                    rs2TileMapComponent?.tileMap?.let { tileMap ->

                        /*simpleBody?.apply {
                            contactCallbackFilter = Companion.OBJECT_FLAG
                            dynamicsWorld.addRigidBody(this)
                        }*/
                        complexBody?.apply {
                            dynamicsWorld.addRigidBody(this)
                        }
                    }

                    entityIndex = Short.MAX_VALUE + index
                }
            }

            val entities = engine.entities
                .filterNot { it.has(mapperFor<RS2TileMapComponent>()) }
                .associateWith { RenderableGameObject(it) }



            compoundModels.addAll(entities.values)
            compoundModels.forEachApply { buildRenderables() }
            CustomRenderableSorter.sort(camera, compoundModels)
            compoundModels.forEachIndexed { index, gameObject -> gameObject.entityIndex = index }


            compoundModels.forEachApply {

                /*simpleBody?.apply {
                    contactCallbackFlag = OBJECT_FLAG
                    dynamicsWorld.addRigidBody(this)
                }*/
                complexBody?.apply {
                    dynamicsWorld.addRigidBody(this)
                }
            }



            buildClusters()


            info { "Added ${compoundModels.size} entities to the render pipeline" }


            dirty = false
        }
    }


    fun buildClusters() {
        compoundModels.forEach { gameObject ->
            val cluster = modelClusters
                .filter { gameObject.type in it.validTypes }
                .firstOrNull { it.instances.values.any { it.boundingSphere.intersects(gameObject.boundingSphere) } }
                ?: GameObjectCluster(camera, RS2Object.Types.getGroup(gameObject.type))

            cluster.instances[gameObject.name] = gameObject
            if (cluster !in modelClusters)
                modelClusters.add(cluster)
        }
        info { "Clustered ${compoundModels.size} game objects into ${modelClusters.size} clusters!" }
        modelClusters.forEachApply { update() }
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

    val terrainInstances = mutableListOf<RenderableGameObject>()
    var underlayEntities: Array<Entity?> = arrayOfNulls(4)
    var overlayEntities: Array<Entity?> = arrayOfNulls(4)

    var gameUI: GameUI? = null
    var keyInputController: MainInputController? = null


    var hoverId = -1
    var backgroundColour = ImmutableColor.YELLOW

    var angle = 90f
    var speed = 90f
    override fun render(delta: Float) {


        //dynamicsWorld.stepSimulation(delta, 1, 1f / 30f)

        compoundModels.forEach { it.update() }
        keyInputController?.update()

        Gdx.gl.glViewport(0, 0, Gdx.graphics.width, Gdx.graphics.height)
        ScreenUtils.clear(backgroundColour.toMutable())
        //Gdx.gl.glFrontFace(GL20.GL_CCW)
        //Gdx.gl.glDisable(GL20.GL_CULL_FACE)


        updateModels()
        getSprites()

        ScreenUtils.clear(backgroundColour.toMutable())

        modelBatch.use(camera) { modelBatch ->


            terrainInstances.forEach { terrainInstance ->
                terrainInstance.update()
                modelBatch.render(terrainInstance, globalEnvironment)
            }

            modelBatch.render(
                compoundModels
                /*modelClusters.filter { it.modelInstances.values.any { isVisible(it) } }*/,
                globalEnvironment
            )

        }


        if (hoverId in compoundModels.indices) {
            modelBatch.use(camera) {
                modelBatch.render(compoundModels[hoverId], highlightedEnvironment)
            }
        }


        debugDrawer.begin(camera)

        keyInputController?.apply {
            dynamicsWorld.debugDrawWorld()
            if (mouseMoved && !dragging) {
                hoverId = raycastResult(mouseX, mouseY)
                info {
                    "Hover id is $hoverId"
                }
                rayCastRequest = false

                mouseMoved = false
            }
        }

        rayTestCB?.apply {
            var rayFrom = Vector3()
            var rayTo = Vector3()
            getRayFromWorld(rayFrom)
            getRayToWorld(rayTo)
            dynamicsWorld.debugDrawer.drawLine(rayFrom.toImmutable(), rayTo.toImmutable(), ImmutableColor.PURPLE)
        }

// dynamicsWorld.debugDrawWorld()
        if (hoverId in compoundModels.indices) {
            compoundModels[hoverId].let {
/*dynamicsWorld.debugDrawObject(
    it.transform,
    it.complexCollisionShape,
    ImmutableColor.GREEN.vec3().toMutable()
)*/
// info { "Attempting to draw bounding sphere ${it.boundingSphere}"}
                dynamicsWorld.debugDrawer.drawSphere(
                    it.boundingSphere.let { it.origin + it.position }.toMutable(),
                    it.boundingSphere.radius,
                    ImmutableColor.RED.vec3().toMutable()
                )
                debugDrawer.drawBox(
                    it.transform.getTranslation().toMutable() + it.boundingBox.min,
                    it.transform.getTranslation().toMutable() + it.boundingBox.max,
                    ImmutableColor.RED.vec3().toMutable()
                )
                debugDrawer.drawBox(
                    (it.transform.getTranslation() + it.boundingBox.getCenterImmutable() - ImmutableVector3(0.1f)).toMutable(),
                    (it.transform.getTranslation() + it.boundingBox.getCenterImmutable() + ImmutableVector3(0.1f)).toMutable(),
                    ImmutableColor.GREEN.vec3().toMutable()
                )
            }
        } else if (hoverId - Short.MAX_VALUE in terrainInstances.indices) {
/*terrainInstances[hoverId - Short.MAX_VALUE].let { gameObject ->
dynamicsWorld.debugDrawObject(
    gameObject.transform,
    gameObject.complexCollisionShape,
    ImmutableColor.GREEN.vec3().toMutable()
)
}*/
        }

/* terrainInstances.forEach { gameObject ->
dynamicsWorld.debugDrawObject(
 gameObject.transform,
 gameObject.collisionShape,
 ImmutableColor.GREEN.vec3().toMutable()
)
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
                "x ${(camera.direction.x)}  | y ${(camera.direction.y)} | z ${(camera.direction.z)} | FPS: ${Gdx.graphics.framesPerSecond} | Gravity: ${dynamicsWorld.gravity}",
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

            dynamicsWorld.rayTest(rayFrom, rayTo, this)
            if (hasHit()) {
//if (!obj.isStaticOrKinematicObject) {
                var body = collisionObject as? btRigidBody
                body?.apply {
                    if (rayCastRequest) {
                        activate()
                        applyCentralImpulse(ray.direction * 3f)
                    }
                    return collisionObject.userValue
                }
// }
            }
        }

        return -1
    }


    var frustumCulling = true
    protected fun isVisible(instance: RenderableGameObject): Boolean {

        if (!frustumCulling)
            return true
        var position = instance.transform.getTranslation()
        position += instance.boundingBox.getCenterImmutable()
        return camera.frustum.sphereInFrustum(position.toMutable(), instance.boundingSphere.radius)
    }

    override fun dispose() {
        clearEntities()
        dynamicsWorld.disposeSafely()
        broadphase.disposeSafely()
        dispatcher.disposeSafely()
        collisionConfig.disposeSafely()
        contactListener.disposeSafely()
        modelBatch.disposeSafely()


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
        compoundModels.forEach { dynamicsWorld.removeRigidBody(it.complexBody) }
        compoundModels.forEach { it.disposeSafely() }
        compoundModels.clear()

        modelClusters.forEach { it.disposeSafely() }
        modelClusters.clear()

        transparentObjects.renderables.clear()
        terrainInstances.forEach { dynamicsWorld.removeRigidBody(it.complexBody) }
        terrainInstances.forEach { it.disposeSafely() }
        terrainInstances.clear()
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
            near = 0.1f
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
        dynamicsWorld.gravity = ImmutableVector3(0, -10f, 0).toMutable()
        dynamicsWorld.latencyMotionStateInterpolation = false

        rayTestCB = ClosestRayResultCallback(ImmutableVector3.ZERO.toMutable(), ImmutableVector3.Z.toMutable())
        debugDrawer = DebugDrawer()
        dynamicsWorld.debugDrawer = debugDrawer
        debugDrawer.debugMode = btIDebugDraw.DebugDrawModes.DBG_DrawNormals

        contactListener = MyContactListener(compoundModels, terrainInstances)
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

                    val mapWidth = landscapeDecoder.width
                    val mapHeight = landscapeDecoder.height
                    for (z in 0 until 4) {
                        underlayEntities[z] = engine.entity {
                            with<NameComponent> {
                                name = "landscape_${regionId}_underlay_$z"
                            }
                            with<WorldPositionComponent> {
                                vector3 = ImmutableVector3(globalX - startX, z, globalY - startY)
                            }
                            with<MeshProviderComponent> {
                                this.provider =
                                    { landscapeDecoder.buildUnderlay(z)?.let { mutableMapOf("underlay_$z" to it) } }
                            }

                            with<WeightComponent> {
                                this.weight = 0f
                            }

                            with<RS2TileMapComponent> {
                                tileMap.putAll(landscapeDecoder.tileMap)
                            }
                            /*with<CollisionShapeComponent> {
                                this.shapeProvider = { _ ->
                                    info { "Collision setup for terrain "}
                                    val tileMap = landscapeDecoder.tileMapaaaaaaaaaaaaaa
                                    val heightBuffer = BufferUtils.newFloatBuffer(mapWidth * mapHeight)
                                    tileMap
                                        .filter { it.key.z == z.toFloat() }
                                        .forEach { (position, tile) ->
                                            heightBuffer.put(
                                                (position.x.toInt() + (position.y.toInt() * mapWidth)),
                                                tile.data.tileHeight
                                            )
                                        }
                                    btHeightfieldTerrainShape(mapWidth, mapHeight, heightBuffer, 1f, 0f, 4096f, 1, false)

                                }
                            }*/

                        }

                    }


                    for (z in 0 until 4) {
                        overlayEntities[z] = engine.entity {
                            with<NameComponent> {
                                name = "landscape_${regionId}_overlay_$z"
                            }
                            with<WorldPositionComponent> {
                                vector3 = ImmutableVector3(globalX - startX, z, globalY - startY)
                            }
                            with<MeshProviderComponent> {
                                this.provider =
                                    { landscapeDecoder.buildOverlay(z)?.let { mutableMapOf("overlay_$z" to it) } }

                            }

                            with<WeightComponent> {
                                this.weight = 0f
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

            with<WeightComponent> {
                this.weight = 0f
            }
            with<MeshProviderComponent> {
                this.provider = {
                    val xyz = modelBuilder.createXYZCoordinates(
                        10f,
                        emptyMaterial,
                        (VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorPacked).toLong()
                    )
                    mutableMapOf("xyzGdx" to modelBuilder.use {
                        xyz.meshes.forEachIndexed { index, mesh ->
                            modelBuilder.part("xyz_$index", mesh, GL20.GL_TRIANGLES, emptyMaterial)
                        }
                    })
                }

            }
        }


        dirtySprites = true
        dirty = true
    }

    companion object {
        val OBJECT_FLAG = 4
        val GROUND_FLAG = 2
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