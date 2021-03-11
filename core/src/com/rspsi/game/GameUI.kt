package com.rspsi.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Stack
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.Scaling
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisImage
import com.kotcrab.vis.ui.widget.VisTextButton
import com.rspsi.ext.visImage
import ktx.actors.*
import ktx.log.info
import ktx.scene2d.*
import ktx.scene2d.vis.*
import javax.swing.GroupLayout


class GameUI(private val camera: Camera) {

    init {
        context.bindSingleton { this }
    }


    val fontManager: FontManager = context.inject()

    val viewport = ExtendViewport(Gdx.graphics.height.toFloat(), Gdx.graphics.width.toFloat())
    val gameStage = Stage(viewport)

    var minimapUI: Minimap? = null
    var testButton: VisTextButton? = null

    fun init() {

        gameStage.actors.clear()
        gameStage.actors {
            testButton = visTextButton("Test") {
                this.setPosition(50f, 50f)

                this.style.font = fontManager.fontMap["JetBrainsMono Light"]
                info {
                    "set 2 font to ${this.style.font}"
                }
                this.onClickEvent { event, x, y ->

                    info {
                        "Test button clicked $event $x $y"
                    }
                }
                this.onClick {
                }
            }


            minimapUI = actor(Minimap(camera))

        }

    }

    fun resize(width: Int, height: Int) {
        with(gameStage.viewport) {
            setWorldSize(width.toFloat(), height.toFloat())
            update(width, height, true)


        }
    }

    fun act(delta: Float) {
        gameStage.act(delta)
    }

    fun draw(delta: Float) {
        gameStage.draw()
    }


}