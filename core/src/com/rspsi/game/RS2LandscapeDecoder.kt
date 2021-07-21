package com.rspsi.game

import com.badlogic.gdx.graphics.*
import com.badlogic.gdx.graphics.VertexAttribute
import com.badlogic.gdx.graphics.g2d.TextureAtlas.AtlasRegion
import com.badlogic.gdx.graphics.g3d.*
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Vector3
import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.*
import ktx.log.info
import ktx.math.ImmutableVector2
import ktx.math.div
import ktx.math.minus
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class HSLAccumulator {
    var hue: Int = 0
    var saturation: Int = 0
    var lightness: Int = 0
    var hueMultiplier: Int = 0
    var counter: Int = 0

    override fun toString(): String {
        return "HSLAccumulator(hue=$hue, saturation=$saturation, lightness=$lightness, hueMultiplier=$hueMultiplier, counter=$counter)"
    }


}

class RS2LandscapeDecoder {

    init {
        context.bindSingleton { this }
    }


    val width = 64
    val height = 64


    private val cacheLibrary: CacheLibrary = context.inject()
    private val floorDecoder: RS2FloorDecoder = context.inject()


    val tileMap = mutableMapOf<ImmutableVector3, RS2Tile>()

    var overlayTextures: Array<Texture?> = arrayOfNulls(4)
    var underlayTextures: Array<Texture?> = arrayOfNulls(4)

    val hslGlobal = Array(height + 1) { _ -> HSLAccumulator() }
    fun buildColours() {


        for (z in 0 until 4) {

            hslGlobal.forEach {
                it.counter = 0
                it.hue = 0
                it.hueMultiplier = 0
                it.lightness = 0
                it.saturation = 0
            }

            for (x in -5 until width + 5) {
                for (y in 0 until height) {
                    val maxX = ImmutableVector3(x + 5, y, z)
                    val minX = ImmutableVector3(x - 5, y, z)

                    val maxTile = tileMap[maxX]
                    val minTile = tileMap[minX]

                    //  if (z == 0) info { "Begin@: ${hslGlobal[y]} $y" }
                    maxTile?.let {
                        if (maxTile.data.underlayId > 0) {
                            val underlay = floorDecoder.underlays[maxTile.data.underlayId - 1]
                            underlay?.let { floor ->
                                hslGlobal[y].apply {
                                    // info { "Added $floor" }
                                    hue += floor.weightedHue
                                    saturation += floor.saturation
                                    lightness += floor.lightness
                                    hueMultiplier += floor.hueMultiplier
                                    counter++
                                }

                            }
                            if (underlay == null) info { "Underlay ${maxTile.data.underlayId - 1} missing" }
                        } else if (z == 0) info { "Underlay is ${maxTile.data.underlayId} at $maxX" }
                    }

                    //  if (z == 0) info { "Mid@: ${hslGlobal[y]} $y" }
                    minTile?.let {
                        if (minTile.data.underlayId > 0) {
                            val underlay = floorDecoder.underlays[minTile.data.underlayId - 1]
                            underlay?.let { floor ->
                                //info { "Removed $floor" }
                                hslGlobal[y].apply {
                                    hue -= floor.weightedHue
                                    saturation -= floor.saturation
                                    lightness -= floor.lightness
                                    hueMultiplier -= floor.hueMultiplier
                                    counter--
                                }
                            }
                            if (underlay == null) info { "Underlay ${minTile.data.underlayId - 1} missing" }
                        }// else if (z == 0) info { "Underlay is ${minTile?.data?.underlayId} at $minX" }

                    }


                    // if (z == 0) info { "End@: ${hslGlobal[y]} $y" }
                }


                // for (centerX in 0 until sizeX) {
                if (x in 0 until width) {
                    val accumulator = HSLAccumulator()
                    for (y in -5 until height + 5) {
                        val maxY = y + 5
                        val minY = y - 5

                        val center = ImmutableVector3(x, y, z)

                        val currentTile = tileMap[center] ?: continue
                        if (currentTile.data.let { it.underlayId <= 0 && it.overlayId <= 0 }) {
                            continue
                        }

                        if (maxY in 0 until height) {
                            //  if (z == 0)
                            //  info { "adjusting + with ${hslGlobal[maxY]} $maxY" }
                            hslGlobal[maxY].let {
                                with(accumulator) {
                                    hue += it.hue
                                    saturation += it.saturation
                                    lightness += it.lightness
                                    hueMultiplier += it.hueMultiplier
                                    counter += it.counter
                                }

                            }

                        }

                        if (minY in 0 until height) {
                            //  if (z == 0)
                            //   info { "adjusting - with ${hslGlobal[minY]} $minY" }
                            hslGlobal[minY].let {
                                with(accumulator) {
                                    hue -= it.hue
                                    saturation -= it.saturation
                                    lightness -= it.lightness
                                    hueMultiplier -= it.hueMultiplier
                                    counter -= it.counter
                                }

                            }
                        }



                        tileMap[center]?.let { tile ->
                            var hslValue = -1
                            var finalHue = -1
                            var sat = 0
                            var lightness = 0

                            var rgbRaw = 0
                            if (tile.data.overlayId > 0) {

                            }
                            if (tile.data.underlayId > 0) {
                                accumulator.apply {
                                    if (counter == 0)
                                        counter = 1
                                    if (hueMultiplier < 1)
                                        hueMultiplier = 1
                                }
                                rgbRaw = floorDecoder.underlays[tile.data.underlayId - 1]?.rgb ?: 0

                                finalHue = accumulator.let { it.hue shl 8 / it.hueMultiplier }
                                sat = accumulator.let { it.saturation / it.counter }
                                lightness = accumulator.let { it.lightness / it.counter }
                                hslValue = toHsl(finalHue, sat, lightness)


                                //finalHue = (finalHue - 8 and 0xFF)
                                //lightness -= 16

                                lightness.coerceIn(0, 255)

                            }


                            var hslLightClamped = -1

                            if (finalHue != -1)
                                hslLightClamped = toHsl(finalHue, sat, lightness)
                            if (hslValue == -1)
                                hslValue = hslLightClamped

                            var rgbLit = 0

                            if (hslValue != -1) {
                                val litValue = light(hslLightClamped, 96)
                                if (litValue != 12345678)
                                    rgbLit = RS2Colour.colourPalette[litValue].toRGBIntBits()
                            }


                            tile.model.apply {
                                this.hslRaw = hslValue
                                this.hslLightClamped = hslLightClamped
                                this.rgbLit = rgbLit
                                this.rgbRaw = rgbRaw
                            }

                        }
                    }

                }
            }
        }


    }

