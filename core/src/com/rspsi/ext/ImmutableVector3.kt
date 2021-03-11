package com.rspsi.ext

import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import ktx.math.ImmutableVector
import ktx.math.ImmutableVector2
import ktx.math.toImmutable
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

open class ImmutableVector3(val x: Float, val y: Float, val z: Float) : ImmutableVector<ImmutableVector3> {


    constructor() : this(ZERO)
    constructor(fillWith: Float) : this(fillWith, fillWith, fillWith)
    constructor(vec2: ImmutableVector2, z: Float) : this(vec2.x, vec2.y, z)
    constructor(vec3: ImmutableVector3): this(vec3.x, vec3.y, vec3.z)
    constructor(vec3: Vector3): this(vec3.x, vec3.y, vec3.z)
    constructor(x: Number, y: Number, z: Number): this(x.toFloat(), y.toFloat(), z.toFloat())

    companion object {
        /** Vector zero */
        val ZERO = ImmutableVector3(0f, 0f, 0f)

        /** Unit vector of positive x axis */
        val X = ImmutableVector3(1f, 0f, 0f)

        /** Unit vector of positive y axis */
        val Y = ImmutableVector3(0f, 1f, 0f)

        /** Unit vector of positive y axis */
        val Z = ImmutableVector3(0f, 0f, 1f)

        /**
         * Returns the [ImmutableVector3] represented by the specified [string] according to the format of [ImmutableVector2::toString]
         */
        fun fromString(string: String): ImmutableVector3 =
            Vector3().fromString(string).toImmutable()
    }

    override val len2: Float = Vector3.len2(x, y, z)

    override val len: Float = Vector3.len(x, y, z)

    val normalized: ImmutableVector3
        get() {
            if (len == 0f || len == 1f) return this
            return this / len
        }

    override val nor: ImmutableVector3
        get() {
            if (len2 == 0f || len2 == 1f) return this
            return this * (1f / sqrt(len2.toDouble()))
        }


    override fun dec() = ImmutableVector3(x - 1, y - 1, z - 1)

    override fun dot(vector: ImmutableVector3) = Vector3.dot(x, y, z, vector.x, vector.y, vector.z)

    override fun dst2(vector: ImmutableVector3): Float = Vector3.dst2(x, y, z, vector.x, vector.y, vector.z)

    override fun epsilonEquals(other: ImmutableVector3, epsilon: Float) = toMutable().epsilonEquals(other.x, other.y, other.z, epsilon)

    override fun inc(): ImmutableVector3 = ImmutableVector3(x + 1, y + 1, z + 1)

    override fun isZero(margin: Float) = x == 0f && y == 0f && z == 0f || len2 < margin

    override fun unaryMinus(): ImmutableVector3 = ImmutableVector3(-x, -y, -z)

    override fun isOnLine(other: ImmutableVector3, epsilon: Float): Boolean = toMutable().isOnLine(other.toMutable(), epsilon)

    override fun withClamp2(min2: Float, max2: Float): ImmutableVector3 {
        val minSq: Float = min2 * min2
        val maxSq: Float = max2 * max2
        return when {
            len2 == 0f -> {
                this
            }
            len2 > maxSq -> {
                times(sqrt((maxSq / len2)))
            }
            len2 < minSq -> {
                times(sqrt((minSq / len2)))
            }
            else -> {
                this
            }
        }
    }

    override fun withLength2(length2: Float): ImmutableVector3 {
        val oldLen2 = len2

        return if (oldLen2 == 0f || oldLen2 == length2) this else times(sqrt(length2 / oldLen2))
    }

    override fun withLerp(target: ImmutableVector3, alpha: Float): ImmutableVector3 = toMutable().lerp(target.toMutable(), alpha).toImmutable()

    override fun withLimit2(limit2: Float): ImmutableVector3 = toMutable().limit2(limit2).toImmutable()

    override fun withRandomDirection(rng: Random): ImmutableVector3 {
        TODO("Not yet implemented")
    }

    override operator fun plus(other: ImmutableVector3) = this.plus(other.x, other.y, other.z)

    override operator fun times(other: ImmutableVector3) = this.times(other.x, other.y, other.z)

    override operator fun times(scalar: Float): ImmutableVector3 = this.times(scalar, scalar, scalar)

    operator fun times(scalar: Number): ImmutableVector3 = this.times(scalar.toFloat(), scalar.toFloat(), scalar.toFloat())

