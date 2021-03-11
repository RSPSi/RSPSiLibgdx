package com.rspsi.game

import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.readUTriByte
import ktx.log.info
import java.util.*

class RS2FloorDecoder {

    init {
        context.bindSingleton { this }
    }


    val overlays = mutableMapOf<Int, Floor>()
    val underlays = mutableMapOf<Int, Floor>()
    open fun load(){
        val cacheLibrary: CacheLibrary = context.inject()
        val configIndex = cacheLibrary.index(RS2CacheInfo.Indexes.CONFIGS)
        val overlaysArchive = configIndex.archive(RS2CacheInfo.ConfigTypes.OVERLAY)
        overlaysArchive?.let { archive ->
            archive.files.values.forEach { file ->
                file.data?.let {
                    val floor = Floor(file.id)
                    floor.decode(it)
                    floor.generateHSL()
                    overlays[file.id] = floor
                }
            }
        }
        val underlaysArchive = configIndex.archive(RS2CacheInfo.ConfigTypes.UNDERLAY)
        underlaysArchive?.let { archive ->
            archive.files.values.forEach { file ->
                file.data?.let {
                    val floor = Floor(file.id)
                    floor.decode2(it)
                    floor.generateHSL()
                    underlays[file.id] = floor
                }
            }
        }

        info {
            "Loaded ${underlays.size} underlays and ${overlays.size} overlays"
        }

    }

}


class Floor(val id: Int, var rgb: Int = 0, var texture: Int = -1, var shadowed: Boolean = true, var rgbSecondary: Int = -1) {

    fun decode(data: ByteArray){
        val buffer = InputBuffer(data)
        while(true){
            when(buffer.readUnsignedByte()){
                0 -> return
                1 -> rgb = buffer.readUTriByte()
                2 -> texture = buffer.readUnsignedByte()
                5 -> shadowed = false
                7 -> rgbSecondary = buffer.readUTriByte()
                else -> {
                    info {
                        "INCORRECT OPCODE"
                    }
                }
            }
        }
    }

    fun decode2(data: ByteArray){
        val buffer = InputBuffer(data)
        while(true){
            when(buffer.readUnsignedByte()){
                0 -> return
                1 -> rgb = buffer.readUTriByte()
                else -> {
                    info {
                        "INCORRECT OPCODE"
                    }
                }
            }
        }
    }

    fun generateHSL() {
        if(rgbSecondary != -1) {
            calculateHsl(rgbSecondary)
            secondaryHue = hue
            secondaryLightness = lightness
            secondarySaturation = saturation
        }
        calculateHsl(rgb)
    }


    var secondaryHue = 0
    var secondarySaturation = 0
    var secondaryLightness = 0

    var hue = 0
    var saturation = 0
    var lightness = 0
    var weightedHue = 0
    var hueMultiplier = 0
    var hsl16Packed = 0

    fun calculateHsl(colour: Int) {
        val var2 = ((colour shr 16) and 255) / 256.0
        val var4 = ((colour shr 8) and 255) / 256.0
        val var6 = (colour and 255) / 256.0
        var var8 = var2
        if (var4 < var2) {
            var8 = var4
        }
        if (var6 < var8) {
            var8 = var6
        }
        var var10 = var2
        if (var4 > var2) {
            var10 = var4
        }
        if (var6 > var10) {
            var10 = var6
        }
        var h = 0.0
        var var14 = 0.0
        val var16 = (var10 + var8) / 2.0
        if (var8 != var10) {
            if (var16 < 0.5) {
                var14 = (var10 - var8) / (var8 + var10)
            }
            if (var16 >= 0.5) {
                var14 = (var10 - var8) / (2.0 - var10 - var8)
            }
            if (var2 == var10) {
                h = (var4 - var6) / (var10 - var8)
            } else if (var10 == var4) {
                h = 2.0 + (var6 - var2) / (var10 - var8)
            } else if (var10 == var6) {
                h = 4.0 + (var2 - var4) / (var10 - var8)
            }
        }
        h /= 6.0
        this.hue = (h * 256.0).toInt()
        this.saturation = (var14 * 256.0).toInt().coerceIn(0, 255)
        this.lightness = (var16 * 256.0).toInt().coerceIn(0, 255)

        if (var16 > 0.5) {
            this.hueMultiplier = ((1.0 - var16) * var14 * 512.0).toInt()
        } else {
            this.hueMultiplier = (var14 * var16 * 512.0).toInt()
        }
        if (this.hueMultiplier < 1) {
            this.hueMultiplier = 1
        }
        this.weightedHue = (this.hueMultiplier * h).toInt()
        this.hsl16Packed = hsl24to16(hue, saturation, lightness)

    }

    private fun hsl24to16(hue: Int, saturation: Int, lightness: Int): Int {
        var s = saturation
        if (lightness > 179) {
            s /= 2
        }
        if (lightness > 192) {
            s /= 2
        }
        if (lightness > 217) {
            s /= 2
        }
        if (lightness > 243) {
            s /= 2
        }
        return (hue / 4 shl 10) + (s / 32 shl 7) + lightness / 2
    }

    override fun toString(): String {
        return "Floor(id=$id, rgb=$rgb, texture=$texture, shadowed=$shadowed, rgbSecondary=$rgbSecondary, secondaryHue=$secondaryHue, secondarySaturation=$secondarySaturation, secondaryLightness=$secondaryLightness, hue=$hue, saturation=$saturation, lightness=$lightness, weightedHue=$weightedHue, hueMultiplier=$hueMultiplier, hsl16Packed=$hsl16Packed)"
    }


}