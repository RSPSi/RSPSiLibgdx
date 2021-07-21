package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.ModelCache.MeshPool
import com.badlogic.gdx.graphics.g3d.ModelCache.SimpleMeshPool
import com.badlogic.gdx.graphics.g3d.model.MeshPart
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter
import com.badlogic.gdx.graphics.g3d.utils.MeshBuilder
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.FlushablePool
import com.badlogic.gdx.utils.GdxRuntimeException
import com.badlogic.gdx.utils.Pool
import ktx.assets.disposeSafely
import ktx.collections.toGdxArray


class KNode {

}
class GameObjectCluster(var camera: Camera? = null, var validTypes: IntRange): RenderableProvider, Disposable {
    val instances = mutableMapOf<String, RenderableGameObject>()


    private val modelCache = OpenModelCache()

    fun update() {
        modelCache.use(camera) {
            instances.values.forEach { gameObject ->
                add(gameObject)
            }
        }

    }

    override fun getRenderables(renderables: Array<Renderable>, pool: Pool<Renderable>) {
        modelCache.getRenderables(renderables, pool)
    }

    override fun dispose() {
        modelCache.disposeSafely()
    }

}
class OpenModelCache
/** Create a ModelCache using the specified [RenderableSorter] and [MeshPool] implementation. The
 * [RenderableSorter] implementation will be called with the camera specified in [.begin]. By default this
 * will be null. The sorter is important for optimizing the cache. For the best result, make sure that renderables that can be
 * merged are next to each other.  */
    (private var sorter: RenderableSorter = DefaultRenderableSorter(), private var meshPool: MeshPool = SimpleMeshPool()): Disposable, RenderableProvider {

    val renderables = ArrayList<Renderable>()

    companion object {
        private val renderablesPool: FlushablePool<Renderable> = object : FlushablePool<Renderable>() {
            override fun newObject(): Renderable {
                return Renderable()
            }
        }
        private val meshPartPool: FlushablePool<MeshPart> = object : FlushablePool<MeshPart>() {
            override fun newObject(): MeshPart {
                return MeshPart()
            }
        }
    }

    private val items = ArrayList<Renderable>()
    private val tmp = ArrayList<Renderable>()

    private var meshBuilder = MeshBuilder()
    var building = false
    private var camera: Camera? = null

    /** Create a ModelCache using the default [Sorter] and the [SimpleMeshPool] implementation. This might not be the
     * most optimal implementation for you use-case, but should be good to start with.  */
    constructor() : this(ModelCache.Sorter(), SimpleMeshPool())

    init {
        meshBuilder = MeshBuilder()
    }

    /** Begin creating the cache, must be followed by a call to [.end], in between these calls one or more calls to one of
     * the add(...) methods can be made. Calling this method will clear the cache and prepare it for creating a new cache. The
     * cache is not valid until the call to [.end] is made. Use one of the add methods (e.g. [.add] or
     * [.add]) to add renderables to the cache.  */
    fun begin() {
        begin(null)
    }

    /** Begin creating the cache, must be followed by a call to [.end], in between these calls one or more calls to one of
     * the add(...) methods can be made. Calling this method will clear the cache and prepare it for creating a new cache. The
     * cache is not valid until the call to [.end] is made. Use one of the add methods (e.g. [.add] or
     * [.add]) to add renderables to the cache.
     * @param camera The [Camera] that will passed to the [RenderableSorter]
     */
    fun begin(camera: Camera?) {
        if (building) throw GdxRuntimeException("Call end() after calling begin()")
        building = true
        this.camera = camera
        renderablesPool.flush()
        renderables.clear()
        items.clear()
        meshPartPool.flush()
        meshPool.flush()
    }

    private fun obtainRenderable(material: Material, primitiveType: Int): Renderable {
        val result = renderablesPool.obtain()
        result.bones = null
        result.environment = null
        result.material = material
        result.meshPart.mesh = null
        result.meshPart.offset = 0
        result.meshPart.size = 0
        result.meshPart.primitiveType = primitiveType
        result.meshPart.center[0f, 0f] = 0f
        result.meshPart.halfExtents[0f, 0f] = 0f
        result.meshPart.radius = -1f
        result.shader = null
        result.userData = null
        result.worldTransform.idt()
        return result
    }

    /** Finishes creating the cache, must be called after a call to [.begin], only after this call the cache will be valid
     * (until the next call to [.begin]). Calling this method will process all renderables added using one of the add(...)
     * methods and will combine them if possible.  */
    fun end() {
        if (!building) throw GdxRuntimeException("Call begin() prior to calling end()")
        building = false
        if (items.size == 0) return
        val sortedItems = items.toGdxArray()
        sorter.sort(camera, sortedItems)
        val itemCount = items.size
        val initCount = renderables.size
        val first = items[0]
        var vertexAttributes = first.meshPart.mesh.vertexAttributes
        var material = first.material
        var primitiveType = first.meshPart.primitiveType
        var offset = renderables.size
        meshBuilder.begin(vertexAttributes)
        var part = meshBuilder.part("", primitiveType, meshPartPool.obtain())
        renderables.add(obtainRenderable(material, primitiveType))
        var i = 0
        val n = sortedItems.size
        while (i < n) {
            val renderable = sortedItems[i]
            val va = renderable.meshPart.mesh.vertexAttributes
            val mat = renderable.material
            val pt = renderable.meshPart.primitiveType
            val sameAttributes = va == vertexAttributes
            val indexedMesh = renderable.meshPart.mesh.numIndices > 0
            val verticesToAdd = if (indexedMesh) renderable.meshPart.mesh.numVertices else renderable.meshPart.size
            val canHoldVertices = meshBuilder.numVertices + verticesToAdd <= MeshBuilder.MAX_VERTICES
            val sameMesh = sameAttributes && canHoldVertices
            val samePart = sameMesh && pt == primitiveType && mat.same(material, true)
            if (!samePart) {
                if (!sameMesh) {
                    val mesh = meshBuilder.end(
                        meshPool.obtain(
                            vertexAttributes, meshBuilder.numVertices,
                            meshBuilder.numIndices
                        )
                    )
                    while (offset < renderables.size) renderables[offset++].meshPart.mesh = mesh
                    meshBuilder.begin(va.also { vertexAttributes = it })
                }
                val newPart = meshBuilder.part("", pt, meshPartPool.obtain())
                val previous = renderables[renderables.size - 1]
                previous.meshPart.offset = part.offset
                previous.meshPart.size = part.size
                part = newPart
                renderables.add(obtainRenderable(mat.also { material = it }, pt.also { primitiveType = it }))
            }
            meshBuilder.setVertexTransform(renderable.worldTransform)
            meshBuilder.addMesh(renderable.meshPart.mesh, renderable.meshPart.offset, renderable.meshPart.size)
            ++i
        }
        val mesh = meshBuilder.end(
            meshPool.obtain(
                vertexAttributes, meshBuilder.numVertices,
                meshBuilder.numIndices
            )
        )
        while (offset < renderables.size) renderables[offset++].meshPart.mesh = mesh
        val previous = renderables[renderables.size - 1]
        previous.meshPart.offset = part.offset
        previous.meshPart.size = part.size
    }

    /** Adds the specified [Renderable] to the cache. Must be called in between a call to [.begin] and [.end].
     * All member objects might (depending on possibilities) be used by reference and should not change while the cache is used. If
     * the [Renderable.bones] member is not null then skinning is assumed and the renderable will be added as-is, by
     * reference. Otherwise the renderable will be merged with other renderables as much as possible, depending on the
     * [Mesh.getVertexAttributes], [Renderable.material] and primitiveType (in that order). The
     * [Renderable.environment], [Renderable.shader] and [Renderable.userData] values (if any) are removed.
     * @param renderable The [Renderable] to add, should not change while the cache is needed.
     */
    fun add(renderable: Renderable) {
        if (!building) throw GdxRuntimeException("Can only add items to the ModelCache in between .begin() and .end()")
        if (renderable.bones == null) items.add(renderable) else renderables.add(renderable)
    }

    /** Adds the specified [RenderableProvider] to the cache, see [.add].  */
    fun add(renderableProvider: RenderableProvider) {
        var renderables = tmp.toGdxArray()
        renderableProvider.getRenderables(renderables, renderablesPool)
        var i = 0
        val n = renderables.size
        while (i < n) {
            add(renderables[i])
            ++i
        }
        tmp.clear()
    }

    /** Adds the specified [RenderableProvider]s to the cache, see [.add].  */
    fun <T : RenderableProvider> add(renderableProviders: Iterable<T>) {
        for (renderableProvider in renderableProviders) add(renderableProvider)
    }

    override fun dispose() {
        if (building) throw GdxRuntimeException("Cannot dispose a ModelCache in between .begin() and .end()")
        meshPool.dispose()
    }

    override fun getRenderables(renderables: Array<Renderable>, pool: Pool<Renderable>) {
        if (building) throw GdxRuntimeException("Cannot render a ModelCache in between .begin() and .end()")

        renderables.addAll(this.renderables.toGdxArray())
    }
}

inline fun <B : OpenModelCache> B.use(camera: Camera? = null, action: B.() -> Unit) {
    begin(camera)
    action(this)
    end()
}