    fun buildLandscape() {
        (0 until 4).map { z ->
            tileMap
                .filter { (pos, tile) -> pos.z == z.toFloat() && tile.data.underlayId > 0 }
                .forEach { (pos, tile) ->


                    for (x in -1..1) {
                        for (y in -1..1) {
                            (pos + (x withY y withZ 0)).let { pos ->
                                tileMap[pos]?.let { other ->
                                    tile.model.apply {
                                        localColours[pos] = other.model.rgbRaw
                                        localHeights[pos] = other.data.tileHeight
                                    }
                                }
                            }
                        }
                    }
                }


        }
    }

    fun request(regionId: Int): Boolean {

        val x = (regionId shr 8) and 0xFF
        val y = regionId and 0xFF
        val data = cacheLibrary.data(RS2CacheInfo.Indexes.MAPS, "m${x}_${y}")
        return data?.let {
            decode(regionId, it)
            true
        } ?: false
    }

    private fun calcNormal(a: Vector3, b: Vector3, c: Vector3): Vector3 {
        val u = b.minus(a)
        val v = c.minus(b)
        return u.crs(v)
    }

    fun decode(regionId: Int, data: ByteArray) {
        tileMap.clear()
        val regionX = (regionId shr 8) and 0xFF
        val regionY = regionId and 0xFF
        val buffer = InputBuffer(data)
        for (z in 0 until 4) {
            for (x in 0 until 64) {
                for (y in 0 until 64) {
                    val pos = x withY y withZ z
                    val tile = RS2Tile(pos)

                    tileMap[pos] = tile

                    val tileDataBelow: RS2Tile? = tileMap[pos - ImmutableVector3(0f, 0f, 1f)]


                    tile.data.apply {
                        while (true) {
                            when (val type = buffer.readUnsignedByte()) {
                                0 -> {
                                    tileHeightGenerated = true
                                    if (z == 0) {
                                        tileHeight = calculateHeight(
                                            (regionX shl 6) + x + 0xe3b7b,
                                            (regionY shl 8) + y + 0x87cce
                                        ) * 8f
                                    } else {
                                        tileDataBelow?.data?.let {
                                            tileHeight = it.tileHeight + 240f
                                        }
                                    }
                                    return@apply
                                }

                                1 -> {
                                    tileHeightGenerated = false
                                    var height = buffer.readUnsignedByte()
                                    if (height == 1)
                                        height = 0
                                    if (z == 0) {
                                        tileHeight = height.toFloat() * 8f
                                    } else {
                                        tileDataBelow?.data?.let {
                                            tileHeight = it.tileHeight + (height * 8f)
                                        }
                                    }
                                    return@apply
                                }
                                in 2..49 -> {
                                    overlayId = buffer.readByte().toInt()
                                    overlayShape = ((type - 2) / 4).toByte()
                                    overlayOrientation = ((type - 2)).toByte()
                                }
                                in 50..81 -> {
                                    flag = type - 49
                                }
                                else -> {
                                    underlayId = type - 81
                                }

                            }
                        }
                    }


                }
            }
        }


        buildLandscape()
        buildLighting()
        buildColours()

    }


