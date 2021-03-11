package com.rspsi.ext

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelBatch
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.model.Node
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.collision.BoundingBox
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract


fun emptyTextureRegion(): TextureRegion {
    val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
    pixmap.setColor(ImmutableColor.WHITE)
    pixmap.drawPixel(0, 0)
    val texture = Texture(pixmap) //remember to dispose of later

    pixmap.dispose()
    return TextureRegion(texture, 0, 0, 1, 1)
}

fun Camera.center(boundary: BoundingBox, offset: ImmutableVector3 = ImmutableVector3.ZERO ): ImmutableVector3 = (boundary.getCenter() + this.position)

inline fun <B : ModelBatch> B.use(camera: Camera? = null, action: (B) -> Unit) {
    begin(camera)
    action(this)
    end()
}

@OptIn(ExperimentalContracts::class)
inline fun <B : ModelCache> B.use(camera: Camera? = null, action: B.() -> Unit) {
    begin(camera)
    action(this)
    end()
}

inline fun <B: ModelBuilder> B.use(withRoot: Boolean = true, action: (B) -> Unit): Model {
    begin()
    if(withRoot) {
        node("root") {
            action(this)
        }
    } else {
        action(this)
    }
    return end()
}

inline fun ModelBuilder.node(name: String? = null, action: (Node) -> Unit) {
    val node = node()
    node.id = name
    action(node)
}

inline fun <T> Iterable<T>.forEachApply(block: T.() -> Unit): Unit {
    for (element in this) element.apply(block)

}

inline fun <T> T.applyIf(condition: Boolean, block: T.() -> Unit) {
    if(condition) this.apply(block)
}

inline fun <T> T.applyIf(condition: T.() -> Boolean, block: T.() -> Unit) {
    if(condition.invoke(this)) this.apply(block)
}


inline fun <T> T.notEqual(other: Any?): Boolean = other == null || (other.let{ it != this })

inline fun <T> Iterable<T>.sumBy(selector: (T) -> Float): Float {
    var sum: Float = 0f
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun Pixmap.setColor(color: ImmutableColor) {
    this.setColor(color.toMutable())
}

fun BoundingBox.getDimensionsImmutable(): ImmutableVector3 = ImmutableVector3(width, height, depth)

fun mergeMeshes(meshes: MutableList<Mesh>, transformations: MutableList<Matrix4?>): Mesh? {
    if (meshes.size == 0) return null
    var vertexArrayTotalSize = 0
    var indexArrayTotalSize = 0
    val va = meshes[0].vertexAttributes
    val vaA = IntArray(va.size())
    for (i in 0 until va.size()) {
        vaA[i] = va[i].usage
    }
    for (i in 0 until meshes.size) {
        val mesh = meshes[i]
        if (mesh.vertexAttributes.size() != va.size()) {
            copyMesh(mesh, true, false, vaA)?.let {  meshes[i] = it }

        }
        vertexArrayTotalSize += mesh.numVertices * mesh.vertexSize / 4
        indexArrayTotalSize += mesh.numIndices
    }
    val vertices = FloatArray(vertexArrayTotalSize)
    val indices = ShortArray(indexArrayTotalSize)
    val indicesNew = IntArray(indexArrayTotalSize)
    var indexOffset = 0
    var vertexOffset = 0
    var vertexSizeOffset = 0
    var vertexSize = 0
    for (i in 0 until meshes.size) {
        val mesh = meshes[i]
        val numIndices = mesh.numIndices
        val numVertices = mesh.numVertices
        vertexSize = mesh.vertexSize / 4
        val baseSize = numVertices * vertexSize
        val posAttr = mesh.getVertexAttribute(VertexAttributes.Usage.Position)
        val offset = posAttr.offset / 4
        val numComponents = posAttr.numComponents
        run {
            //uzupelnianie tablicy indeksow
            mesh.getIndices(indices, indexOffset)
            for (c in indexOffset until indexOffset + numIndices) {

                indicesNew[c] = indices[c] + vertexOffset
            }
            indexOffset += numIndices
        }
        mesh.getVertices(0, baseSize, vertices, vertexSizeOffset)
        Mesh.transform(transformations[i], vertices, vertexSize, offset, numComponents, vertexOffset, numVertices)
        vertexOffset += numVertices
        vertexSizeOffset += baseSize
    }
    val result = Mesh(true, vertexOffset, indices.size, meshes[0].vertexAttributes)
    result.setVertices(vertices)
    result.setIndices(indicesNew.map { it.toShort() }.toShortArray())
    return result
}

fun copyMesh(meshToCopy: Mesh, isStatic: Boolean, removeDuplicates: Boolean, usage: IntArray?): Mesh? {
    // TODO move this to a copy constructor?
    // TODO duplicate the buffers without double copying the data if possible.
    // TODO perhaps move this code to JNI if it turns out being too slow.
    val vertexSize = meshToCopy.vertexSize / 4
    var numVertices = meshToCopy.numVertices
    var vertices = FloatArray(numVertices * vertexSize)
    meshToCopy.getVertices(0, vertices.size, vertices)
    var checks: ShortArray? = null
    var attrs: Array<VertexAttribute?>? = null
    var newVertexSize = 0
    if (usage != null) {
        var size = 0
        var `as` = 0
        for (i in usage.indices) if (meshToCopy.getVertexAttribute(usage[i]) != null) {
            size += meshToCopy.getVertexAttribute(usage[i]).numComponents
            `as`++
        }
        if (size > 0) {
            attrs = arrayOfNulls(`as`)
            checks = ShortArray(size)
            var idx = -1
            var ai = -1
            for (i in usage.indices) {
                val a = meshToCopy.getVertexAttribute(usage[i]) ?: continue
                for (j in 0 until a.numComponents) checks[++idx] = (a.offset / 4 + j).toShort()
                attrs[++ai] = VertexAttribute(a.usage, a.numComponents, a.alias)
                newVertexSize += a.numComponents
            }
        }
    }
    if (checks == null) {
        checks = ShortArray(vertexSize)
        for (i in 0 until vertexSize) checks[i] = i.toShort()
        newVertexSize = vertexSize
    }
    val numIndices = meshToCopy.numIndices
    var indices: ShortArray? = null
    if (numIndices > 0) {
        indices = ShortArray(numIndices)
        meshToCopy.getIndices(indices)
        if (removeDuplicates || newVertexSize != vertexSize) {
            val tmp = FloatArray(vertices.size)
            var size = 0
            for (i in 0 until numIndices) {
                val idx1 = indices[i] * vertexSize
                var newIndex: Short = -1
                if (removeDuplicates) {
                    var j: Short = 0
                    while (j < size && newIndex < 0) {
                        val idx2 = j * newVertexSize
                        var found = true
                        var k = 0
                        while (k < checks.size && found) {
                            if (tmp[idx2 + k] != vertices[idx1 + checks[k]]) found = false
                            k++
                        }
                        if (found) newIndex = j
                        j++
                    }
                }
                if (newIndex > 0) indices[i] = newIndex else {
                    val idx = size * newVertexSize
                    for (j in checks.indices) tmp[idx + j] = vertices[idx1 + checks[j]]
                    indices[i] = size.toShort()
                    size++
                }
            }
            vertices = tmp
            numVertices = size
        }
    }
    val result: Mesh = if (attrs == null) Mesh(isStatic, numVertices, indices?.size ?: 0, meshToCopy.vertexAttributes) else Mesh(
            isStatic, numVertices,
            indices?.size ?: 0, *attrs
        )
    result.setVertices(vertices, 0, numVertices * newVertexSize)
    result.setIndices(indices)
    return result
}