    override operator fun minus(other: ImmutableVector3) = this.minus(other.x, other.y, other.z)

    fun crs(otherX: Float, otherY: Float, otherZ: Float): Float = x * otherX - y * otherY - z * otherZ

    fun cross(vector: ImmutableVector3): ImmutableVector3 = ImmutableVector3(y * vector.z - z * vector.y, z * vector.x - x * vector.z, x * vector.y - y * vector.x)

    fun minus(deltaX: Float = 0f, deltaY: Float = 0f, deltaZ: Float = 0f): ImmutableVector3 = ImmutableVector3(x - deltaX, y - deltaY, z - deltaZ)
    fun times(deltaX: Float = 0f, deltaY: Float = 0f, deltaZ: Float = 0f): ImmutableVector3 = ImmutableVector3(x * deltaX, y * deltaY, z * deltaZ)
    fun plus(deltaX: Float = 0f, deltaY: Float = 0f, deltaZ: Float = 0f): ImmutableVector3 = ImmutableVector3(x + deltaX, y + deltaY, z + deltaZ)


    /** Returns the angle in radians of this vector relative to the [reference]. Angles are towards the positive y-axis. (typically counter-clockwise) */
    fun angleRad(reference: ImmutableVector3 = ImmutableVector3.X): Float = angleRad(reference.x, reference.y, reference.z)

    /** Returns the angle in radians of this vector relative to the ([referenceX], [referenceY]) reference. Angles are towards the positive y-axis. (typically counter-clockwise) */
    fun angleRad(referenceX: Float, referenceY: Float, referenceZ: Float): Float {
        if (this.isZero() || (referenceX == 0f && referenceY == 0f && referenceZ == 0f)) return Float.NaN

        val result = atan2(y, x) - atan2(referenceY, referenceX)
        return when {
            result > MathUtils.PI -> result - MathUtils.PI2
            result < -MathUtils.PI -> result + MathUtils.PI2
            else -> result
        }
    }

    operator fun div(vector: ImmutableVector3) = ImmutableVector3(x / vector.x, y / vector.y, z / vector.z)
    operator fun div(scalar: Float) = ImmutableVector3(x / scalar, y / scalar, z / scalar)
    fun flipYZ() = ImmutableVector3(x, z, y)


    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImmutableVector3

        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        result = 31 * result + z.hashCode()
        return result
    }

    override fun toString(): String {
        return "ImmutableVector3(x=$x, y=$y, z=$z)"
    }
}

inline fun ImmutableVector3.toMutable() = Vector3(x, y, z)
inline fun Vector3.toImmutable() = ImmutableVector3(x, y, z)


/** Calculates the 2D cross product between this and the [other] vector */
inline infix fun ImmutableVector3.x(other: ImmutableVector3): Float = crs(other.x, other.y, other.z)

/** Calculates the 2D cross product between this and the [other] vector */
inline infix fun ImmutableVector3.crs(other: ImmutableVector3): Float = crs(other.x, other.y, other.z)
inline infix fun ImmutableVector3.cross(other: ImmutableVector3): ImmutableVector3 = cross(other)


fun Quaternion.getAngleAround(axis: ImmutableVector3) = getAngleAround(axis.toMutable())
fun Quaternion.getAxisAngle(axis: ImmutableVector3) = getAxisAngle(axis.toMutable())
fun Quaternion.getAxisAngleRad(axis: ImmutableVector3) = getAxisAngleRad(axis.toMutable())
fun Quaternion.getAngleAroundRad(axis: ImmutableVector3) = getAngleAroundRad(axis.toMutable())
fun Quaternion.transform(v: ImmutableVector3) = transform(v.toMutable()).toImmutable()
fun Quaternion.setFromAxis(vector: ImmutableVector3, degrees: Float): Quaternion = setFromAxis(vector.toMutable(), degrees)
fun Quaternion.setFromAxisRad(vector: ImmutableVector3, radians: Float): Quaternion = setFromAxisRad(vector.toMutable(), radians)
fun Quaternion.setFromCross(v1: ImmutableVector3, v2: ImmutableVector3): Quaternion = setFromCross(v1.toMutable(), v2.toMutable())
fun Quaternion.getSwingTwist(axis: ImmutableVector3, swing: Quaternion, twist: Quaternion) = getSwingTwist(axis.toMutable(), swing, twist)
