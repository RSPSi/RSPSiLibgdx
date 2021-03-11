package com.rspsi.game

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.rspsi.ext.ImmutableColor
import ktx.collections.GdxArray


class MyContactListener(val gameObjects: MutableList<RenderableGameObject>) : ContactListener() {

    override fun onContactAdded(
        userValue0: Int, partId0: Int, index0: Int, match0: Boolean, userValue1: Int, partId1: Int,
        index1: Int, match1: Boolean
    ): Boolean {
        if (match0)
            gameObjects[userValue0].renderables.forEach {
                (it.material.get(ColorAttribute.Diffuse) as? ColorAttribute)?.color?.set(ImmutableColor.WHITE.toMutable())
            }
        if (match1)
            gameObjects[userValue1].renderables.forEach {
                (it.material.get(ColorAttribute.Diffuse) as? ColorAttribute)?.color?.set(ImmutableColor.WHITE.toMutable())
            }
        return true
    }

}

class MyMotionState(var renderables: GdxArray<Renderable>) : btMotionState() {
    override fun getWorldTransform(worldTrans: Matrix4) {
        if(renderables.size > 0)
            worldTrans.set(renderables[0].worldTransform)
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        renderables.forEach { it.worldTransform.set(worldTrans) }
    }
}