    fun buildUnderlay(z: Int): Model? {
        val underlayPixmap = getUnderlayPixmap(z)
        underlayTextures[z] = Texture(underlayPixmap)
        val modelBuilder = ModelBuilder()

        val underlayTileCount =
            tileMap.filter { (pos, tile) -> pos.z == z.toFloat() && tile.data.underlayId > 0 }.size

        if(underlayTileCount == 0)
            return null

        return modelBuilder.use {

            val underlayMaterial = Material()//Material(TextureAttribute.createDiffuse(underlayTextures[z]))
            val underlayVertexAttributes = VertexAttributes(
                VertexAttribute.Position(),
                VertexAttribute.ColorPacked()
            )//, VertexAttribute.Normal())

            val part = modelBuilder.part(
                "underlay_${z}",
                GL20.GL_TRIANGLES,
                underlayVertexAttributes,
                underlayMaterial
            )
            tileMap
                .filter { (pos, tile) -> pos.z == z.toFloat() && tile.data.underlayId > 0 }
                .forEach { (tilePosition, tile) ->

                    val tileUVOffset = tilePosition.xy / 64f
                    val tileB = tileMap[tilePosition east 1] ?: tile
                    val tileC = tileMap[tilePosition northeast 1] ?: tile
                    val tileD = tileMap[tilePosition north 1] ?: tile


                    val vertices = RS2TileShape.getSimpleVertices(
                        tile.data.tileHeight,
                        tileB.data.tileHeight,
                        tileC.data.tileHeight,
                        tileD.data.tileHeight,
                        light(tile.model.hslRaw, tile.model.heightLighting),
                        light(tile.model.hslRaw, tileB.model.heightLighting),
                        light(tile.model.hslRaw, tileC.model.heightLighting),
                        light(tile.model.hslRaw, tileD.model.heightLighting)

                    )

                    if (vertices.size < 4) {
                        info {
                            "Found ${vertices.size} verts at ${tile.data.position}"
                        }
                    }

                    vertices.forEach { vertex ->

                        vertex.uv /= 64f
                        vertex.uv += tileUVOffset


                        vertex.position += tilePosition.xy withZ 0
                        vertex.position += ImmutableVector3(0f, 0f, tilePosition.z * 240f)
                        vertex.position /= ImmutableVector3(1f, 1f, 128f)
                        vertex.position = vertex.position.flipYZ()

                    }

                    var quad = 0
                    vertices.chunked(4).forEach { vertInfo ->
                        part.triangle(vertInfo[2], vertInfo[1], vertInfo[0], underlayVertexAttributes)
                        part.triangle(vertInfo[2], vertInfo[3], vertInfo[1], underlayVertexAttributes)
                        quad++
                    }
                }
        }


    }

