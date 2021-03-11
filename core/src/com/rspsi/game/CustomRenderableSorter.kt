package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.utils.DefaultRenderableSorter
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter
import com.badlogic.gdx.utils.Array
import ktx.collections.toGdxArray
import java.util.*

class CustomRenderableSorter: RenderableSorter, Comparator<Renderable> {
    val defaultSorter = DefaultRenderableSorter()


    private var camera: Camera? = null
    override fun sort(camera: Camera?, renderables: Array<Renderable>) {
        this.camera = camera
        renderables.sort(this)
        defaultSorter.sort(camera, renderables)
    }

    override fun compare(o1: Renderable, o2: Renderable): Int {

        val va0: VertexAttributes = o1.meshPart.mesh.vertexAttributes
        val va1: VertexAttributes = o2.meshPart.mesh.vertexAttributes
        val vc = va0.compareTo(va1)
        if (vc == 0) {
            val mc: Int = o1.material.compareTo(o2.material)
            return if (mc == 0) {
                o1.meshPart.primitiveType - o2.meshPart.primitiveType
            } else mc
        }
        return vc
    }
}
