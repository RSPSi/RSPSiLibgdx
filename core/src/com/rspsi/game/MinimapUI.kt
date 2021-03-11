package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Batch
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.rspsi.ext.setColor
import com.rspsi.ext.visImage
import ktx.scene2d.actors
import ktx.scene2d.horizontalGroup
import ktx.scene2d.stack

class Minimap(private val camera: Camera): VisImage() {


    lateinit var underlayMinimap: VisImage
    lateinit var overlayMinimap: VisImage
    lateinit var mapStack: Stack
    var visibleZ = 0

    val mapDecoder: RS2LandscapeDecoder = context.inject()


    override fun act(delta: Float) {
        super.act(delta)
        this.rotation = camera.view.getRotation(Quaternion()).getAngleAround(Vector3.Y)

    }

    override fun draw(batch: Batch?, parentAlpha: Float) {
        super.draw(batch, parentAlpha)
        batch?.let {
            mapDecoder.underlayTextures[visibleZ]?.let { batch.draw(it, 0f, 0f) }
            mapDecoder.overlayTextures[visibleZ]?.let{ batch.draw(it, 0f, 0f) }

        }
    }
}
