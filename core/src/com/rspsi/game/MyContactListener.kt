package com.rspsi.game

import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.physics.bullet.collision.ContactListener
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState
import com.rspsi.ext.*
import ktx.collections.GdxArray
import ktx.log.info


class MyContactListener(val gameObjects: MutableList<RenderableGameObject>, val terrainObjects: MutableList<RenderableGameObject>) : ContactListener() {

    override fun onContactAdded(
        userValue0: Int, partId0: Int, index0: Int, match0: Boolean, userValue1: Int, partId1: Int,
        index1: Int, match1: Boolean
    ): Boolean {

        if (match0) {
            val contacted = if(userValue0 >= Short.MAX_VALUE) terrainObjects[userValue0 - Short.MAX_VALUE] else gameObjects[userValue0]

        }
        if (match1) {
            val contactedWith = if(userValue1 >= Short.MAX_VALUE) terrainObjects[userValue1 - Short.MAX_VALUE] else gameObjects[userValue1]

        }
        return true
    }

}

class MyMotionState(var gameObject: RenderableGameObject) : btMotionState() {
    override fun getWorldTransform(worldTrans: Matrix4) {
        if(gameObject.boundingBox.getHalfExtents() == ImmutableVector3.ZERO) {
            info { "${gameObject.name} has no size?"}
        }
            worldTrans.set(gameObject.transform.cpy().translate(gameObject.boundingBox.getHalfExtents()))
    }

    override fun setWorldTransform(worldTrans: Matrix4) {
        gameObject.transform.set(worldTrans)
        gameObject.transform.translate(-gameObject.boundingBox.getHalfExtents())
    }
}