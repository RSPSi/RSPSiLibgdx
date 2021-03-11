package com.rspsi.game

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.ImmutableVector3
import com.rspsi.ext.xy
import ktx.graphics.use
import kotlin.math.sqrt


object RS2TileShape {
    const val TILE_FULL = 1f
    const val TILE_HALF = 0.5f
    const val TILE_THREE_QUARTER = 0.75f
    const val TILE_QUARTER = 0.25f
    val SHAPE_LAYOUT = intArrayOf(
            1, 17, 2, 18, 3,
            24, 13, 9, 14, 19,
            8, 12, 16, 10, 4,
            23, 0, 11, 15, 20,
            7, 22, 6, 21, 5
    )
    val tileShapePoints =
            arrayOf(intArrayOf(1, 3, 5, 7),
                    intArrayOf(1, 3, 5, 7),
                    intArrayOf(1, 3, 5, 7),
                    intArrayOf(1, 3, 5, 7, 6),
                    intArrayOf(1, 3, 5, 7, 6),
                    intArrayOf(1, 3, 5, 7, 6),
                    intArrayOf(1, 3, 5, 7, 6),
                    intArrayOf(1, 3, 5, 7, 2, 6),
                    intArrayOf(1, 3, 5, 7, 2, 8),
                    intArrayOf(1, 3, 5, 7, 2, 8),
                    intArrayOf(1, 3, 5, 7, 11, 12),
                    intArrayOf(1, 3, 5, 7, 11, 12),
                    intArrayOf(1, 3, 5, 7, 13, 14))


    val coords2D: Array<Vector2> = arrayOf<Vector2>(
            Vector2(TILE_QUARTER, TILE_HALF), Vector2(0F, 0F), Vector2(TILE_THREE_QUARTER, 0F), Vector2(TILE_FULL, 0F),
            Vector2(TILE_FULL, TILE_THREE_QUARTER), Vector2(TILE_FULL, TILE_FULL), Vector2(TILE_THREE_QUARTER, TILE_FULL), Vector2(0F, TILE_FULL),
            Vector2(0F, TILE_THREE_QUARTER), Vector2(TILE_THREE_QUARTER, TILE_QUARTER), Vector2(TILE_HALF, TILE_THREE_QUARTER), Vector2(TILE_THREE_QUARTER, TILE_HALF),
            Vector2(TILE_QUARTER, TILE_THREE_QUARTER), Vector2(TILE_QUARTER, TILE_QUARTER), Vector2(TILE_HALF, TILE_QUARTER), Vector2(TILE_HALF, TILE_HALF),  //Custom points below this line
            Vector2(TILE_THREE_QUARTER, TILE_THREE_QUARTER), Vector2(TILE_QUARTER, 0F), Vector2(TILE_HALF, 0F), Vector2(TILE_FULL, TILE_QUARTER),
            Vector2(TILE_FULL, TILE_HALF), Vector2(TILE_HALF, TILE_FULL), Vector2(TILE_QUARTER, TILE_FULL), Vector2(0F, TILE_HALF),
            Vector2(0F, TILE_QUARTER))

    enum class FloorType {
        OVERLAY, UNDERLAY
    }

    class FloorTile2D(val floorType: FloorType, val a: Vector2, val b: Vector2, val c: Vector2)

