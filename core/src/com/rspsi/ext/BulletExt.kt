package com.rspsi.ext

import com.badlogic.gdx.physics.bullet.collision.ClosestRayResultCallback
import com.badlogic.gdx.physics.bullet.collision.RayResultCallback
import com.badlogic.gdx.physics.bullet.collision.btCollisionWorld
import com.badlogic.gdx.physics.bullet.linearmath.btIDebugDraw

fun btCollisionWorld.rayTest(rayFromWorld: ImmutableVector3, rayToWorld: ImmutableVector3, resultCallback: RayResultCallback) =
    rayTest(rayFromWorld.toMutable(), rayFromWorld.toMutable(), resultCallback)

fun ClosestRayResultCallback.setRayFromWorld(value: ImmutableVector3) = setRayFromWorld(value.toMutable())
fun ClosestRayResultCallback.setRayToWorld(value: ImmutableVector3) = setRayFromWorld(value.toMutable())

fun btIDebugDraw.drawLine(from: ImmutableVector3, to: ImmutableVector3, color: ImmutableColor)  = drawLine(from.toMutable(), to.toMutable(), color.vec3().toMutable())
fun btIDebugDraw.drawAabb(from: ImmutableVector3, to: ImmutableVector3, color: ImmutableColor)  = drawLine(from.toMutable(), to.toMutable(), color.vec3().toMutable())