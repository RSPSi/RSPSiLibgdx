package com.rspsi.game

import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.setColor
import kotlin.math.pow

object RS2Colour {

    val colourPalette = setBrightness(0.6)

    fun setBrightness(exponent: Double): Array<ImmutableColor> {
        val palette = mutableMapOf<Int, ImmutableColor>()
        //exponent += Math.random() * 0.03 - 0.015;
        var j = 0

        for (k in 0 until 512) {
            val d1 = k / 8 / 64f + 0.0078125f
            val d2 = (k and 7) / 8f + 0.0625f

            for (k1 in 0 until 128) {
                val initial = k1 / 128f
                var r = initial
                var g = initial
                var b = initial

                if (d2 != 0.0f) {
                    val d7 = if (initial < 0.5f) {
                        initial * (1.0f + d2)
                    } else {
                        initial + d2 - initial * d2
                    }

                    val d8 = 2f * initial - d7
                    var d9 = d1 + 0.33333333333333331f
                    if (d9 > 1.0f) {
                        d9--
                    }

                    val d10 = d1
                    var d11 = d1 - 0.33333333333333331f
                    if (d11 < 0.0f) {
                        d11++
                    }

                    r = if (6f * d9 < 1.0f) {
                        d8 + (d7 - d8) * 6f * d9
                    } else if (2f * d9 < 1.0f) {
                        d7
                    } else if (3f * d9 < 2f) {
                        d8 + (d7 - d8) * (0.66666666666666663f - d9) * 6f
                    } else {
                        d8
                    }

                    g = if (6f * d10 < 1.0f) {
                        d8 + (d7 - d8) * 6f * d10
                    } else if (2f * d10 < 1.0f) {
                        d7
                    } else if (3f * d10 < 2f) {
                        d8 + (d7 - d8) * (0.66666666666666663f - d10) * 6f
                    } else {
                        d8
                    }

                    b = if (6f * d11 < 1.0f) {
                        d8 + (d7 - d8) * 6f * d11
                    } else if (2f * d11 < 1.0f) {
                        d7
                    } else if (3f * d11 < 2f) {
                        d8 + (d7 - d8) * (0.66666666666666663f - d11) * 6f
                    } else {
                        d8
                    }
                }

                val newR = (r * 256.0).toInt()
                val newG = (g * 256.0).toInt()
                val newB = (b * 256.0).toInt()
                val colour = ImmutableColor.rgb888ToColor((newR shl 16) + (newG shl 8) + newB)
                var lightAdjustedColour = colour.pow(exponent)
                if (lightAdjustedColour.toRGBIntBits() and 0xFFFFFF == 0) {
                    lightAdjustedColour = ImmutableColor.rgb888ToColor(1)
                }




                palette[j++] = lightAdjustedColour
            }
        }
        return palette.values.toTypedArray()
    }

    /*val colourTex: Texture = getTexture(colourPalette)

    val material = Material(TextureAttribute.createDiffuse(colourTex))


    fun getTexture(palette: Array<ImmutableColor>): Texture {
        val colourPixmap = Pixmap(256, 256, Pixmap.Format.RGB888)
        for(x in 0 until 256)
            for(y in 0 until 256) {
                colourPixmap.setColor(palette[x + (y * 256)])
                colourPixmap.drawRectangle(x, y, 1, 1)
            }

        val colourTex = Texture(colourPixmap)
        colourPixmap.dispose()
        return colourTex
    }*/



    fun adjustRGB(rgb: Int, exponent: Double): Int {
    //    var alpha = (rgba shr 24 and 255).toDouble() / 256.0
        var var3 = (rgb shr 16 and 255).toDouble() / 256.0
        var var5 = (rgb shr 8 and 255).toDouble() / 256.0
        var var7 = (rgb and 255).toDouble() / 256.0
        var3 = var3.pow(exponent)
        var5 = var5.pow(exponent)
        var7 = var7.pow(exponent)
        val var9 = (var3 * 256.0).toInt()
        val var10 = (var5 * 256.0).toInt()
        val var11 = (var7 * 256.0).toInt()
        return var11 + (var10 shl 8) + (var9 shl 16)// + ((alpha * 256.0).toInt() shl 24)
    }



}