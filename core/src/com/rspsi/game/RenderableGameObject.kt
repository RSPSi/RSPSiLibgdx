package com.rspsi.game

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.collision.BoundingBox
import com.badlogic.gdx.math.collision.Sphere
import com.badlogic.gdx.physics.bullet.Bullet
import com.badlogic.gdx.physics.bullet.collision.*
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.FlushablePool
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import com.rspsi.ext.*
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.assets.disposeSafely
import ktx.collections.GdxArray
import ktx.collections.toGdxArray
import ktx.log.info

open class BoundingSphere() {

    var position: ImmutableVector3 = ImmutableVector3.ZERO
        set(value) {
            sphere.center.set(value + origin)
            field = value
        }
    var origin: ImmutableVector3 = ImmutableVector3.ZERO
        set(value) {
            sphere.center.set(position + value)
            field = value
        }

    var radius: Float = 0f
        set(value){
            sphere.radius = value
            field = value
        }

    var sphere = Sphere(origin.toMutable(), 0f)

    companion object {
        fun BoundingSphere?.intersects(b: BoundingSphere?): Boolean {
            return if (this != null && b != null) b.sphere.overlaps(sphere) else false
        }
    }



    //https://github.com/zeux/meshoptimizer/blob/3bdc8125430dcdbf8b494844645cdbd16169cc3a/src/clusterizer.cpp#L78
    fun extendBoundary(mesh: Mesh) {
        if (mesh.numVertices <= 0) {
            throw GdxRuntimeException("vertex count > 0 required to calculate spherical boundary")
        }
        val meshRadius = mesh.calculateRadius(origin.toMutable())
        if(meshRadius > radius)
            radius = meshRadius
    }


    fun zero() {
        position = ImmutableVector3.ZERO
        origin = ImmutableVector3.ZERO
        radius = 0f
    }

    override fun toString(): String {
        return "BoundingSphere(position=$position, origin=$origin, radius=$radius)"
    }


}


open class RenderableGameObject : RenderableProvider, Disposable {

    companion object {
        var simpleMeshPool = object : FlushablePool<Renderable?>() {
            override fun newObject(): Renderable {
                return Renderable()
            }
        }
    }

    var id = -1
    var type = -1
    var boundingSphere: BoundingSphere = BoundingSphere()
    var entity: Entity? = null

    constructor(entity: Entity?) {
        this.entity = entity
        entity?.let { parseComponents(entity) }
    }

    constructor()

    var alwaysCollide = false
    var entityIndex = -1
        set(value) {
            renderables.forEach { it.userData = value }
            complexBody?.userValue = value
            field = value
        }
    var forcedMaterial: Material? = null
    var shader: RS2Shader? = null //NOT IMPL

    var scale: ImmutableVector3 = ImmutableVector3(1f)
    val rotation: Quaternion = Quaternion()
    var worldPos: ImmutableVector3 = ImmutableVector3()
    var localPos: ImmutableVector3 = ImmutableVector3()
    var translate: ImmutableVector3 = ImmutableVector3()
    var center: ImmutableVector3 = ImmutableVector3()
    var dimensions: ImmutableVector3 = ImmutableVector3()

    var name: String = ""
    val modelInstances = mutableListOf<ModelInstance>()//TODO
    val modelCaches = mutableListOf<ModelCache>()//TODO
    var meshProvider: () -> MutableMap<String, Model>? = { null }

