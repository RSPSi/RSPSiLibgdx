package com.rspsi.game

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.ArrowShapeBuilder
import com.badlogic.gdx.math.Matrix4
import com.brashmonkey.spriter.Calculator.sqrt
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.*
import ktx.log.info
import ktx.math.ImmutableVector2


class RS2ModelBoundary {
    var modelHeight = 0f
    var boundingPlaneRadius = 0f
    var boundingCylinderRadius = 0f
    var boundingSphereRadius = 0f
    var minimumY = 0f
    var minimumX: Float = 0xf423f.toFloat()
    var maximumX: Float = (-0xf423f).toFloat()
    var maximumZ: Float = (-0x1869f).toFloat()
    var minimumZ: Float = 0x1869f.toFloat()
}

open class RS2Model(val id: Int, val header: RS2ModelHeader) {

    val textureProvider: RS2TextureProvider = context.inject()

    var faces = arrayListOf<Face>()
    var vertices = arrayListOf<RS2Vertex>()
    var textures = arrayListOf<Texture>()
    var textureCoordinates = arrayListOf<Int>()

    var boundary = RS2ModelBoundary()

    open fun computeBounds() {
        boundary = RS2ModelBoundary()
        with(boundary) {
            for (vertex in vertices) {
                val x: Float = vertex.position.x
                val y: Float = vertex.position.y
                val z: Float = vertex.position.z
                if (x < minimumX) {
                    minimumX = x
                }
                if (x > maximumX) {
                    maximumX = x
                }
                if (z < minimumZ) {
                    minimumZ = z
                }
                if (z > maximumZ) {
                    maximumZ = z
                }
                if (-y > modelHeight) {
                    modelHeight = -y
                }
                if (y > minimumY) {
                    minimumY = y
                }
                val radius = x * x + z * z
                if (radius > boundingPlaneRadius) {
                    boundingPlaneRadius = radius
                }
            }
            boundingPlaneRadius = sqrt(boundingPlaneRadius)
            boundingCylinderRadius = sqrt(boundingPlaneRadius * boundingPlaneRadius + modelHeight * modelHeight)
            boundingSphereRadius =
                (boundingCylinderRadius + sqrt(boundingPlaneRadius * boundingPlaneRadius + minimumY * minimumY))
        }
    }

    open fun decode(): RS2Model {

        decodeVertices()
        decodeFaceIndices()
        decodeFaceData()
        if (header.useTextures) {
            decodeTextureData()
            computeTextureUVCoordinates()
        }
        return this
    }

    fun addToPart(
        transform: Matrix4?,
        builder: MeshPartBuilder,
        predicate: (Face) -> Boolean,
        vertexAttributes: VertexAttributes
    ) {
        transform?.let { builder.setVertexTransform(it) }
        val scaledVertices = vertices.map { RS2Vertex(it) }
        scaledVertices.forEach { it.position /= 128f }
        faces.filter(predicate).forEach {
            builder.triangle(scaledVertices[it.a], scaledVertices[it.b], scaledVertices[it.c], vertexAttributes)
        }
    }

