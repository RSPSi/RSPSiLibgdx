package com.rspsi.game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.physics.bullet.Bullet.obtainStaticNodeShape
import com.badlogic.gdx.physics.bullet.collision.Collision
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape
import com.badlogic.gdx.physics.bullet.collision.btSphereShape
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.FlushablePool
import com.badlogic.gdx.utils.Pool
import com.rspsi.ext.*
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.collections.toGdxArray
import ktx.log.info
import com.badlogic.gdx.utils.Array as GdxArray


open class RenderableGameObject : RenderableProvider, Disposable {

    companion object {
        var simpleMeshPool = object : FlushablePool<Renderable?>() {
            override fun newObject(): Renderable {
                return Renderable()
            }
        }
    }

    var entity: Entity? = null

    constructor(entity: Entity?) {
        this.entity = entity
        entity?.let { parseComponents(entity) }
    }

    constructor()

    var alwaysCollide = false
    var entityIndex = -1
    var forcedMaterial: Material? = null
    var shader: RS2Shader? = null //NOT IMPL

    var scale: ImmutableVector3 = ImmutableVector3(1f)
    val rotation: Quaternion = Quaternion()
    var worldPos: ImmutableVector3 = ImmutableVector3()
    var localPos: ImmutableVector3 = ImmutableVector3()
    var translate: ImmutableVector3 = ImmutableVector3()
    var center: ImmutableVector3 = ImmutableVector3()
    var dimensions: ImmutableVector3 = ImmutableVector3()
    var radius = 0f

    var name: String = ""
    val modelInstances = mutableListOf<ModelInstance>()
    val modelCaches = mutableListOf<ModelCache>()
    var meshProvider: () -> MutableMap<String, Model>? = { null }

    val transform: Matrix4 by lazy { Matrix4().set((worldPos.xy withZ 0).flipYZ() + localPos + translate, rotation, scale) }
    val boundingBox = BoundingBox()

    fun set(renderable: RenderableGameObject) {
        scale = renderable.scale
        rotation.set(renderable.rotation)
        worldPos = renderable.worldPos
        name = renderable.name

        modelInstances.addAll(renderable.modelInstances.map { ModelInstance(it) })
        modelCaches.addAll(renderable.modelCaches)
        calculateBoundary()
        modelsDirty = true
    }

    fun calculateBoundary() {
        boundingBox.inf()

        modelInstances.forEach {
            try {
                for (node in it.nodes) {
                    node.extendBoundingBox(boundingBox, false)
                }
            } catch (ex: Exception) {
                //info { "Failed to get boundary for $this"}
            }
        }
        center = boundingBox.getCenter()
        dimensions = boundingBox.getDimensionsImmutable()
        radius = dimensions.len / 2f
    }

    lateinit var constructInfo: btRigidBody.btRigidBodyConstructionInfo
    var collisionShape: btCollisionShape? = null
    var motionState: MyMotionState? = null
    var renderables = GdxArray<Renderable>()

    private var modelsDirty = false

    fun buildRenderables() {
        renderables.clear()
        val pretransformed = GdxArray<Renderable>()
        val modelInstances = meshProvider()?.map { (name, model) -> ModelInstance(model) } ?: emptyList()
        if(modelInstances.isEmpty()) {
            info {"Returned no model instances"}
        }
        modelInstances.forEach {
            it.getRenderables(pretransformed, simpleMeshPool)
        }
        pretransformed.forEach { renderable ->
            renderable.worldTransform.set(transform)
             renderable.material.set(PickableAttribute(entityIndex))
        }
        collisionShape = try {
            obtainStaticNodeShape(modelInstances.flatMap { it.nodes.map { node ->
                val cpy = node.copy().apply {
                    parts.removeAll { it.meshPart.mesh.numIndices <= 0 || it.meshPart.mesh.numVertices <= 0 }
                }

                if(cpy.parts.size > 0 )
                    cpy
                else null
            }.filterNotNull() }.toGdxArray())
        } catch(ex: Exception) {
            ex.printStackTrace()
            btSphereShape(0.1f)
        }
        collisionShape?.calculateLocalInertia(weight, ImmutableVector3(0f, 10f, 10f).toMutable())

        motionState = MyMotionState(renderables)
        constructInfo = btRigidBody.btRigidBodyConstructionInfo(weight, motionState, collisionShape,  ImmutableVector3(0f, 10f, 10f).toMutable())
        rigidBody = btRigidBody(constructInfo).apply {
            proceedToTransform(transform)
            userValue = entityIndex
            collisionFlags = collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
            this.activate()
        }
        renderables.addAll(pretransformed)

    }

    var weight = 0f

    var rigidBody: btRigidBody? = null

    open fun parseComponents(entity: Entity) {
        val meshComponent = entity[mapperFor<MeshComponent>()]
        val meshProviderComponent = entity[mapperFor<MeshProviderComponent>()]
        val modelCacheComponent = entity[mapperFor<ModelCacheComponent>()]
        val worldPositionComponent = entity[mapperFor<WorldPositionComponent>()]
        val positionComponent = entity[mapperFor<LocalPositionComponent>()]
        val translateComponent = entity[mapperFor<TranslateComponent>()]
        val rotationComponent = entity[mapperFor<RotateComponent>()]
        val scaleComponent = entity[mapperFor<ScaleComponent>()]
        val nameComponent = entity[mapperFor<NameComponent>()]
        val weightComponent = entity[mapperFor<WeightComponent>()]
        val dataComponent = entity[mapperFor<PropertiesComponent>()]


        weightComponent?.weight?.let { weight = it}
        name = nameComponent?.name ?: positionComponent?.vector3.toString()
        meshComponent?.gdxMeshes?.let { meshes -> modelInstances.addAll(meshes.values.map { ModelInstance(it) }) }
        meshProviderComponent?.provider?.let { provider -> meshProvider = provider }
        worldPositionComponent?.let { worldPos = it.vector3 }
        positionComponent?.let { localPos = it.vector3 }
        translateComponent?.let { translate = it.vector3 }
        rotationComponent?.let { rotation.set(it.quaternion()) }
        scaleComponent?.let { scale = it.vector3 }
        modelCacheComponent?.modelCaches?.let { modelCaches.addAll(it) }
        calculateBoundary()

        modelsDirty = true
    }

    override fun getRenderables(renderables: GdxArray<Renderable>, pool: Pool<Renderable>) {
        if(modelsDirty) {
            buildRenderables()
            modelsDirty = false
        }
        renderables.addAll(this.renderables)

    }

    fun update() {
        renderables.forEach { renderable ->
            renderable.worldTransform.set(transform)
        }
    }

    override fun toString(): String {
        return "RenderableGameObject(transform=$transform forcedMaterial=$forcedMaterial, shader=$shader, scale=$scale, rotation=$rotation, worldPos=$worldPos, localPos=$localPos, translate=$translate, center=$center, dimensions=$dimensions, radius=$radius, name='$name', models=$modelInstances, modelCaches=$modelCaches, meshProvider=$meshProvider, boundingBox=$boundingBox)"
    }

    override fun dispose() {
        rigidBody?.dispose()
        constructInfo.dispose()
        motionState?.dispose()
        renderables.forEach { it.meshPart.mesh.dispose() }


    }


}