    fun buildOverlay(z: Int): Model? {
        val overlayPixmap = getOverlayUV(z)
        overlayTextures[z] = Texture(overlayPixmap)
        val floorDecoder: RS2FloorDecoder = context.inject()
        val textureProvider: RS2TextureProvider = context.inject()

        val overlayTileCount =
            tileMap.filter { (pos, tile) -> pos.z == z.toFloat() && tile.data.overlayId > 0 }.size

        if(overlayTileCount == 0)
            return null

        val modelBuilder = ModelBuilder()

        return modelBuilder.use { modelBuilder ->


            val overlayVertexAttributes =
                VertexAttributes(
                    VertexAttribute.Position(),
                    VertexAttribute.TexCoords(0)
                )//, VertexAttribute.Normal())
            val overlayMaterial = Material(TextureAttribute.createDiffuse(overlayTextures[z]))


            tileMap.filter { (pos, tile) -> pos.z == z.toFloat() && tile.data.overlayId > 0 }
                .forEach { (tilePosition, tile) ->

                    val tileUVOffset = tilePosition.xy / 64f
                    val overlay = floorDecoder.overlays[tile.data.overlayId - 1]
                    val textureId = overlay?.texture ?: -1

                    val tileA = tile.data//south west
                    val tileB = tileMap[tilePosition + ImmutableVector3(1f, 0f, 0f)]?.data ?: tileA//south east
                    val tileC = tileMap[tilePosition + ImmutableVector3(1f, 1f, 0f)]?.data ?: tileA//north east
                    val tileD = tileMap[tilePosition + ImmutableVector3(0f, 1f, 0f)]?.data ?: tileA//north west

                    val tileHeightA = tileA.tileHeight
                    val tileHeightB = tileB.tileHeight
                    val tileHeightC = tileC.tileHeight
                    val tileHeightD = tileD.tileHeight

                    overlay?.let {

                        val vertices = RS2TileShape.getVertices(
                            tile.data.overlayShape + 1, tile.data.overlayOrientation,
                            tileHeightA, tileHeightB, tileHeightC, tileHeightD,
                            overlay.rgb, overlay.rgb, overlay.rgb, overlay.rgb
                        )

                        val indices =
                            RS2TileShape.getIndices(tile.data.overlayShape + 1, tile.data.overlayOrientation)

                        //val overlayTile = modelBuilder.part("terrain_overlay_${tilePosition.x}_${tilePosition.y}", GL20.GL_TRIANGLES, uvVertexAttributes, overlayMaterial)


                        vertices.forEach { vertex ->

                            if (textureId < 0) {
                                vertex.uv /= 64f
                                vertex.uv += tileUVOffset
                            }


                            vertex.position += tilePosition.xy withZ 0
                            //  vertex.position += ImmutableVector3(0f, 0f, tilePosition.z * 128f)
                            vertex.position /= ImmutableVector3(1f, 1f, 128f)
                            vertex.position += ImmutableVector3(0f, 0f, 0.15f)
                            vertex.position = vertex.position.flipYZ()


                        }
                        val part = modelBuilder.part(
                            "overlay_${tilePosition.x}_${tilePosition.y}_${tilePosition.z}",
                            GL20.GL_TRIANGLES,
                            overlayVertexAttributes,
                            if (textureId >= 0) textureProvider.materials[textureId] else overlayMaterial
                        )
                        indices.forEach { face ->

                            if (face.type == 1) {

                                val a = vertices[face.a]
                                val b = vertices[face.b]
                                val c = vertices[face.c]

                                part.triangle(a, b, c, overlayVertexAttributes)
                            }

                        }
                    }
                }
        }

    }

    fun remapUVs(mesh: Mesh, region: AtlasRegion) {
        val UVs = mesh.vertexAttributes.findByUsage(VertexAttributes.Usage.TextureCoordinates)
        val verts = FloatArray(mesh.vertexSize * mesh.numVertices)
        mesh.getVertices(verts)
        var i = 0
        while (i < verts.size) {
            verts[UVs.offset] = region.u + region.u2 * verts[UVs.offset]
            verts[UVs.offset + 1] = region.v + region.v2 * verts[UVs.offset + 1]
            i += mesh.vertexSize
        }
        mesh.setVertices(verts)
    }


    fun toHsl(hue: Int, saturation: Int, luminance: Int): Int {
        var saturation = saturation
        if (luminance > 179) {
            saturation /= 2
        }
        if (luminance > 192) {
            saturation /= 2
        }
        if (luminance > 217) {
            saturation /= 2
        }
        if (luminance > 243) {
            saturation /= 2
        }
        return (hue / 4 shl 10) + (saturation / 32 shl 7) + luminance / 2
    }


