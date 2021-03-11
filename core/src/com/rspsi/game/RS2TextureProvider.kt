package com.rspsi.game

import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.glutils.PixmapTextureData
import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.setColor
import com.rspsi.ext.withAlpha
import com.rspsi.game.RS2Colour.adjustRGB
import ktx.log.info
import kotlin.experimental.and

class RS2TextureProvider {

    init {
        context.bindSingleton { this }
    }

    var textureAtlas: TextureAtlas? = null

    val spriteProvider: RS2SpriteProvider = context.inject()

    val materials = mutableMapOf<Int, Material>()
    lateinit var material: Material
    val textureDefinitions = mutableMapOf<Int, TextureDefinition>()


    fun load() {
        val cacheLibrary: CacheLibrary = context.inject()

        val textureArchive = cacheLibrary.index(RS2CacheInfo.Indexes.TEXTURES).archive(0)

        textureArchive?.let { archive ->
            archive.files.forEach { (id, file) ->
                file.data?.let { data ->
                    val buffer = InputBuffer(data)
                    val textureDef = TextureDefinition(id)

                    textureDef.colour = buffer.readUnsignedShort()
                    textureDef.opaque = buffer.readBoolean()
                    val size = buffer.readUnsignedByte()
                    textureDef.spriteFileIds = (0 until size).map { buffer.readUnsignedShort() }.toIntArray()

                    if (size > 1) {
                        textureDef.textureType = (0 until size - 1).map { buffer.readUnsignedByte() }.toIntArray()
                        textureDef.field1781 = (0 until size - 1).map { buffer.readUnsignedByte() }.toIntArray()

                    }

                    textureDef.field1786 = (0 until size).map { buffer.readInt() }.toIntArray()

                    textureDef.animationDirection = buffer.readUnsignedByte()
                    textureDef.animationSpeed = buffer.readUnsignedByte()
                    textureDefinitions[id] = textureDef
                }

            }
        }

        info {
            "Loaded ${textureDefinitions.size} texture defs"
        }
        textureDefinitions.values.forEach { it.generatePixels(0.7, 128, spriteProvider) }
        textureAtlas?.dispose()
        textureAtlas = buildTextureAtlas()
        buildMaterial()
    }

    fun buildMaterial() {
        textureAtlas?.let {
            textureDefinitions.forEach { (id, textureDef) ->
                val textureAttribute = TextureAttribute.createDiffuse(it.findRegion("oldschool_texture_$id"))
                val material = Material("oldschool_material_$id", textureAttribute)
                if (!textureDef.opaque)
                    material.set(BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f))

                materials[id] = material

            }

        }
    }

    fun buildTextureAtlas(): TextureAtlas {
        val textureAtlas = TextureAtlas()

        textureDefinitions.forEach { (id, textureDef) ->
            val pixmap = textureDef.getPixmap()
            val data = PixmapTextureData(pixmap, pixmap.format, false, false)
            val texture = Texture(data)

            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            texture.setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)

            textureAtlas.addRegion("oldschool_texture_$id", texture, 0, 0, textureDef.width, textureDef.height)
            pixmap.dispose()
        }
        return textureAtlas
    }


}

class TextureDefinition(
    val id: Int,
    var colour: Int = 0,
    var opaque: Boolean = true,
    var spriteFileIds: IntArray = intArrayOf(),
    var textureType: IntArray = intArrayOf(),
    var field1781: IntArray = intArrayOf(),
    var field1786: IntArray = intArrayOf(),
    var animationDirection: Int = 0,
    var animationSpeed: Int = 0,
    var width: Int = 0,
    var height: Int = 0,
) {


    var pixels = intArrayOf()

    fun generatePixels(brightness: Double, dimension: Int, spriteProvider: RS2SpriteProvider): Boolean {
        val var5 = dimension * dimension
        this.width = dimension
        this.height = dimension
        this.pixels = IntArray(var5)
        for (var6 in this.spriteFileIds.indices) {
            val var7 = spriteProvider.spriteCache[spriteFileIds[var6]]

            try {
                var7?.let { sprite ->
                    var7.normalize()
                    val var8: ByteArray = var7.palletteIndices.copyOf()
                    val var9: IntArray = var7.pallette.copyOf()
                    val var10 = field1786[var6]
                    var var11: Int
                    var var12: Int
                    var var13: Int
                    var var14: Int
                    if (var10 and -16777216 == 50331648) {
                        var11 = var10 and 16711935
                        var12 = var10 shr 8 and 255
                        var13 = 0
                        while (var13 < var9.size) {
                            var14 = var9[var13]
                            if (var14 shr 8 == var14 and 65535) {
                                var14 = var14 and 255
                                var9[var13] = var11 * var14 shr 8 and 16711935 or var12 * var14 and 65280
                            }
                            ++var13
                        }
                    }
                    var11 = 0
                    while (var11 < var9.size) {
                        var9[var11] = adjustRGB(var9[var11], brightness)
                        ++var11
                    }
                    var11 = if (var6 == 0) {
                        0
                    } else {
                        textureType[var6 - 1]
                    }
                    if (var11 == 0) {
                        if (dimension == var7.maxWidth) {
                            var12 = 0
                            while (var12 < var5) {
                                this.pixels[var12] = var9[(var8[var12] and 255.toByte()).toInt()]
                                ++var12
                            }
                        } else if (var7.maxWidth == 64 && dimension == 128) {
                            var12 = 0
                            var13 = 0
                            while (var13 < dimension) {
                                var14 = 0
                                while (var14 < dimension) {
                                    this.pixels[var12++] =
                                        var9[(var8[(var13 shr 1 shl 6) + (var14 shr 1)] and 255.toByte()).toInt()]
                                    ++var14
                                }
                                ++var13
                            }
                        } else {
                            if (var7.maxWidth != 128 || dimension != 64) {
                                info {
                                    "mw: ${var7.maxWidth} dim $dimension"
                                }
                                throw RuntimeException()
                            }
                            var12 = 0
                            var13 = 0
                            while (var13 < dimension) {
                                var14 = 0
                                while (var14 < dimension) {
                                    this.pixels[var12++] =
                                        var9[(var8[(var14 shl 1) + (var13 shl 1 shl 7)] and 255.toByte()).toInt()]
                                    ++var14
                                }
                                ++var13
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                info {
                    "failed to load sprite ${spriteFileIds[var6]}"
                }
            }

        }



        return true
    }

    fun getPixmap(): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        pixmap.blending = Pixmap.Blending.None
        for (x in 0 until width) {
            for (y in 0 until height) {
                val alpha = if (pixels[x + (y * width)] == 0) 0f else 1f
                val colour = ImmutableColor.rgb888ToColor(pixels[x + (y * width)]) withAlpha alpha
                pixmap.setColor(colour)
                pixmap.fillRectangle(x, y, 1, 1)
                /*info {
                    "Drew pixel $x, $y colour as ${pixels[x + (y * width)]}"
                }*/
            }

        }
        return pixmap
    }

    override fun toString(): String {
        return "TextureDefinition(id=$id, colour=$colour, transparent=$opaque, spriteFileIds=${spriteFileIds.contentToString()}, field1780=${textureType.contentToString()}, field1781=${field1781.contentToString()}, field1786=${field1786.contentToString()}, field1783=$animationDirection, field1782=$animationSpeed, width=$width, height=$height)"
    }
}