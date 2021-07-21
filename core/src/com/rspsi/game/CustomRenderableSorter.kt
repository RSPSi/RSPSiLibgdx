package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter
import com.badlogic.gdx.utils.Array
import com.rspsi.ext.compareTo
import com.rspsi.ext.toMutable
import ktx.math.compareTo
import java.util.*

object CustomRenderableSorter {
    val defaultSorter = DefaultRenderableSorter()


    lateinit var camera: Camera

    fun sort(camera: Camera, gameObjects: List<RenderableGameObject>) {
        this.camera = camera
        gameObjects.forEach {
            it.renderables.sort { o1, o2 ->
                val va0: VertexAttributes = o1.meshPart.mesh.vertexAttributes
                val va1: VertexAttributes = o2.meshPart.mesh.vertexAttributes
                val vc = va0.compareTo(va1)
                if (vc == 0) {
                    val mc: Int = o1.material.compareTo(o2.material)
                    return@sort if (mc == 0) {
                        o1.meshPart.primitiveType - o2.meshPart.primitiveType
                    } else mc
                }
                return@sort vc
            }
            defaultSorter.sort(camera, it.renderables)
        }
    }

}