    fun buildLighting() {

        val shading = mutableMapOf<ImmutableVector3, Int>()//TODO
        val byte0: Byte = 96
        val diffusion = '\u0300'
        val lightX: Byte = -50
        val lightY: Byte = -10
        val lightZ: Byte = -50

        val light = diffusion.toInt() * sqrt((lightX * lightX + lightY * lightY + lightZ * lightZ).toDouble())
            .toInt() shr 8

        val tileHeights = tileMap.mapValues { it.value.data.tileHeight.toInt() }
        for (z in 0 until 4)
            for (y in 1 until height) {
                for (x in 1 until width) {

                    val position = ImmutableVector3(x, y, z)

                    tileMap[position]?.let { tile ->


                        val north = position north 1
                        val east = position east 1
                        val south = position south 1
                        val west = position west 1


                        val dhWidth: Int = (tileHeights[east]?.let { it - (tileHeights[west] ?: 0) } ?: 0).toInt()
                        val dhLength: Int =
                            (tileHeights[north]?.let { it - (tileHeights[south] ?: 0) } ?: 0).toInt()
                        var distance = sqrt((dhWidth * dhWidth + 0x10000 + dhLength * dhLength).toDouble()).toInt()
                        if (distance == 0) {
                            distance = 1
                        }
                        val dx = (dhWidth shl 8) / distance
                        val dy = 0x10000 / distance
                        val dz = (dhLength shl 8) / distance
                        val lightness = byte0 + (lightX * dx + lightY * dy + lightZ * dz) / light
                        val offset: Int = ((shading[west] ?: 0 shr 2)
                                + (shading[east] ?: 0 shr 3)
                                + (shading[south] ?: 0 shr 2)
                                + (shading[north] ?: 0 shr 3)
                                + (shading[position] ?: 0 shr 1))

                        tile.model.heightLighting = lightness - offset
                    }

                }
            }
    }


    fun uvBackgroundColour(z: Number): Int = ImmutableColor.rgba8888(
        when (z.toInt()) {
            0 -> ImmutableColor.RED
            1 -> ImmutableColor.GREEN
            2 -> ImmutableColor.YELLOW
            else -> ImmutableColor.BLUE
        }
    )

    fun uvBackgroundColour2(z: Number): Int = ImmutableColor.rgba8888(
        when (z.toInt()) {
            0 -> ImmutableColor.CYAN
            1 -> ImmutableColor.FIREBRICK
            2 -> ImmutableColor.FOREST
            else -> ImmutableColor.MAGENTA
        }
    )

    fun getUnderlayPixmap(z: Int): Pixmap =
        Pixmap(64, 64, Pixmap.Format.RGBA8888).apply {
            blending = Pixmap.Blending.None

            setColor(ImmutableColor.WHITE)
            fill()
            fillRect(this, ImmutableVector2.ZERO, uvBackgroundColour(z), 64, 64)


            tileMap
                .filter { it.key.z == z.toFloat() }
                .filter { it.value.data.underlayId > 0 }
                .forEach { (pos, tile) ->
                    fillRect(this, pos.xy, tile.model.rgbLit, 1, 1)

                }
        }


    fun getHeightmapPixmap(z: Int): Pixmap =
        Pixmap(64, 64, Pixmap.Format.RGBA4444).apply {
            blending = Pixmap.Blending.None

            setColor(ImmutableColor.BLACK)
            fill()
            tileMap
                .filter { it.key.z == z.toFloat() }
                .forEach { (pos, tile) ->
                    val tileHeight = tile.data.tileHeight
                    fillRect(
                        this, pos.xy, ImmutableColor.rgba4444(
                            -(tileHeight / Companion.MAX_TILE_HEIGHT) / 255.0f,
                            -(tileHeight / Companion.MAX_TILE_HEIGHT) / 255.0f,
                            -(tileHeight / Companion.MAX_TILE_HEIGHT) / 255.0f,
                            1f
                        ), 1, 1
                    )
                }

        }

