package com.rspsi.game

import com.badlogic.gdx.graphics.Mesh
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.model.Node
import java.util.*


fun Mesh.copy2(isStatic: Boolean, removeDuplicates: Boolean, usage: IntArray?): Mesh {
    // TODO move this to a copy constructor?
    // TODO duplicate the buffers without double copying the data if possible.
    // TODO perhaps move this code to JNI if it turns out being too slow.
    val vertexSize: Int = this.vertexSize / 4
    var numVertices: Int = this.numVertices
    var vertices = FloatArray(numVertices * vertexSize)
    this.getVertices(0, vertices.size, vertices)
    var checks: ShortArray? = null
    var attrs: Array<VertexAttribute?>? = null
    var newVertexSize = 0
    if (usage != null) {
        var size = 0
        var `as` = 0
        for (i in usage.indices) if (this.getVertexAttribute(usage[i]) != null) {
            size += this.getVertexAttribute(usage[i]).numComponents
            `as`++
        }
        if (size > 0) {
            attrs = arrayOfNulls(`as`)
            checks = ShortArray(size)
            var idx = -1
            var ai = -1
            for (i in usage.indices) {
                val a: VertexAttribute = this.getVertexAttribute(usage[i]) ?: continue
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
    val numIndices: Int = this.numIndices
    var indices: ShortArray? = null
    if (numIndices > 0) {
        indices = ShortArray(numIndices)
        this.getIndices(indices)
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
    val result =
    if (attrs == null) Mesh(isStatic, numVertices, indices?.size
            ?: 0, this.vertexAttributes) else Mesh(isStatic, numVertices, indices?.size
            ?: 0, *attrs.filterNotNull().toTypedArray())
    result.setVertices(vertices, 0, numVertices * newVertexSize)
    result.setIndices(indices)
    return result
}

