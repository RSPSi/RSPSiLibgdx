package com.rspsi.game

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Attributes
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.rspsi.ext.ImmutableVector3
import ktx.math.ImmutableVector2
import ktx.math.toMutable
import com.badlogic.gdx.graphics.VertexAttributes.Usage
import com.badlogic.gdx.math.Vector3
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.toMutable

data class RS2Vertex(
    var position: ImmutableVector3 = ImmutableVector3.ZERO,
    var bone: Int = 0,
    var colour: ImmutableColor = ImmutableColor.MAGENTA,
    var alpha: Float = 1f,
    var normal: ImmutableVector3 = ImmutableVector3.ZERO,
    var uv: ImmutableVector2 = ImmutableVector2.ZERO
) {
    constructor(toCopy: RS2Vertex) : this(toCopy.position, toCopy.bone, toCopy.colour, toCopy.alpha, toCopy.normal, toCopy.uv)

}

private fun RS2Vertex.meshPartVertexInfo(vertexAttributes: VertexAttributes): MeshPartBuilder.VertexInfo {
    val vertexUsage = vertexAttributes.map { it.usage }
    val vertexInfo = MeshPartBuilder.VertexInfo()
    if(Usage.Position in vertexUsage)
        vertexInfo.setPos(position.toMutable())
    if(Usage.TextureCoordinates in vertexUsage)
            vertexInfo.setUV(this.uv.toMutable())
    if(Usage.ColorPacked in vertexUsage)
        vertexInfo.setCol(colour.toMutable())
    if(Usage.Normal in vertexUsage && normal != ImmutableVector3.ZERO)
        vertexInfo.setNor(normal.flipYZ().toMutable())

    //if(Usage.BoneWeight in vertexUsage)
   //if(Usage.BiNormal in vertexUsage)
    return vertexInfo
}
fun MeshPartBuilder.triangle(a: RS2Vertex, b: RS2Vertex, c: RS2Vertex, vertexAttributes: VertexAttributes) {
    this.triangle(a.meshPartVertexInfo(vertexAttributes), b.meshPartVertexInfo(vertexAttributes), c.meshPartVertexInfo(vertexAttributes))
}
