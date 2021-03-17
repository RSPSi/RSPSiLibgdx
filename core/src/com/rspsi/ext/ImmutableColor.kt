package com.rspsi.ext

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.utils.NumberUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ImmutableColor(r: Float, g: Float, b: Float, a: Float = 1f) {

    constructor() : this(0f, 0f, 0f, 0f)
    constructor(rgba8888: Int): this(rgba8888ToColor(rgba8888))

    constructor(immutableColor: ImmutableColor): this(immutableColor.r, immutableColor.g, immutableColor.b, immutableColor.a)


    /** NOT STANDARD BEHAVIOUR **/
   val r = r.coerceIn(0f, 1f)
   val g = g.coerceIn(0f, 1f)
   val b = b.coerceIn(0f, 1f)
   val a = a.coerceIn(0f, 1f)



    /** Extract Hue-Saturation-Value. This is the inverse of [.fromHsv].
     * @param hsv The HSV array to be modified.
     * @return HSV components for chaining.
     */
    fun toHsv(hsv: FloatArray): FloatArray {
        val max = max(max(r, g), b)
        val min = min(min(r, g), b)
        val range = max - min
        if (range == 0f) {
            hsv[0] = 0f
        } else if (max == r) {
            hsv[0] = (60 * (g - b) / range + 360) % 360
        } else if (max == g) {
            hsv[0] = 60 * (b - r) / range + 120
        } else {
            hsv[0] = 60 * (r - g) / range + 240
        }
        if (max > 0) {
            hsv[1] = 1 - min / max
        } else {
            hsv[1] = 0f
        }
        hsv[2] = max
        return hsv
    }



    operator fun plus(color: ImmutableColor): ImmutableColor {
        return ImmutableColor(r + color.r, g + color.g, b + color.b, a + color.a)

    }

    operator fun minus(color: ImmutableColor): ImmutableColor {
        return ImmutableColor(r - color.r, g - color.g, b - color.b, a - color.a)
    }

    operator fun times(color: ImmutableColor): ImmutableColor {
        return ImmutableColor(r * color.r, g * color.g, b * color.b, a * color.a)
    }

    operator fun div(color: ImmutableColor): ImmutableColor {
        return ImmutableColor(r / color.r, g / color.g, b / color.b, a / color.a)
    }

    operator fun times(scalar: Float): ImmutableColor {
        return ImmutableColor(r * scalar, g * scalar, b * scalar, a * scalar)
    }

    /** Linearly interpolates between this color and the target color by t which is in the range [0,1]. The result is stored in
     * this color.
     * @param target The target color
     * @param t The interpolation coefficient
     * @return This color for chaining.
     * */
    fun lerp(target: ImmutableColor, t: Float): ImmutableColor {
        return ImmutableColor(
            r + (t * (target.r - r)).toInt(),
            g + (t * (target.g - g)).toInt(),
            b + (t * (target.b - b)).toInt(),
            a + (t * (target.a - a)).toInt()
        )
    }

    fun premultiplyAlpha(): ImmutableColor {
        return ImmutableColor(r * a, g * a, b * a, a)
    }
    fun toMutable(): Color {
        return Color(r, g, b, a)
    }


    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val color = o as ImmutableColor
        return toIntBits() == color.toIntBits()
    }

    override fun hashCode(): Int {
        var result = if (r != 0.0f) NumberUtils.floatToIntBits(r) else 0
        result = 31 * result + if (g != 0.0f) NumberUtils.floatToIntBits(g) else 0
        result = 31 * result + if (b != 0.0f) NumberUtils.floatToIntBits(b) else 0
        result = 31 * result + if (a != 0.0f) NumberUtils.floatToIntBits(a) else 0
        return result
    }


    /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Alpha is compressed
     * from 0-255 to use only even numbers between 0-254 to avoid using float bits in the NaN range (see
     * [NumberUtils.intToFloatColor]). Converting a color to a float and back can be lossy for alpha.
     * @return the packed color as a 32-bit float
     */
    fun toFloatBits(): Float {
        val color =
            (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
        return NumberUtils.intToFloatColor(color)
    }

    /** Packs the color components into a 32-bit integer with the format ABGR.
     * @return the packed color as a 32-bit int.
     */
    fun toIntBits(): Int {
        return (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
    }

    fun toARGBIntBits(): Int {
        return (255 * a).toInt() shl 24 or ((255 * r).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * b).toInt()
    }
    fun toRGBIntBits(): Int {
        return ((255 * r).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * b).toInt()
    }

    /** Returns the color encoded as hex string with the format RRGGBBAA.  */
    override fun toString(): String {
        var value = Integer
            .toHexString((255 * r).toInt() shl 24 or ((255 * g).toInt() shl 16) or ((255 * b).toInt() shl 8) or (255 * a).toInt())
        while (value.length < 8) value = "0$value"
        return value
    }

    /**
     * Raises the R, G, B values to the exponent
     * @param exponent the exponent
     * @return a new ImmutableColour with the new R, G, B values and the original alpha
     */
    fun pow(exponent: Double): ImmutableColor {
        return ImmutableColor( r.toDouble().pow(exponent).toFloat(), g.toDouble().pow(exponent).toFloat(), b.toDouble().pow(exponent).toFloat(), a)
    }

    companion object {

        /** Sets the specified color from a hex string with the format RRGGBBAA.
         * @see .toString
         */
        fun valueOf(hex: String): ImmutableColor {
            var hex = hex
            hex = if (hex[0] == '#') hex.substring(1) else hex
            val r = hex.substring(0, 2).toInt(16) / 255f
            val g = hex.substring(2, 4).toInt(16) / 255f
            val b = hex.substring(4, 6).toInt(16) / 255f
            val a = if (hex.length != 8) 1f else hex.substring(6, 8).toInt(16) / 255f
            return ImmutableColor(r, g, b, a)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float. Note that no range
         * checking is performed for higher performance.
         * @param r the red component, 0 - 255
         * @param g the green component, 0 - 255
         * @param b the blue component, 0 - 255
         * @param a the alpha component, 0 - 255
         * @return the packed color as a float
         * @see NumberUtils.intToFloatColor
         */
        fun toFloatBits(r: Int, g: Int, b: Int, a: Int): Float {
            val color = a shl 24 or (b shl 16) or (g shl 8) or r
            return NumberUtils.intToFloatColor(color)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR and then converts it to a float.
         * @return the packed color as a 32-bit float
         * @see NumberUtils.intToFloatColor
         */
        fun toFloatBits(r: Float, g: Float, b: Float, a: Float): Float {
            val color =
                (255 * a).toInt() shl 24 or ((255 * b).toInt() shl 16) or ((255 * g).toInt() shl 8) or (255 * r).toInt()
            return NumberUtils.intToFloatColor(color)
        }

        /** Packs the color components into a 32-bit integer with the format ABGR. Note that no range checking is performed for higher
         * performance.
         * @param r the red component, 0 - 255
         * @param g the green component, 0 - 255
         * @param b the blue component, 0 - 255
         * @param a the alpha component, 0 - 255
         * @return the packed color as a 32-bit int
         */
        fun toIntBits(r: Int, g: Int, b: Int, a: Int): Int {
            return a shl 24 or (b shl 16) or (g shl 8) or r
        }

        fun alpha(alpha: Float): Int {
            return (alpha * 255.0f).toInt()
        }

        fun luminanceAlpha(luminance: Float, alpha: Float): Int {
            return (luminance * 255.0f).toInt() shl 8 or (alpha * 255).toInt()
        }

        fun rgb565(r: Float, g: Float, b: Float): Int {
            return (r * 31).toInt() shl 11 or ((g * 63).toInt() shl 5) or (b * 31).toInt()
        }

        fun rgba4444(r: Float, g: Float, b: Float, a: Float): Int {
            return (r * 15).toInt() shl 12 or ((g * 15).toInt() shl 8) or ((b * 15).toInt() shl 4) or (a * 15).toInt()
        }

        fun rgb888(r: Float, g: Float, b: Float): Int {
            return (r * 255).toInt() shl 16 or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        fun rgba8888(r: Float, g: Float, b: Float, a: Float): Int {
            return (r * 255).toInt() shl 24 or ((g * 255).toInt() shl 16) or ((b * 255).toInt() shl 8) or (a * 255).toInt()
        }

        fun argb8888(a: Float, r: Float, g: Float, b: Float): Int {
            return (a * 255).toInt() shl 24 or ((r * 255).toInt() shl 16) or ((g * 255).toInt() shl 8) or (b * 255).toInt()
        }

        fun rgb565(color: ImmutableColor): Int {
            return (color.r * 31).toInt() shl 11 or ((color.g * 63).toInt() shl 5) or (color.b * 31).toInt()
        }

        fun rgba4444(color: ImmutableColor): Int {
            return (color.r * 15).toInt() shl 12 or ((color.g * 15).toInt() shl 8) or ((color.b * 15).toInt() shl 4) or (color.a * 15).toInt()
        }

        fun rgb888(color: ImmutableColor): Int {
            return (color.r * 255).toInt() shl 16 or ((color.g * 255).toInt() shl 8) or (color.b * 255).toInt()
        }

        fun rgba8888(color: ImmutableColor): Int {
            return (color.r * 255).toInt() shl 24 or ((color.g * 255).toInt() shl 16) or ((color.b * 255).toInt() shl 8) or (color.a * 255).toInt()
        }

        fun argb8888(color: ImmutableColor): Int {
            return (color.a * 255).toInt() shl 24 or ((color.r * 255).toInt() shl 16) or ((color.g * 255).toInt() shl 8) or (color.b * 255).toInt()
        }

        /** Sets the ImmutableColor components using the specified integer value in the format RGB565. This is inverse to the rgb565(r, g, b)
         * method.
         *
         * @param value An integer color value in RGB565 format.
         */
        fun rgb565ToColor(value: Int): ImmutableColor {
            val r = (value and 0x0000F800 ushr 11) / 31f
            val g = (value and 0x000007E0 ushr 5) / 63f
            val b = (value and 0x0000001F ushr 0) / 31f
            return ImmutableColor(r, g, b, 1f)
        }

        /** Sets the ImmutableColor components using the specified integer value in the format RGBA4444. This is inverse to the rgba4444(r, g,
         * b, a) method.
         *
         * @param value An integer color value in RGBA4444 format.
         */
        fun rgba4444ToColor(value: Int): ImmutableColor {
            val r = (value and 0x0000f000 ushr 12) / 15f
            val g = (value and 0x00000f00 ushr 8) / 15f
            val b = (value and 0x000000f0 ushr 4) / 15f
            val a = (value and 0x0000000f) / 15f
            return ImmutableColor(r, g, b, a)
        }

        /** Sets the ImmutableColor components using the specified integer value in the format RGB888. This is inverse to the rgb888(r, g, b)
         * method.
         *
         * @param value An integer color value in RGB888 format.
         */
        fun rgb888ToColor(value: Int): ImmutableColor {
            val r = (value and 0x00ff0000 ushr 16) / 255f
            val g = (value and 0x0000ff00 ushr 8) / 255f
            val b = (value and 0x000000ff) / 255f

            return ImmutableColor(r, g, b, 1f)
        }

        /** Sets the ImmutableColor components using the specified integer value in the format RGBA8888. This is inverse to the rgba8888(r, g,
         * b, a) method.
         *
         * @param value An integer color value in RGBA8888 format.
         */
        fun rgba8888ToColor(value: Int): ImmutableColor {
            val r = (value and -0x1000000 ushr 24) / 255f
            val g = (value and 0x00ff0000 ushr 16) / 255f
            val b = (value and 0x0000ff00 ushr 8) / 255f
            val a = (value and 0x000000ff) / 255f

            return ImmutableColor(r, g, b, a)
        }

        fun rgba8888ToColor(value: ByteArray): ImmutableColor {
            val r = value[0] / 255f
            val g = value[1] / 255f
            val b = value[2] / 255f
            val a = value[3] / 255f

            return ImmutableColor(r, g, b, a)
        }


        /** Sets the ImmutableColor components using the specified float value in the format ABGR8888.
         * @param value the ABGR8888 color value
         */
        fun argb8888ToColor(value: Int): ImmutableColor {
            val a = (value and -0x1000000 ushr 24) / 255f
            val r = (value and 0x00ff0000 ushr 16) / 255f
            val g = (value and 0x0000ff00 ushr 8) / 255f
            val b = (value and 0x000000ff) / 255f

            return ImmutableColor(r, g, b, a)
        }

        fun rgb888ToColor(value: ByteArray): ImmutableColor {
            val r = value[0] / 255f
            val g = value[1] / 255f
            val b = value[2] / 255f

            return ImmutableColor(r, g, b, 1f)
        }

        /** Gets an ImmutableColor instance for the given abgr8888 value
         * @param value the abgr8888 color value
         */
        fun abgr8888ToColor(value: Float): ImmutableColor {
            val c = NumberUtils.floatToIntColor(value)
            val a = (c and -0x1000000 ushr 24) / 255f
            val b = (c and 0x00ff0000 ushr 16) / 255f
            val g = (c and 0x0000ff00 ushr 8) / 255f
            val r = (c and 0x000000ff) / 255f

            return ImmutableColor(r, g, b, a)
        }

        /** Sets the RGB Color components using the specified Hue-Saturation-Value. Note that HSV components are voluntary not clamped
         * to preserve high range color and can range beyond typical values.
         * @param h The Hue in degree from 0 to 360
         * @param s The Saturation from 0 to 1
         * @param v The Value (brightness) from 0 to 1
         * @return The modified Color for chaining.
         */
        fun fromHsv(h: Float, s: Float, v: Float): ImmutableColor {
            val x = (h / 60f + 6) % 6
            val i = x.toInt()
            val f = x - i
            val p = v * (1 - s)
            val q = v * (1 - s * f)
            val t = v * (1 - s * (1 - f))
            return when (i) {
                0 -> ImmutableColor(v, t, p)
                1 -> ImmutableColor(q, v, p)
                2 -> ImmutableColor(p, v, t)
                3 -> ImmutableColor(p, q, v)
                4 -> ImmutableColor(t, p, v)
                else -> ImmutableColor(v, p, q)
            }
        }

        /** Sets RGB components using the specified Hue-Saturation-Value. This is a convenient method for
         * [.fromHsv]. This is the inverse of [.toHsv].
         * @param hsv The Hue, Saturation and Value components in that order.
         * @return The modified Color for chaining.
         */
        fun fromHsv(hsv: FloatArray): ImmutableColor {
            return fromHsv(hsv[0], hsv[1], hsv[2])
        }

        val TRANSPARENT = ImmutableColor(0f, 0f, 0f, 0f)
        val WHITE = ImmutableColor(1f, 1f, 1f, 1f)
        val LIGHT_GRAY = ImmutableColor(-0x40404001)
        val GRAY = ImmutableColor(0x7f7f7fff)
        val DARK_GRAY = ImmutableColor(0x3f3f3fff)
        val BLACK = ImmutableColor(0f, 0f, 0f, 1f)

        /** Convenience for frequently used `WHITE.toFloatBits()`  */
        val WHITE_FLOAT_BITS = WHITE.toFloatBits()

        val CLEAR = ImmutableColor(0f, 0f, 0f, 0f)

        val BLUE = ImmutableColor(0f, 0f, 1f, 1f)
        val NAVY = ImmutableColor(0f, 0f, 0.5f, 1f)
        val ROYAL = ImmutableColor(0x4169e1ff)
        val SLATE = ImmutableColor(0x708090ff)
        val SKY = ImmutableColor(-0x78311401)
        val CYAN = ImmutableColor(0f, 1f, 1f, 1f)
        val TEAL = ImmutableColor(0f, 0.5f, 0.5f, 1f)

        val GREEN = ImmutableColor(0x00ff00ff)
        val CHARTREUSE = ImmutableColor(0x7fff00ff)
        val LIME = ImmutableColor(0x32cd32ff)
        val FOREST = ImmutableColor(0x228b22ff)
        val OLIVE = ImmutableColor(0x6b8e23ff)

        val YELLOW = ImmutableColor(-0xff01)
        val GOLD = ImmutableColor(-0x28ff01)
        val GOLDENROD = ImmutableColor(-0x255adf01)
        val ORANGE = ImmutableColor(-0x5aff01)

        val BROWN = ImmutableColor(-0x74baec01)
        val TAN = ImmutableColor(-0x2d4b7301)
        val FIREBRICK = ImmutableColor(-0x4ddddd01)

        val RED = ImmutableColor(-0xffff01)
        val SCARLET = ImmutableColor(-0xcbe301)
        val CORAL = ImmutableColor(-0x80af01)
        val SALMON = ImmutableColor(-0x57f8d01)
        val PINK = ImmutableColor(-0x964b01)
        val MAGENTA = ImmutableColor(1f, 0f, 1f, 1f)

        val PURPLE = ImmutableColor(-0x5fdf0f01)
        val VIOLET = ImmutableColor(-0x117d1101)
        val MAROON = ImmutableColor(-0x4fcf9f01)

    }
}

fun Color.toImmutable(): ImmutableColor = ImmutableColor(r, g, b, a)

infix fun ImmutableColor.withAlpha(alpha: Float) = ImmutableColor(r, g, b, alpha)

fun ImmutableColor.vec3(): ImmutableVector3 = ImmutableVector3(r, g, b)