    open fun computeTextureUVCoordinates() {
        for (i in 0 until faces.size) {
            var textureCoordinate = faces[i].textureCoordIndex
            var textureIdx = faces[i].textureId
            if (textureIdx != -1) {
                val uvs = mutableListOf<ImmutableVector2>()
                if (textureCoordinate == -1) {
                    uvs.add(0 withY 1)
                    uvs.add(1 withY 1)
                    uvs.add(0 withY 0)
                } else {
                    textureCoordinate = textureCoordinate and 0xFF
                    var textureRenderType = textures[textureCoordinate].type
                    if (textureRenderType == 0) {
                        val faceVertexIdx1: Int = faces[i].a
                        val faceVertexIdx2: Int = faces[i].b
                        val faceVertexIdx3: Int = faces[i].c
                        val triangleVertexIdx1: Int = textures[textureCoordinate].p
                        val triangleVertexIdx2: Int = textures[textureCoordinate].m
                        val triangleVertexIdx3: Int = textures[textureCoordinate].N

                        val triangleA = vertices[triangleVertexIdx1].position
                        val triangleB = vertices[triangleVertexIdx2].position
                        val triangleC = vertices[triangleVertexIdx3].position

                        val faceA = vertices[faceVertexIdx1].position
                        val faceB = vertices[faceVertexIdx2].position
                        val faceC = vertices[faceVertexIdx3].position

                        val f_882_ = triangleB.x - triangleA.x
                        val f_883_ = triangleB.y - triangleA.y
                        val f_884_ = triangleB.z - triangleA.z
                        val f_885_ = triangleC.x - triangleA.x
                        val f_886_ = triangleC.y - triangleA.y
                        val f_887_ = triangleC.z - triangleA.z
                        val f_888_ = faceA.x - triangleA.x
                        val f_889_ = faceA.y - triangleA.y
                        val f_890_ = faceA.z - triangleA.z
                        val f_891_ = faceB.x - triangleA.x
                        val f_892_ = faceB.y - triangleA.y
                        val f_893_ = faceB.z - triangleA.z
                        val f_894_ = faceC.x - triangleA.x
                        val f_895_ = faceC.y - triangleA.y
                        val f_896_ = faceC.z - triangleA.z

                        val f_897_ = f_883_ * f_887_ - f_884_ * f_886_
                        val f_898_ = f_884_ * f_885_ - f_882_ * f_887_
                        val f_899_ = f_882_ * f_886_ - f_883_ * f_885_
                        var f_900_ = f_886_ * f_899_ - f_887_ * f_898_
                        var f_901_ = f_887_ * f_897_ - f_885_ * f_899_
                        var f_902_ = f_885_ * f_898_ - f_886_ * f_897_
                        var f_903_ = 1.0f / (f_900_ * f_882_ + f_901_ * f_883_ + f_902_ * f_884_)


                        var f_900_2 = f_883_ * f_899_ - f_884_ * f_898_
                        var f_901_2 = f_884_ * f_897_ - f_882_ * f_899_
                        var f_902_2 = f_882_ * f_898_ - f_883_ * f_897_
                        var f_903_2 = 1.0f / (f_900_2 * f_885_ + f_901_2 * f_886_ + f_902_2 * f_887_)

                        uvs.add((f_900_ * f_888_ + f_901_ * f_889_ + f_902_ * f_890_).toInt() * f_903_ withY (f_900_2 * f_888_ + f_901_2 * f_889_ + f_902_2 * f_890_).toInt() * f_903_2)
                        uvs.add((f_900_ * f_891_ + f_901_ * f_892_ + f_902_ * f_893_).toInt() * f_903_ withY (f_900_2 * f_891_ + f_901_2 * f_892_ + f_902_2 * f_893_).toInt() * f_903_2)
                        uvs.add((f_900_ * f_894_ + f_901_ * f_895_ + f_902_ * f_896_).toInt() * f_903_ withY (f_900_2 * f_894_ + f_901_2 * f_895_ + f_902_2 * f_896_).toInt() * f_903_2)


                    } else {
                        uvs.add(0 withY 1)
                        uvs.add(1 withY 1)
                        uvs.add(0 withY 0)
                    }
                }
                vertices[faces[i].a].uv = uvs[0]
                vertices[faces[i].b].uv = uvs[1]
                vertices[faces[i].c].uv = uvs[2]

                /*
                val textureRegion = textureProvider.textureAtlas?.findRegion("oldschool_texture_$textureIdx")
                textureRegion?.let {
                    info {"Found region $textureIdx | ${textureRegion.regionWidth} | ${textureRegion.regionHeight} " +
                            "| ${textureRegion.u} | ${textureRegion.v} | ${textureRegion.u2} | ${textureRegion.v2}"}

                    val transformed = uvs.map {
                        ImmutableVector2(textureRegion.regionWidth * it.x, textureRegion.regionHeight * it.y) / 2048f
                    }.map {
                        it + ImmutableVector2(textureRegion.regionX.toFloat(), textureRegion.regionY.toFloat()) / 2048f
                    }.map {
                        it
                    }
                    vertices[faces[i].a].uv = transformed[0]
                    vertices[faces[i].b].uv = transformed[1]
                    vertices[faces[i].c].uv = transformed[2]

                    info {"SEt uvs to ${vertices[faces[i].a].uv} ${vertices[faces[i].b].uv} ${vertices[faces[i].c].uv}"}
                }

                 */
            }
        }
    }

