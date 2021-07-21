package com.rspsi.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Tree
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.kotcrab.vis.ui.widget.VisLabel
import com.kotcrab.vis.ui.widget.VisTextButton
import ktx.actors.*
import ktx.log.info
import ktx.scene2d.*
import ktx.scene2d.vis.*


class GameUI(private val camera: Camera) {

    init {
        context.bindSingleton { this }
    }


    val fontManager: FontManager = context.inject()

    val viewport = ExtendViewport(Gdx.graphics.height.toFloat(), Gdx.graphics.width.toFloat())
    val gameStage = Stage(viewport)

    var minimapUI: Minimap? = null
    var testButton: VisTextButton? = null

    class ClusterTree : Tree.Node<RenderableLabel, GameObjectCluster, KVisTree> {
        constructor(cluster: GameObjectCluster) : super(KVisTree("default")) {
            actor.add(RenderableLabel(cluster.validTypes.toString()))
            cluster.instances.forEach { name, renderable ->
                actor.add(RenderableLabel(name))
            }
            value = cluster
        }
    }
    class RenderableLabel : Tree.Node<RenderableLabel, String, VisLabel> {
        constructor(text: String) : super(VisLabel(text)) {
            value = text
        }
    }
    lateinit var tree: KVisTree

    fun init() {

        gameStage.actors.clear()
        gameStage.actors {
            tree = visTree {
                setFillParent(true)
            }
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