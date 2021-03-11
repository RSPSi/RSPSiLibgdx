package com.rspsi.game

import com.badlogic.gdx.graphics.Pixmap
import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.setColor
import com.rspsi.ext.withAlpha

open class RS2SpriteProvider {

    init {
        context.bindSingleton { this }
    }


    var spriteCache = mutableMapOf<Int, RS2Sprite>()

    open fun load() {
        val cacheLibrary: CacheLibrary = context.inject()
        val index = cacheLibrary.index(8)
        for (archiveId in index.archiveIds()) {
            cacheLibrary.data(RS2CacheInfo.Indexes.SPRITES, archiveId)?.let { data ->
                val sprites = load(archiveId, data)
                sprites.forEach {
                    spriteCache[it.id] = it
                }

            }
        }


    }


    fun load(id: Int, b: ByteArray): MutableList<RS2Sprite> {
        val buffer = InputBuffer(b)
        buffer.offset = b.size - 2
        val spriteCount: Int = buffer.readUnsignedShort()
        val sprites = mutableListOf<RS2Sprite>()

        // 2 for size
        // 5 for width, height, palette length
        // + 8 bytes per sprite for offset x/y, width, and height
        buffer.offset = b.size - 7 - spriteCount * 8

        // max width and height
        val width: Int = buffer.readUnsignedShort()
        val height: Int = buffer.readUnsignedShort()
        val paletteLength: Int = buffer.readUnsignedByte() + 1
        for (i in 0 until spriteCount) {
            val sprite = RS2Sprite(id)
            sprite.maxWidth = width
            sprite.maxHeight = height
            sprite.frame = i
            sprites.add(sprite)
        }
        for (i in 0 until spriteCount) {
            sprites[i].offsetX = (buffer.readUnsignedShort())
        }
        for (i in 0 until spriteCount) {
            sprites[i].offsetY = (buffer.readUnsignedShort())
        }
        for (i in 0 until spriteCount) {
            sprites[i].width = (buffer.readUnsignedShort())
        }
        for (i in 0 until spriteCount) {
            sprites[i].height = (buffer.readUnsignedShort())
        }

        // same as above + 3 bytes for each palette entry, except for the first one (which is transparent)
        buffer.offset = (b.size - 7 - spriteCount * 8 - (paletteLength - 1) * 3)
        val palette = IntArray(paletteLength)
        for (i in 1 until paletteLength) {
            palette[i] = buffer.read24BitInt()
            if (palette[i] == 0) {
                palette[i] = 1
            }
        }
        buffer.offset = 0
        for (i in 0 until spriteCount) {
            val def = sprites[i]
            val spriteWidth: Int = def.width
            val spriteHeight: Int = def.height
            val dimension = spriteWidth * spriteHeight
            val pixelPaletteIndicies = ByteArray(dimension)
            val pixelAlphas = ByteArray(dimension)
            val flags: Int = buffer.readUnsignedByte()
            if (flags and FLAG_VERTICAL == 0) {
                // read horizontally
                for (j in 0 until dimension) {
                    pixelPaletteIndicies[j] = buffer.readByte()
                }
            } else {
                // read vertically
                for (j in 0 until spriteWidth) {
                    for (k in 0 until spriteHeight) {
                        pixelPaletteIndicies[spriteWidth * k + j] = buffer.readByte()
                    }
                }
            }

            // read alphas
            if (flags and FLAG_ALPHA != 0) {
                if (flags and FLAG_VERTICAL == 0) {
                    // read horizontally
                    for (j in 0 until dimension) {
                        pixelAlphas[j] = buffer.readByte()
                    }
                } else {
                    // read vertically
                    for (j in 0 until spriteWidth) {
                        for (k in 0 until spriteHeight) {
                            pixelAlphas[spriteWidth * k + j] = buffer.readByte()
                        }
                    }
                }
            } else {
                // everything non-zero is opaque
                for (j in 0 until dimension) {
                    val index = pixelPaletteIndicies[j].toInt()
                    if (index != 0) pixelAlphas[j] = 0xFF.toByte()
                }
            }
            val pixels = IntArray(dimension)

            // build argb pixels from palette/alphas
            for (j in 0 until dimension) {
                val index: Int = pixelPaletteIndicies[j].toInt() and 0xFF
                pixels[j] = palette[index] or (pixelAlphas[j].toInt() shl 24)
            }
            def.pixels = (pixels)
            def.palletteIndices = pixelPaletteIndicies
            def.pallette = palette
        }
        return sprites
    }

    companion object {

        const val FLAG_VERTICAL = 1
        const val FLAG_ALPHA = 2
    }

}

data class RS2Sprite(
        val id: Int, var frame: Int = 0,
        var width: Int = 0, var height: Int = 0,
        var maxWidth: Int = 0, var maxHeight: Int = 0,
        var offsetX: Int = 0, var offsetY: Int = 0,
        var pallette: IntArray = intArrayOf(),
        var palletteIndices: ByteArray = byteArrayOf(),
        var alphaPixels: IntArray = intArrayOf(),
) {
    var pixels = intArrayOf()


    fun normalize() {
        if (width != maxWidth || height != maxHeight) {
            val var1 = ByteArray(maxWidth * maxHeight)
            var var2 = 0
            for (var3 in 0 until height) {
                for (var4 in 0 until width) {
                    var1[var4 + (var3 + offsetY) * maxWidth + offsetX] = this.palletteIndices[var2++]
                }
            }
            this.palletteIndices = var1
            width = maxWidth
            height = maxHeight
            offsetX = 0
            offsetY = 0
        }
    }

    fun toPixmap(): Pixmap {
        val pixmap = Pixmap(width, height, Pixmap.Format.RGBA8888)
        for (x in 0 until width) {
            for (y in 0 until height) {
                val alpha =  if(pixels[x + (y * width)] == 0) 0f else 1f
                val colour = ImmutableColor.rgb888ToColor(pixels[x + (y * width)]) withAlpha alpha
                pixmap.setColor(colour)
                pixmap.drawPixel(x, y)
            }

        }
        return pixmap
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RS2Sprite

        if (id != other.id) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (offsetX != other.offsetX) return false
        if (offsetY != other.offsetY) return false
        if (!pallette.contentEquals(other.pallette)) return false
        if (!palletteIndices.contentEquals(other.palletteIndices)) return false
        if (!alphaPixels.contentEquals(other.alphaPixels)) return false
        if (!pixels.contentEquals(other.pixels)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + offsetX
        result = 31 * result + offsetY
        result = 31 * result + pallette.contentHashCode()
        result = 31 * result + palletteIndices.contentHashCode()
        result = 31 * result + alphaPixels.contentHashCode()
        result = 31 * result + pixels.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "RS2Sprite(id=$id, frame=$frame, width=$width, height=$height, maxWidth=$maxWidth, maxHeight=$maxHeight, offsetX=$offsetX, offsetY=$offsetY, pallette=${pallette.contentToString()}, palletteIndices=${palletteIndices.contentToString()})"
    }
}