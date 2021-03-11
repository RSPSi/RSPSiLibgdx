package com.rspsi.ext

import com.badlogic.gdx.math.*
import com.badlogic.gdx.math.collision.BoundingBox
import ktx.math.ImmutableVector2
import ktx.math.plus
import ktx.math.toImmutable
import java.awt.Dimension


infix fun ImmutableVector2.north(size: Int) = this + ImmutableVector2(0f, 1f * size)
infix fun ImmutableVector2.east(size: Int) = this + ImmutableVector2(1f * size, 0f )
infix fun ImmutableVector2.south(size: Int) = this + ImmutableVector2(0f, -1f * size)
infix fun ImmutableVector2.west(size: Int) = this + ImmutableVector2(-1f * size, 0f)

infix fun ImmutableVector2.northeast(size: Int) = this north size east size
infix fun ImmutableVector2.southeast(size: Int) = this south size east size
infix fun ImmutableVector2.southwest(size: Int) = this south size west size
infix fun ImmutableVector2.northwest(size: Int) = this north size west size

infix fun ImmutableVector2.withZ(z: Number) = ImmutableVector3(this, z.toFloat())


infix fun ImmutableVector3.north(size: Int) = this + ImmutableVector3(0f, 1f * size, 0f)
infix fun ImmutableVector3.east(size: Int) = this + ImmutableVector3(1f * size, 0f, 0f)
infix fun ImmutableVector3.south(size: Int) = this + ImmutableVector3(0f, -1f * size, 0f)
infix fun ImmutableVector3.west(size: Int) = this + ImmutableVector3(-1f * size, 0f, 0f)

infix fun ImmutableVector3.northeast(size: Int) = this north size east size
infix fun ImmutableVector3.southeast(size: Int) = this south size east size
infix fun ImmutableVector3.southwest(size: Int) = this south size west size
infix fun ImmutableVector3.northwest(size: Int) = this north size west size


val ImmutableVector3.xy: ImmutableVector2
    get() { return ImmutableVector2(x, y) }
val ImmutableVector3.yx: ImmutableVector2
    get() { return ImmutableVector2(y, x) }
val ImmutableVector3.xz: ImmutableVector2
    get() { return ImmutableVector2(x, z) }
val ImmutableVector3.zx: ImmutableVector2
    get() { return ImmutableVector2(z, x) }
val ImmutableVector3.yz: ImmutableVector2
    get() { return ImmutableVector2(y, z) }
val ImmutableVector3.zy: ImmutableVector2
    get() { return ImmutableVector2(z,y) }


infix fun ImmutableVector2.crs(other: ImmutableVector2): Float = crs(other.x, other.y)

operator fun ImmutableVector3.plus(mutableVector: Vector3) = this.plus(mutableVector.toImmutable())
operator fun ImmutableVector3.minus(mutableVector: Vector3) = this.minus(mutableVector.toImmutable())
operator fun ImmutableVector3.times(mutableVector: Vector3) = this.times(mutableVector.toImmutable())


operator fun ImmutableVector3.plus(mutableVector: ImmutableVector2) = this.plus(mutableVector withZ 0)
operator fun ImmutableVector3.minus(mutableVector: ImmutableVector2) = this.minus(mutableVector withZ 0)
operator fun ImmutableVector3.times(mutableVector: ImmutableVector2) = this.times(mutableVector withZ 0)

operator fun ImmutableVector3.plus(mutableVector: Vector2) = this.plus(mutableVector.toImmutable())
operator fun ImmutableVector3.minus(mutableVector: Vector2) = this.minus(mutableVector.toImmutable())
operator fun ImmutableVector3.times(mutableVector: Vector2) = this.times(mutableVector.toImmutable())

fun Matrix4.translate(vec3: ImmutableVector3): Matrix4 = translate(vec3.toMutable())

fun Matrix4.getTranslation(): ImmutableVector3 = getTranslation(Vector3()).toImmutable()

fun Matrix4.set(translation: ImmutableVector3?, rotation: Quaternion?, scale: ImmutableVector3? = ImmutableVector3(1f)): Matrix4 =
    this.set((translation ?: ImmutableVector3.ZERO).toMutable(), rotation, (scale ?: ImmutableVector3(1f)).toMutable())

fun Frustum.sphereInFrustum(center: ImmutableVector3, radius: Float): Boolean = this.sphereInFrustum(center.toMutable(), radius)

fun BoundingBox.getCenter(): ImmutableVector3 = this.getCenter(Vector3()).toImmutable()
fun BoundingBox.getDimensions(): ImmutableVector3 = this.getDimensions(Vector3()).toImmutable()
fun Frustum.boundsInFrustum( center: ImmutableVector3, dimensions: ImmutableVector3): Boolean = this.boundsInFrustum(center.toMutable(), dimensions.toMutable())

infix fun Number.withY(y: Number): ImmutableVector2 = ImmutableVector2(this.toFloat(), y.toFloat())