    fun getOverlayUV(z: Int): Pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888).apply {
        blending = Pixmap.Blending.None

        tileMap
            .filter { it.key.z == z.toFloat() }
            .filter { it.value.data.overlayId > 0 }
            .forEach { (pos, tile) ->
                floorDecoder.overlays[tile.data.overlayId - 1]?.let {
                    fillRect(this, pos.xy, it.rgb, 1, 1)
                }
            }

    }

    fun fillRect(pixmap: Pixmap, pos: ImmutableVector2, rgb: ImmutableColor, width: Int = 1, height: Int = 1) {
        pixmap.setColor(rgb)
        pixmap.fillRectangle(
            pos.x.toInt(), pos.y.toInt(), width, height
        )
    }

    fun fillRect(pixmap: Pixmap, pos: ImmutableVector2, rgb: Int, width: Int = 1, height: Int = 1) {
        pixmap.setColor(ImmutableColor.rgb888ToColor(rgb))
        pixmap.fillRectangle(
            pos.x.toInt(), pos.y.toInt(), width, height
        )
    }

    private fun packHsl(var0: Int, var1: Int, var2: Int): Int {
        var var1 = var1
        if (var2 > 179) {
            var1 /= 2
        }
        if (var2 > 192) {
            var1 /= 2
        }
        if (var2 > 217) {
            var1 /= 2
        }
        if (var2 > 243) {
            var1 /= 2
        }
        return (var1 / 32 shl 7) + (var0 / 4 shl 10) + var2 / 2
    }

    private fun perlinNoise(x: Int, y: Int): Int {
        var n = x + y * 57
        n = n shl 13 xor n
        n = n * (n * n * 15731 + 0xc0ae5) + 0x5208dd0d and 0x7fffffff
        return n shr 19 and 0xff
    }

    private fun smoothNoise(x: Int, y: Int): Int {
        val corners = (perlinNoise(x - 1, y - 1) + perlinNoise(x + 1, y - 1) + perlinNoise(x - 1, y + 1)
                + perlinNoise(x + 1, y + 1))
        val sides = perlinNoise(x - 1, y) + perlinNoise(x + 1, y) + perlinNoise(x, y - 1) + perlinNoise(x, y + 1)
        val center = perlinNoise(x, y)
        return corners / 16 + sides / 8 + center / 4
    }


    private fun calculateHeight(x: Int, y: Int): Float {
        var height = interpolatedNoise(x + 45365, y + 0x16713, 4) - 128 + (interpolatedNoise(
            x + 10294,
            y + 37821,
            2
        ) - 128 shr 1) + (interpolatedNoise(x, y, 1) - 128 shr 2)
        height = (height * 0.3).toInt() + 35
        if (height < 10) {
            height = 10
        } else if (height > 60) {
            height = 60
        }
        return height.toFloat()
    }

    private fun interpolate(a: Int, b: Int, angle: Int, frequencyReciprocal: Int): Int {
        val cosine: Int = 0x10000 - cosine[angle * 1024 / frequencyReciprocal] shr 1
        return (a * (0x10000 - cosine) shr 16) + (b * cosine shr 16)
    }

    private fun interpolatedNoise(x: Int, y: Int, frequencyReciprocal: Int): Int {
        val adj_x = x / frequencyReciprocal
        val i1 = x and frequencyReciprocal - 1
        val adj_y = y / frequencyReciprocal
        val k1 = y and frequencyReciprocal - 1
        val l1: Int = smoothNoise(adj_x, adj_y)
        val i2: Int = smoothNoise(adj_x + 1, adj_y)
        val j2: Int = smoothNoise(adj_x, adj_y + 1)
        val k2: Int = smoothNoise(adj_x + 1, adj_y + 1)
        val l2 = interpolate(l1, i2, i1, frequencyReciprocal)
        val i3 = interpolate(j2, k2, i1, frequencyReciprocal)
        return interpolate(l2, i3, k1, frequencyReciprocal)
    }


    companion object {
        val cosine = IntArray(2048)
        val sine = IntArray(2048)

        init {
            for (theta in 0..2047) {
                sine[theta] = (65536.0 * sin(theta * 0.0030679614999999999)).toInt()
                cosine[theta] = (65536.0 * cos(theta * 0.0030679614999999999)).toInt()
            }
        }

        val MAX_TILE_HEIGHT = 2048
    }

}


fun Attributes.setAll(vararg attributes: Attribute) {
    attributes.forEach { this.set(it) }
}