    val transform: Matrix4 by lazy {
        Matrix4().set(
            (worldPos.xy withZ 0).flipYZ() + localPos + translate,
            rotation,
            scale
        )
    }
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
            it.model.meshes.forEach { it.extendBoundingBox(boundingBox) }
        }
        center = boundingBox.getCenterImmutable()
        dimensions = boundingBox.getDimensionsImmutable()

        boundingSphere.zero()
        boundingSphere.position = transform.getTranslation()
        boundingSphere.origin = center
        boundingSphere.apply {
            modelInstances.forEach {
                it.model.meshes.forEach { extendBoundary(it) }
            }
        }


    }

    lateinit var complexInfo: btRigidBody.btRigidBodyConstructionInfo
    lateinit var simpleInfo: btRigidBody.btRigidBodyConstructionInfo
    var complexCollisionShape: btCollisionShape? = null
    var simpleCollisionShape: btCollisionShape? = null
    var motionState: MyMotionState? = null
    var renderables = GdxArray<Renderable>()

    private var modelsDirty = false

    fun buildRenderables() {
        renderables.clear()
        val pretransformed = GdxArray<Renderable>()

        meshProvider()?.forEach { (name, model) -> modelInstances.add(ModelInstance(model)) }

        if (modelInstances.isEmpty()) {
            info { "Returned no model instances" }
        }

        modelInstances.addAll(modelInstances)

        modelInstances.forEach { instance ->
            instance.getRenderables(pretransformed, simpleMeshPool)

        }

        pretransformed.forEach { renderable ->
            renderable.worldTransform.set(transform)
            renderable.material.set(PickableAttribute(entityIndex))
        }

        renderables.addAll(pretransformed)
        calculateBoundary()



        calculateBulletBodies()

        modelsDirty = false

    }

    fun calculateBulletBodies() {

        entity?.get(mapperFor<CollisionShapeComponent>())?.let {
            complexCollisionShape = it.shapeProvider(modelInstances)
        }

        if (complexCollisionShape == null) {
            complexCollisionShape = try {
                Bullet.obtainStaticNodeShape(modelInstances.flatMap { it.nodes }.toGdxArray())
            } catch (ex: Exception) {
                btSphereShape(boundingSphere.radius)
            }
        }

        complexCollisionShape?.calculateLocalInertia(weight, ImmutableVector3(0f, -10f, 10f).toMutable())

        motionState = MyMotionState(this)
        complexInfo = btRigidBody.btRigidBodyConstructionInfo(
            weight,
            motionState,
            complexCollisionShape,
            ImmutableVector3(0f, -10f, 0f).toMutable()
        )
        complexBody = btRigidBody(complexInfo).apply {
            proceedToTransform(transform)
            friction = 0.9f
            collisionFlags = collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
        }



        if (simpleCollisionShape == null) {
            simpleCollisionShape = try {
                BoxBodyProvider.obtainShape(modelInstances.flatMap { it.nodes }.toGdxArray())
            } catch (ex: Exception) {
                btSphereShape(boundingSphere.radius)
            }
        }

        simpleInfo = btRigidBody.btRigidBodyConstructionInfo(
            weight,
            motionState,
            complexCollisionShape,
            ImmutableVector3(0f, -10f, 0f).toMutable()
        )
        simpleBody = btRigidBody(simpleInfo).apply {
            proceedToTransform(transform.cpy().translate(boundingBox.getHalfExtents()))
            friction = 0.9f
            collisionFlags = collisionFlags or btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK
        }
    }


    var weight = 0f

    var complexBody: btRigidBody? = null
    var simpleBody: btRigidBody? = null

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
        val rs2ObjectComponent = entity[mapperFor<RS2ObjectComponent>()]

        rs2ObjectComponent?.rs2Object?.let {
            this.type = it.type
            this.id = it.id
        }
        weightComponent?.weight?.let { weight = it }
        name = nameComponent?.name ?: positionComponent?.vector3.toString()
        meshComponent?.gdxMeshes?.let { meshes ->
            modelInstances.addAll(meshes.values.map {
                ModelInstance(it)
            })
        }
        meshProviderComponent?.provider?.let { meshProvider = it }
        worldPositionComponent?.let { worldPos = it.vector3 }
        positionComponent?.let { localPos = it.vector3 }
        translateComponent?.let { translate = it.vector3 }
        rotationComponent?.let { rotation.set(it.quaternion()) }
        scaleComponent?.let { scale = it.vector3 }
        modelCacheComponent?.modelCaches?.let { modelCaches.addAll(it) }

        modelsDirty = true
    }

    override fun getRenderables(renderables: GdxArray<Renderable>, pool: Pool<Renderable>) {
        if (modelsDirty) {
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


    override fun dispose() {
        simpleBody.disposeSafely()
        complexBody.disposeSafely()

        complexInfo.disposeSafely()
        simpleInfo.disposeSafely()
        motionState.disposeSafely()
        complexCollisionShape.disposeSafely()
        renderables.forEach { it.meshPart.mesh.disposeSafely() }
        modelInstances.forEach { it.model.disposeSafely() }


    }

    override fun toString(): String {
        return "RenderableGameObject(name='$name', id=$id, type=$type, boundingSphere=$boundingSphere, entityIndex=$entityIndex, scale=$scale, rotation=$rotation, worldPos=$worldPos, localPos=$localPos, translate=$translate, center=$center, dimensions=$dimensions, boundingBox=$boundingBox, weight=$weight)"
    }


}


class ShapePart {
    var parts = GdxArray<MeshPart>()
    var transform = Matrix4()

    companion object {


        private val idt = Matrix4()
        private val tmpM = Matrix4()

        fun getShapeParts(
            node: Node, applyTransform: Boolean, out: MutableList<ShapePart>, offset: Int,
            pool: Pool<ShapePart>
        ) {
            val transform = if (applyTransform) node.localTransform else idt
            if (node.parts.size > 0) {
                var part: ShapePart? = out.firstOrNull { it.transform == transform }

                if (part == null) {
                    part = pool.obtain()
                    part.parts.clear()
                    part.transform.set(transform)
                    out.add(part)
                }
                repeat(node.parts.size) {
                    part?.parts?.add(node.parts[it].meshPart)
                }
            }
            if (node.hasChildren()) {
                val transformed = applyTransform && transform.notEqual(idt)
                val o = if (transformed) out.size else offset
                getShapeParts(node.children, applyTransform, out, o, pool)
                if (transformed) {
                    for (i in o until out.size) {
                        val part = out[i]
                        tmpM.set(part.transform)
                        part.transform.set(transform).mul(tmpM)
                    }
                }
            }
        }

        fun <T : Node> getShapeParts(
            nodes: Iterable<T>,
            applyTransform: Boolean = true,
            out: MutableList<ShapePart>, offset: Int,
            pool: Pool<ShapePart>
        ) {
            for (node in nodes) getShapeParts(node, applyTransform, out, offset, pool)
        }
    }
}

object ShapePartPool : Pool<ShapePart>() {
    override fun newObject(): ShapePart {
        return ShapePart()
    }

}

interface RigidBodyProvider {
    fun obtainShape(node: Node, applyTransform: Boolean = true): btCollisionShape?
    fun obtainShape(nodes: GdxArray<Node>, applyTransform: Boolean = true): btCollisionShape?
}

object GImpactBodyProvider : RigidBodyProvider {

    private val shapePartArray = mutableListOf<ShapePart>()

    override fun obtainShape(nodes: GdxArray<Node>, applyTransform: Boolean): btCollisionShape? {
        ShapePart.getShapeParts(nodes, applyTransform, shapePartArray, 0, ShapePartPool)
        val result = obtainDynamicShape(shapePartArray)
        ShapePartPool.freeAll(shapePartArray.toGdxArray())
        shapePartArray.clear()
        return result
    }

    override fun obtainShape(node: Node, applyTransform: Boolean): btCollisionShape? {
        ShapePart.getShapeParts(node, applyTransform, shapePartArray, 0, ShapePartPool)
        val result = obtainDynamicShape(shapePartArray)
        ShapePartPool.freeAll(shapePartArray.toGdxArray())
        shapePartArray.clear()
        return result
    }

    fun obtainDynamicShape(parts: MutableList<ShapePart>): btCollisionShape? {
        if (parts.size == 0) return null


        if (parts.size == 1 && parts[0].transform.isIdentity()
        ) {
            return btGImpactMeshShape(btTriangleIndexVertexArray.obtain(parts[0].parts))
        }


        val result = btGImpactCompoundShape()
        result.obtain()
        var i = 0
        val n = parts.size
        while (i < n) {
            val shape = btGImpactMeshShape(btTriangleIndexVertexArray.obtain(parts[i].parts))
            result.addChildShape(parts[i].transform, shape)
            shape.release()
            i++
        }
        return result
    }
}

object BoxBodyProvider : RigidBodyProvider {


    private val boundingBox = BoundingBox()
    private val shapePartArray = mutableListOf<ShapePart>()

    override fun obtainShape(node: Node, applyTransform: Boolean): btCollisionShape? {
        ShapePart.getShapeParts(node, applyTransform, shapePartArray, 0, ShapePartPool)
        val result = obtainDynamicShape(shapePartArray)
        ShapePartPool.freeAll(shapePartArray.toGdxArray())
        shapePartArray.clear()
        return result
    }

    /** Obtain a [btCollisionShape] based on the specified nodes, which can be used for a static body but not for a dynamic
     * body. Depending on the specified nodes the result will be either a [btBvhTriangleMeshShape] or a
     * [btCompoundShape] of multiple btBvhTriangleMeshShape's. Where possible, the same btBvhTriangleMeshShape will be reused
     * if multiple nodes use the same (mesh) part. The node transformation (translation and rotation) will be included, but scaling
     * will be ignored.
     * @param nodes The nodes for which to obtain a node, typically this would be: `model.nodes`.
     * @return The obtained shape, if you're using reference counting then you can release the shape when no longer needed.
     */
    override fun obtainShape(nodes: GdxArray<Node>, applyTransform: Boolean): btCollisionShape? {
        ShapePart.getShapeParts(nodes, applyTransform, shapePartArray, 0, ShapePartPool)
        val result = obtainDynamicShape(shapePartArray)
        ShapePartPool.freeAll(shapePartArray.toGdxArray())
        shapePartArray.clear()
        return result
    }

    fun obtainDynamicShape(parts: MutableList<ShapePart>): btCollisionShape? {
        if (parts.size == 0) return null


        if (parts.size == 1 && parts[0].transform.isIdentity()
        ) {
            boundingBox.inf()
            parts[0].parts.forEach { subpart -> subpart.mesh.extendBoundingBox(boundingBox) }
            return btBoxShape(boundingBox.getHalfExtents().toMutable())
        }


        val result = btCompoundShape()
        result.obtain()
        var i = 0
        val n = parts.size
        while (i < n) {
            boundingBox.inf()
            parts[i].parts.forEach { it.mesh.extendBoundingBox(boundingBox) }
            val shape = btBoxShape(boundingBox.getHalfExtents().toMutable())
            result.addChildShape(parts[i].transform, shape)
            shape.release()
            i++
        }
        return result
    }
}


fun Mesh.extendBoundingBox(out: BoundingBox, transform: Matrix4? = null): BoundingBox {
    val count = if (numIndices == 0) numVertices else numIndices
    return extendBoundingBox(out, 0, count, transform)
}