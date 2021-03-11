package com.rspsi.ext

import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder


fun MeshPartBuilder.VertexInfo.copy(): MeshPartBuilder.VertexInfo {
    val vertexInfo =  MeshPartBuilder.VertexInfo()
    vertexInfo.set(this.position.cpy(), this.normal.cpy(), this.color.cpy(), this.uv.cpy())
    return vertexInfo
}