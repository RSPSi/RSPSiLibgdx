package com.rspsi.game

import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.*
import ktx.math.ImmutableVector2
import ktx.math.minus

class RS2ModelHD(id: Int, header: RS2ModelHeader): RS2Model(id, header) {


    override fun computeTextureUVCoordinates() {
        for (i in 0 until faces.size) {
            var textureCoordinate = faces[i].textureCoordIndex
            var textureIdx = faces[i].textureId
            if (textureIdx != -1) {
                val u = FloatArray(3)
                val v = FloatArray(3)
                if (textureCoordinate == -1) {
                    u[0] = 0.0f
                    v[0] = 1.0f
                    u[1] = 1.0f
                    v[1] = 1.0f
                    u[2] = 0.0f
                    v[2] = 0.0f
                } else {
                    textureCoordinate = textureCoordinate and 0xFF
                    val textureRenderType = textures[textureCoordinate].type
                    if (textureRenderType == 0) {
                        val triangleA = vertices[textures[textureCoordinate].p].position
                        val triangleB = vertices[textures[textureCoordinate].m].position
                        val triangleC = vertices[textures[textureCoordinate].N].position

                        val vertexA = vertices[faces[i].a].position
                        val vertexB = vertices[faces[i].b].position
                        val vertexC = vertices[faces[i].c].position

                        val f88234 = triangleB - triangleA
                        val f88567 = triangleC - triangleA

                        val f88890 = vertexA - triangleA
                        val f89123 = vertexB - triangleA
                        val f89456 = vertexC - triangleA

                        //this.x * v.y - this.y * v.x

                        val f897 = f88234.yz crs f88567.yz
                        val f898 = f88234.xz crs f88567.xz
                        val f899 = f88234.xy crs f88567.xy

                        val f89789 = ImmutableVector3(f897, f898, f899)

                        var f900 = f88567.yz crs f89789.yz
                        var f901 = f88567.xz crs f89789.xz
                        var f902 = f88567.xy crs f89789.xy

                        var f9012 = ImmutableVector3(f900, f901, f902)
//x * vector.x + y * vector.y + z * vector.z;
                        var f903 = 1.0f / (f9012 dot f88234)
                        u[0] = (f9012 dot f88890) * f903
                        u[1] = (f9012 dot f89123) * f903
                        u[2] = (f9012 dot f89456) * f903

                        f900 = f88234.yz crs f89789.yz
                        f901 = f88234.xz crs f89789.xz
                        f902 = f88234.xy crs f89789.xy
                        f903 = 1.0f / (f9012 dot f88567)

                        f9012 = ImmutableVector3(f900, f901, f902)

                        v[0] = (f9012 dot f88890) * f903
                        v[1] = (f9012 dot f89123) * f903
                        v[2] = (f9012 dot f89456) * f903

                    }
                }

                vertices[faces[i].a].uv = ImmutableVector2(u[0], v[0])
                vertices[faces[i].b].uv = ImmutableVector2(u[1], v[1])
                vertices[faces[i].c].uv = ImmutableVector2(u[2], v[2])
            }
        }
    }

    override fun decodeTextureData() {
        val maps = InputBuffer.create(header.data, header.uvMapFaceOffset)

        for (index in 0 until header.texturedFaceCount) {
            val type = textures[index].type

            if(type == 0) {
                textures[index].apply {
                    p = maps.readUnsignedShort()
                    m = maps.readUnsignedShort()
                    N = maps.readUnsignedShort()
                }
            }
        }
    }

    override fun decodeFaceData() {

        textureCoordinates.clear()

        val colours: InputBuffer = InputBuffer.create(header.data, header.colourDataOffset)
        val points: InputBuffer = InputBuffer.create(header.data, header.texturePointerOffset)
        val priorities: InputBuffer = InputBuffer.create(header.data, header.facePriorityOffset)
        val alphas: InputBuffer = InputBuffer.create(header.data, header.faceAlphaOffset)
        val bones: InputBuffer = InputBuffer.create(header.data, header.faceBoneOffset)
        val textures: InputBuffer = InputBuffer.create(header.data, header.texturePointerOffset)
        val textureCoordinates: InputBuffer = InputBuffer.create(header.data, header.faceDataOffset)


        for (index in 0 until header.faceCount) {
            val face = faces[index]
            face.colour = colours.readUnsignedShort()
            if (header.useTextures) {
                face.type = points.readUnsignedByte()
            }
            face.priority =
                if (header.facePriority == 255) priorities.readUnsignedByte() else header.facePriority


            face.transparency = if (header.useTransparency) (255 - alphas.readUnsignedByte()) / 255f else 1f

            face.skinAttachment = if (header.useFaceSkinning) bones.readUnsignedByte() else -1

            if(header.useTextures) {
                face.textureId = textures.readUnsignedShort() - 1
            }

            if(this.textures.isNotEmpty() && face.textureId >= 0){
                this.textureCoordinates[index] = textureCoordinates.readUnsignedShort() - 1
            }

        }


    }

    fun calculateTextureTypes() {
        val var2 = InputBuffer.create(header.data)
        var textureAmount = 0
        var var7 = 0
        var var29 = 0
        var position: Int
        if (header.texturedFaceCount > 0) {

            position = 0
            while (position < header.texturedFaceCount) {
                textures[position].type = var2.readByte().toInt()
                val renderType = textures[position].type
                if (renderType == 0) {
                    ++textureAmount
                }
                if (renderType in 1..3) {
                    ++var7
                }
                if (renderType == 2) {
                    ++var29
                }
                ++position
            }
        }

    }
}