    val shapedTileElementData2 = arrayOf(arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[3], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.UNDERLAY, coords2D[3], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[3], coords2D[5]),
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[5], coords2D[6]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[6], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[3], coords2D[6]),
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[6], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[3], coords2D[5], coords2D[6])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[6], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[5]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[5], coords2D[6])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[5], coords2D[6]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[6]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[6], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[2], coords2D[3], coords2D[5]),
            FloorTile2D(FloorType.OVERLAY, coords2D[2], coords2D[5], coords2D[6]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[2], coords2D[6]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[6], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[2], coords2D[3], coords2D[5]),
            FloorTile2D(FloorType.OVERLAY, coords2D[2], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[2], coords2D[7], coords2D[8]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[2], coords2D[8])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[2], coords2D[8]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[2], coords2D[3], coords2D[5]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[2], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[2], coords2D[7], coords2D[8])
    ), arrayOf(
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[3], coords2D[12]),
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[11], coords2D[12]),
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[5], coords2D[11]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[12], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[12], coords2D[11], coords2D[7]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[11], coords2D[5], coords2D[7])
    ), arrayOf(
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[12]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[3], coords2D[11], coords2D[12]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[3], coords2D[5], coords2D[11]),
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[12], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[12], coords2D[11], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[11], coords2D[5], coords2D[7])
    ), arrayOf<FloorTile2D>(
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[14], coords2D[13]),
            FloorTile2D(FloorType.UNDERLAY, coords2D[1], coords2D[3], coords2D[14]),
            FloorTile2D(FloorType.OVERLAY, coords2D[1], coords2D[13], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[13], coords2D[14], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[14], coords2D[5], coords2D[7]),
            FloorTile2D(FloorType.OVERLAY, coords2D[3], coords2D[5], coords2D[14])
    ))

    val shapeRenderer = ShapeRenderer()

    fun getTexture(tileType: Int): Texture {
        val frameBuffer = FrameBuffer(Pixmap.Format.RGBA8888, 128, 128, false)

        frameBuffer.use {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
            shapedTileElementData2[tileType].forEach { data ->
                val floorType = data.floorType
                val a = data.a.cpy()
                val b = data.b.cpy()
                val c = data.c.cpy()
                shapeRenderer.color =
                    (if (floorType == FloorType.OVERLAY) ImmutableColor.WHITE else ImmutableColor.MAGENTA).toMutable()
                shapeRenderer.triangle(a.x, a.y, b.x, b.y, c.x, c.y)
            }

            shapeRenderer.end()
        }
        return frameBuffer.colorBufferTexture


    }

    fun getAllTexture(): TextureAtlas {
        val textureAtlas = TextureAtlas()

        for (i in shapedTileElementData2.indices) {
            val texture = getTexture(i)
            textureAtlas.addRegion("tile_shape_$i", TextureRegion(texture, 0, 0, 128, 128))
        }


        return textureAtlas
    }

    private val TILE_SHAPE_2D = arrayOf(intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 1, 1), intArrayOf(1, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), intArrayOf(0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 0, 1, 0, 0, 0, 1), intArrayOf(0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1, 1, 1, 1, 1, 1), intArrayOf(1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0), intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 1, 0, 0), intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 1, 1, 0, 0, 1, 1), intArrayOf(1, 1, 1, 1, 1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 0), intArrayOf(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 0, 1, 1, 1), intArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1, 1))
    private val TILE_ROTATION_2D = arrayOf(intArrayOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15), intArrayOf(12, 8, 4, 0, 13, 9, 5, 1, 14, 10, 6, 2, 15, 11, 7, 3), intArrayOf(15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0), intArrayOf(3, 7, 11, 15, 2, 6, 10, 14, 1, 5, 9, 13, 0, 4, 8, 12))


    val shapedTileElementData = arrayOf(
            intArrayOf(0, 1, 2, 3,  /**/0, 0, 1, 3),
            intArrayOf(1, 1, 2, 3,  /**/1, 0, 1, 3),
            intArrayOf(0, 1, 2, 3,  /**/1, 0, 1, 3),
            intArrayOf(0, 0, 1, 2,  /**/0, 0, 2, 4,  /**/1, 0, 4, 3),
            intArrayOf(0, 0, 1, 4,  /**/0, 0, 4, 3,  /**/1, 1, 2, 4),
            intArrayOf(0, 0, 4, 3,  /**/1, 0, 1, 2,  /**/1, 0, 2, 4),
            intArrayOf(0, 1, 2, 4,  /**/1, 0, 1, 4,  /**/1, 0, 4, 3),
            intArrayOf(0, 4, 1, 2,  /**/0, 4, 2, 5,  /**/1, 0, 4, 5,  /**/1, 0, 5, 3),
            intArrayOf(0, 4, 1, 2,  /**/0, 4, 2, 3,  /**/0, 4, 3, 5,  /**/1, 0, 4, 5),
            intArrayOf(0, 0, 4, 5,  /**/1, 4, 1, 2,  /**/1, 4, 2, 3,  /**/1, 4, 3, 5),
            intArrayOf(0, 0, 1, 5,  /**/0, 1, 4, 5,  /**/0, 1, 2, 4,  /**/1, 0, 5, 3,  /**/1, 5, 4, 3,  /**/1, 4, 2, 3),
            intArrayOf(1, 0, 1, 5,  /**/1, 1, 4, 5,  /**/1, 1, 2, 4,  /**/0, 0, 5, 3,  /**/0, 5, 4, 3,  /**/0, 4, 2, 3),
            intArrayOf(1, 0, 5, 4,  /**/1, 0, 1, 5,  /**/0, 0, 4, 3,  /**/0, 4, 5, 3,  /**/0, 5, 2, 3,  /**/0, 1, 2, 5),
            intArrayOf(1, 4, 5, 3)
    )

    fun getIndices(type: Int, orientation: Byte): MutableList<RS2Model.Face> {
        val shapedTileElement = shapedTileElementData[type.toInt()]
        val indices = mutableListOf<RS2Model.Face>()
        for (i in 0 until shapedTileElement.size / 4) {

            var shapeIndex = i * 4
            val overlayOrUnderlay = shapedTileElement[shapeIndex]

            var faceA = shapedTileElement[shapeIndex + 1]
            var faceB = shapedTileElement[shapeIndex + 2]
            var faceC = shapedTileElement[shapeIndex + 3]

            if (faceA < 4) {
                faceA = faceA - orientation and 3
            }
            if (faceB < 4) {
                faceB = faceB - orientation and 3
            }
            if (faceC < 4) {
                faceC = faceC - orientation and 3
            }
            indices.add(RS2Model.Face(faceA, faceC, faceB, overlayOrUnderlay))
        }

        return indices
    }

    fun getSimpleVertices(heightD: Float, heightC: Float, heightA: Float, heightB: Float, colourA: Int, colourB: Int, colourC: Int, colourD: Int): List<RS2Vertex> {

        val positions = arrayOf(
            ImmutableVector3(TILE_FULL, TILE_FULL, heightA),
            ImmutableVector3(0f, TILE_FULL, heightB),
            ImmutableVector3(TILE_FULL, 0f, heightC),
            ImmutableVector3(0f, 0f, heightD),
        )
        val colours = arrayOf(colourA, colourB, colourC, colourD)

        val normal = calcNormal(positions[0], positions[1], positions[2])

        val vertices = mutableListOf<RS2Vertex>()
        for(index in positions.indices) {
            val colour = if(colours[index] != 12345678) RS2Colour.colourPalette[colours[index]] else RS2Colour.colourPalette[colours[0]]
            val vertex = RS2Vertex()
            with(vertex) {
                this.position = positions[index]
                this.bone = 0
                this.uv = positions[index].xy
                this.normal = normal
                this.colour = colour

            }
            vertices.add(vertex)
        }
        return vertices
    }



    private fun calcNormal(p1: ImmutableVector3, p2: ImmutableVector3, p3: ImmutableVector3): ImmutableVector3 {

        // u = p3 - p1
        val ux = p3.x - p1.x
        val uy = p3.y - p1.y
        val uz = p3.z - p1.z

        // v = p2 - p1
        val vx = p2.x - p1.x
        val vy = p2.y - p1.y
        val vz = p2.z - p1.z

        // n = cross(v, u)
        var nx = vy * uz - vz * uy
        var ny = vz * ux - vx * uz
        var nz = vx * uy - vy * ux

        // // normalize(n)
        val num2 = nx * nx + ny * ny + nz * nz
        val num = 1f / sqrt(num2.toDouble()).toFloat()
        nx *= num
        ny *= num
        nz *= num
        return ImmutableVector3(nx, ny, nz)
    }

    fun getVertices(type: Int, orientation: Byte, tileHeightA: Float, tileHeightB: Float, tileHeightC: Float, tileHeightD: Float, tileColourA: Int, tileColourB: Int, tileColourC: Int, tileColourD: Int): MutableList<RS2Vertex> {
        val tileShape = tileShapePoints[type]
        val tileShapeLength = tileShape.size
        val vertices = mutableListOf<RS2Vertex>()
        for (index in 0 until tileShapeLength) {
            /*val vertexType = when (tileShape[index]) {
                in 0..8 -> {
                    if ((tileShape[index] and 1) == 0) (tileShape[index] - orientation - orientation - 1 and 7) + 1 else tileShape[index]
                }
                in 9..12 -> {
                    (tileShape[index] - 9 - orientation and 3) + 9
                }
                in 13..16 -> {
                    (tileShape[index] - 13 - orientation and 3) + 13
                }
                else -> tileShape[index]
            }*/

            var vertexType = tileShape[index]
            if (vertexType and 1 == 0 && vertexType <= 8) {
                vertexType = (vertexType - orientation - orientation - 1 and 7) + 1
            }

            if (vertexType in 9..12) {
                vertexType = (vertexType - 9 - orientation and 3) + 9
            }

            if (vertexType in 13..16) {
                vertexType = (vertexType - 13 - orientation and 3) + 13
            }

            var vertexZ: Float
            var vertexX: Float
            var vertexY: Float
            var colour: Int
            when (vertexType) {
                1 -> {
                    vertexX = 0f
                    vertexY = 0f
                    vertexZ = tileHeightA
                    colour = tileColourA
                }
                2 -> {
                    vertexX = TILE_HALF
                    vertexY = 0f
                    vertexZ = (tileHeightA + tileHeightB) / 2
                    colour = tileColourA + tileColourB / 2
                }
                3 -> {
                    vertexX = TILE_FULL
                    vertexY = 0f
                    vertexZ = tileHeightB
                    colour = tileColourB
                }
                4 -> {
                    vertexX = TILE_FULL
                    vertexY = TILE_HALF
                    vertexZ = (tileHeightB + tileHeightC) / 2
                    colour = tileColourC + tileColourB / 2
                }
                5 -> {
                    vertexX = TILE_FULL
                    vertexY = TILE_FULL
                    vertexZ = tileHeightC
                    colour = tileColourC
                }
                6 -> {
                    vertexX = TILE_HALF
                    vertexY = TILE_FULL
                    vertexZ = (tileHeightC + tileHeightD) / 2
                    colour = tileColourC + tileColourD / 2
                }
                7 -> {
                    vertexX = 0f
                    vertexY = TILE_FULL
                    vertexZ = tileHeightD
                    colour = tileColourD
                }
                8 -> {
                    vertexX = 0f
                    vertexY = TILE_HALF
                    vertexZ = (tileHeightD + tileHeightA) / 2
                    colour = tileColourA + tileColourD / 2
                }
                9 -> {
                    vertexX = TILE_HALF
                    vertexY = TILE_QUARTER
                    vertexZ = (tileHeightA + tileHeightB) / 2
                    colour = tileColourA + tileColourB / 2
                }
                10 -> {
                    vertexX = TILE_THREE_QUARTER
                    vertexY = TILE_HALF
                    vertexZ = (tileHeightB + tileHeightC) / 2
                    colour = tileColourC + tileColourB / 2
                }
                11 -> {
                    vertexX = TILE_HALF
                    vertexY = TILE_THREE_QUARTER
                    vertexZ = (tileHeightC + tileHeightD) / 2
                    colour = tileColourC + tileColourD / 2
                }
                12 -> {
                    vertexX = TILE_QUARTER
                    vertexY = TILE_HALF
                    vertexZ = (tileHeightD + tileHeightA) / 2
                    colour = tileColourA + tileColourD / 2
                }
                13 -> {
                    vertexX = TILE_QUARTER
                    vertexY = TILE_QUARTER
                    vertexZ = tileHeightA
                    colour = tileColourA
                }
                14 -> {
                    vertexX = TILE_THREE_QUARTER
                    vertexY = TILE_QUARTER
                    vertexZ = tileHeightB
                    colour = tileColourB
                }
                15 -> {
                    vertexX = TILE_THREE_QUARTER
                    vertexY = TILE_THREE_QUARTER
                    vertexZ = tileHeightC
                    colour = tileColourC
                }
                else -> {
                    vertexX = TILE_QUARTER
                    vertexY = TILE_THREE_QUARTER
                    vertexZ = tileHeightD
                    colour = tileColourD
                }
            }
            val position = ImmutableVector3(vertexX, vertexY, vertexZ)

            val vertexColour = ImmutableColor.rgb888ToColor(colour)
            val vertex = RS2Vertex()
            with(vertex) {
                this.position = position
                this.bone = 0
                this.uv = position.xy
                this.normal = position.nor
                this.colour = vertexColour
            }
            vertices.add(vertex)
        }
        return vertices
    }
}
