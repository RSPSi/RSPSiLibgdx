package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute
import com.badlogic.gdx.graphics.g3d.utils.RenderableSorter
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Vector3

class MaterialSorter: RenderableSorter, Comparator<Renderable> {

    lateinit var camera: Camera
    val tmpV1 = Vector3()
    val tmpV2 = Vector3()

    override fun compare(o1: Renderable, o2: Renderable): Int {
        val b1 =
            o1.material.has(BlendingAttribute.Type) && (o1.material[BlendingAttribute.Type] as BlendingAttribute).blended
        val b2 =
            o2.material.has(BlendingAttribute.Type) && (o2.material[BlendingAttribute.Type] as BlendingAttribute).blended
        if (b1 != b2) return if (b1) 1 else -1
        // FIXME implement better sorting algorithm
        // final boolean same = o1.shader == o2.shader && o1.mesh == o2.mesh && (o1.lights == null) == (o2.lights == null) &&
        // o1.material.equals(o2.material);
        if(o1.material.id != o2.material.id) return o1.material.id.compareTo(o2.material.id)

        getTranslation(o1.worldTransform, o1.meshPart.center, tmpV1)
        getTranslation(o2.worldTransform, o2.meshPart.center, tmpV2)
        val dst =
            ((1000f * camera.position.dst2(tmpV1)).toInt() - (1000f * camera.position.dst2(tmpV2)).toInt()).toFloat()
        val result = if (dst < 0) -1 else if (dst > 0) 1 else 0
        return if (b1) -result else result
    }

    private fun getTranslation(worldTransform: Matrix4, center: Vector3, output: Vector3): Vector3? {
        if (center.isZero) worldTransform.getTranslation(output) else if (!worldTransform.hasRotationOrScaling()) worldTransform.getTranslation(
            output
        ).add(center) else output.set(center).mul(worldTransform)
        return output
    }

    override fun sort(camera: Camera, renderables: com.badlogic.gdx.utils.Array<Renderable>) {
        this.camera = camera
        renderables.sort(this)
    }

}