    open fun build(transform: Matrix4? = null): Model {

        return ModelBuilder().use { modelBuilder ->


            if(faces.any { it.type >= 0 && it.textureId < 0 && it.transparency >= 1f }) {
                val nonTextureMeshBuilder = modelBuilder.part(
                    "nonTexturedFaces",
                    GL20.GL_TRIANGLES,
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.ColorPacked(),
                        VertexAttribute.Normal()
                    ),
                    Material()
                )
                addToPart(
                    transform,
                    nonTextureMeshBuilder,
                    { it.type >= 0 && it.textureId < 0 && it.transparency == 1f },
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.ColorPacked(),
                        VertexAttribute.Normal()
                    )
                )
            }


            if(faces.any { it.type >= 0 && it.textureId < 0 && it.transparency < 1f }) {
                val alphaFaces = modelBuilder.part(
                    "alphaFaces",
                    GL20.GL_TRIANGLES,
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.ColorPacked(),
                        VertexAttribute.Normal()
                    ),
                    Material(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))
                )

                addToPart(
                    transform,
                    alphaFaces,
                    { it.type >= 0 && it.textureId < 0 && it.transparency < 1f },
                    VertexAttributes(
                        VertexAttribute.Position(),
                        VertexAttribute.ColorPacked(),
                        VertexAttribute.Normal()
                    )
                )
            }

            val textureIds = faces
                .filter { it.textureId >= 0 }
                .map { it.textureId }
                .distinct()
            for (textureId in textureIds) {


                    val textureMeshBuilder = modelBuilder.part(
                        "texturedFaces_$textureId",
                        GL20.GL_TRIANGLES,
                        VertexAttributes(
                            VertexAttribute.Position(),
                            VertexAttribute.TexCoords(0),
                            VertexAttribute.Normal()
                        ),
                        textureProvider.materials[textureId]
                    )

                    addToPart(
                        transform, textureMeshBuilder, { it.type >= 0 && it.textureId == textureId },
                        VertexAttributes(
                            VertexAttribute.Position(),
                            VertexAttribute.TexCoords(0),
                            VertexAttribute.Normal()
                        )
                    )

            }


            if(showNormals)
            faces.forEachIndexed { index, face ->

                val center = getCenter(face)

                if(center != ImmutableVector3.ZERO) {

                    val normalArrows = modelBuilder.part(
                        "normalArrow_$index",
                        GL20.GL_TRIANGLES,
                        VertexAttributes(
                            VertexAttribute.Position(),
                            VertexAttribute.ColorPacked()
                        ),
                        Material()
                    )
                    normalArrows.setColor(Color.RED)
                    vertices[face.a].normal.normalized.let { normal ->

                        if (center == normal || center == ImmutableVector3.ZERO || normal == ImmutableVector3.ZERO) {
                            info { "Clashing $center $normal" }
                        } else {
                            ArrowShapeBuilder.build(
                                normalArrows,
                                -0.001f,
                                -0.001f,
                                -0.001f,
                                normal.x,
                                normal.y,
                                normal.z,
                                0.1f,
                                0.1f,
                                5
                            )
                        }
                        //normalArrows.setVertexTransform(Matrix4().translate(center).setToLookAt((center * normal).toMutable(), ImmutableVector3.Y.toMutable()))
                    }
                }

            }

        }

    }

    fun getCenter(face: Face): ImmutableVector3 {
        var areaTotal = 0f
        var p1 = vertices[face.a].position
        var p2 = vertices[face.b].position
        var p3 = vertices[face.c].position

        var edge1 = p3 - p1
        var edge2 = p3 - p2

        var cross = edge1 cross edge2
        var area = cross.len / 2

        return ImmutableVector3(
            x = area * (p1.x + p2.x + p3.x) / 3,
            y = area * (p1.y + p2.y + p3.y) / 3,
            z = area * (p1.z + p2.z + p3.z) / 3
        )
    }

    var showNormals = false

    open fun generateNormals(lighting: Int, diffusion: Int, x: Int, y: Int, z: Int, immediateShading: Boolean) {
        val length = sqrt((x * x + y * y + z * z).toFloat()).toInt()
        val k1 = diffusion * length shr 8

        for (face in faces) {

            var surfaceNormal = getSurfaceNormal(face)

            if(surfaceNormal.z < 0f)
                surfaceNormal *= ImmutableVector3(1f, 1f, -1f)

            for (vert in arrayOf(face.a, face.b, face.c)) {
                with(vertices[vert]) {

                    if (face.renderType and 1 == 0) {
                        normal += surfaceNormal
                    } else {
                        //normal = surfaceNormal * ImmutableVector3(x, y, z)
                        //normal += surfaceNormal
                        //normal += surfaceNormal.normalized
                    }
                }
            }


        }
    }

    fun getSurfaceNormal(face: Face): ImmutableVector3 {
        val ba = (vertices[face.a].position * vertices[face.b].position)
        val ca = (vertices[face.c].position * vertices[face.a].position)
        return ImmutableVector3(
            x = (ba.y * ca.z - ba.z * ca.y),
            y = (ba.z * ca.x - ba.x * ca.z),
            z = (ba.x * ca.y - ba.y * ca.x)
        )
    }


    open fun decodeVertices() {
        vertices.clear()
        val directions: InputBuffer = InputBuffer.create(header.data, header.vertexDirectionOffset)
        val verticesX: InputBuffer = InputBuffer.create(header.data, header.xDataOffset)
        val verticesY: InputBuffer = InputBuffer.create(header.data, header.yDataOffset)
        val verticesZ: InputBuffer = InputBuffer.create(header.data, header.zDataOffset)
        val bones: InputBuffer = InputBuffer.create(header.data, header.boneOffset)

        var baseVertex = RS2Vertex()


        for (vertex in 0 until header.vertexCount) {
            val mask = directions.readUnsignedByte()

            var x = 0f
            if (mask and 1 != 0) {
                x = verticesX.readSmart().toFloat()
            }

            var y = 0f
            if (mask and 2 != 0) {
                y = verticesY.readSmart().toFloat()
            }

            var z = 0f
            if (mask and 4 != 0) {
                z = verticesZ.readSmart().toFloat()
            }

            val bone = if (header.useFaceSkinning) bones.readUnsignedByte() else 0

            val pos = ImmutableVector3(x, (1f/ 128f) - y, z)
            vertices.add(vertex, RS2Vertex(baseVertex.position + pos, bone))

            baseVertex = vertices[vertex]


        }


    }


    open fun invert(): RS2Model {
        for (vertex in 0 until vertices.size) {
            vertices[vertex].position *= ImmutableVector3(1f, 1f, -1f)
        }
        for (face in 0 until faces.size) {

            val a: Int = faces[face].a
            faces[face].a = faces[face].c
            faces[face].c = a
        }
        return this
    }


    open fun decodeFaceIndices() {

        faces.clear()
        val faceData: InputBuffer = InputBuffer.create(header.data, header.faceDataOffset)
        val faceTypes: InputBuffer = InputBuffer.create(header.data, header.faceTypeOffset)

        var faceX = 0
        var faceY = 0
        var faceZ = 0
        var offset = 0

        for (index in 0 until header.faceCount) {
            val type = faceTypes.readUnsignedByte()

            when (type) {

                1 -> {
                    faceX = faceData.readSmart() + offset
                    offset = faceX
                    faceY = faceData.readSmart() + offset
                    offset = faceY
                    faceZ = faceData.readSmart() + offset
                    offset = faceZ
                }
                2 -> {
                    faceY = faceZ
                    faceZ = faceData.readSmart() + offset
                    offset = faceZ
                }
                3 -> {
                    faceX = faceZ
                    faceZ = faceData.readSmart() + offset
                    offset = faceZ
                }
                4 -> {
                    val temp = faceX
                    faceX = faceY
                    faceY = temp
                    faceZ = faceData.readSmart() + offset
                    offset = faceZ
                }
            }


            faces.add(Face(a = faceY, b = faceX, c = faceZ, type = type))



        }

    }


    fun splitVertices() {
        val duplicatedVertices = mutableListOf<RS2Vertex>()
        faces.forEach {
            val pos = duplicatedVertices.size
            for(vertex in arrayOf(it.a, it.b, it.c))
                duplicatedVertices.add(vertices[vertex].copy())
            it.a = pos
            it.b = pos + 1
            it.c = pos + 2
        }

        vertices.clear()
        vertices.addAll(duplicatedVertices)
    }

    open fun decodeTextureData() {
        val maps = InputBuffer.create(header.data, header.uvMapFaceOffset)

        for (index in 0 until header.texturedFaceCount) {
            textures.add(
                Texture(
                    type = 0,
                    p = maps.readUnsignedShort(),
                    m = maps.readUnsignedShort(),
                    N = maps.readUnsignedShort()
                )
            )
        }

    }

    fun setVertexColours() {
        faces.forEach { face ->
            val vertexColour = if (face.textureId < 0) {
                RS2Colour.colourPalette[face.colour] withAlpha face.transparency
            } else {
                val textureColor = textureProvider.textureDefinitions[face.textureId]?.colour ?: 0
                RS2Colour.colourPalette[textureColor] withAlpha face.transparency
            }

            vertices[face.a].colour = vertexColour
            vertices[face.b].colour = vertexColour
            vertices[face.c].colour = vertexColour
        }
    }

    open fun decodeFaceData() {
        val colours: InputBuffer = InputBuffer.create(header.data, header.colourDataOffset)
        val points: InputBuffer = InputBuffer.create(header.data, header.texturePointerOffset)
        val priorities: InputBuffer = InputBuffer.create(header.data, header.facePriorityOffset)
        val alphas: InputBuffer = InputBuffer.create(header.data, header.faceAlphaOffset)
        val bones: InputBuffer = InputBuffer.create(header.data, header.faceBoneOffset)


        for (index in 0 until header.faceCount) {
            val face = faces[index]
            face.colour = colours.readUnsignedShort()
            if (header.useTextures) {
                val mask = points.readUnsignedByte()

                if (mask and 1 == 1) {
                    face.renderType = 1
                } else {
                    face.renderType = 0
                }

                if (mask and 2 == 2) {
                    face.textureCoordIndex = mask shr 2
                    face.textureId = face.colour
                    face.colour = 127
                } else {
                    face.textureCoordIndex = -1
                    face.textureId = -1
                }

            }
            face.priority =
                if (header.facePriority == 255) priorities.readUnsignedByte() else header.facePriority


            face.transparency = if (header.useTransparency) (255f - alphas.readUnsignedByte()) / 255f else 1f

            face.skinAttachment = if (header.useFaceSkinning) bones.readUnsignedByte() else -1
        }


    }

    fun cpy(): RS2Model {
        val copy = RS2Model(id, header)
        faces.forEach {
            copy.faces.add(it.copy())
        }
        vertices.forEach {
            copy.vertices.add(it.copy())
        }

        textures.forEach {
            copy.textures.add(it.copy())
        }

        return copy

    }


    data class Face(
        var a: Int, var b: Int, var c: Int,
        var type: Int,
        var skinAttachment: Int = -1,
        var priority: Int = -1,
        var transparency: Float = 1f,
        var colour: Int = 0,
        var textureId: Int = -1,
        var textureCoordIndex: Int = -1,
        var colourLit: Int = 0,
        var renderType: Int = 0
    ) {

        init {
            if (a < 0 || b < 0 || c < 0)
                info {
                    "$a | $b | $c"
                }

        }
    }


    data class Texture(var type: Int, var p: Int, var m: Int, var N: Int)
}

