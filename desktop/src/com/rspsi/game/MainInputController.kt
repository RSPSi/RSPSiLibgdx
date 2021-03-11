package com.rspsi.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController
import com.badlogic.gdx.utils.IntIntMap
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.toMutable
import ktx.log.info

class MainInputController(camera: Camera) : FirstPersonCameraController(camera) {

    init {
        context.bindSingleton { this }
    }
    private val chatManager: ChatManager = context.inject()
    private val gameUI: GameUI = context.inject()
    val keys = IntIntMap()
    private var curVelocity = 5f
    private val gameScreen: GameScreen = context.inject()


    override fun keyDown(keycode: Int): Boolean {
        keys.put(keycode, keycode)
        return super.keyDown(keycode)
    }

    override fun keyUp(keycode: Int): Boolean {

        keys.remove(keycode, 0)
        return super.keyUp(keycode)
    }


    var mouseMoved = false
    var mouseX = 0
    var mouseY = 0

    var dragging = false
    override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
        mouseMoved = true
        mouseX = screenX
        mouseY = screenY
        return if(!gameUI.gameStage.mouseMoved(screenX, screenY))
            super.mouseMoved(screenX, screenY)
        else
            true
    }

    override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        return if(!gameUI.gameStage.touchDown(screenX, screenY, pointer, button)){

                gameScreen.fboUpdateRequested = true
                true
            } else
                super.touchDown(screenX, screenY, pointer, button)



    }

    override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
        dragging = false
        return if(!gameUI.gameStage.touchUp(screenX, screenY, pointer, button))
            super.touchDown(screenX, screenY, pointer, button)
        else
            true
    }

    override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
        mouseMoved = true
        mouseX = screenX
        mouseY = screenY
        dragging = true
        return if(!gameUI.gameStage.touchDragged(screenX, screenY, pointer))
            super.touchDragged(screenX, screenY, pointer)
        else
            true
    }

    override fun scrolled(amountX: Float, amountY: Float): Boolean {
        return if(!gameUI.gameStage.scrolled(amountX, amountY))
            super.scrolled(amountX, amountY)
        else
            true
    }

    override fun update() {
        update(Gdx.graphics.deltaTime)
    }

    override fun update(deltaTime: Float) {

        gameUI.act(deltaTime)
        if(chatManager.update(deltaTime))
            return

        super.update(deltaTime)


        keys.apply {

            containsKey(Input.Keys.X, true) {
                gameScreen.directionalLight.setDirection(gameScreen.camera.direction)
                gameScreen.dirty = true
            }
            containsKey(Input.Keys.NUMPAD_ADD){
                curVelocity += 0.5f
                setVelocity(curVelocity)
                info {
                    "$curVelocity"
                }
            }
            containsKey(Input.Keys.NUMPAD_SUBTRACT) {
                curVelocity -= 0.5f
                setVelocity(curVelocity)
                info {
                    "$curVelocity"
                }
            }
            containsKey(Input.Keys.R) {
                keys.remove(Input.Keys.R, 0)
               // gameScreen.loadTextures()
                gameScreen.loadRegion()


            }
            containsKey(Input.Keys.Y) {
                keys.remove(Input.Keys.Y, 0)

                val randomObject = gameScreen.compoundModels.random()
                gameScreen.camera.position.set(randomObject.worldPos.flipYZ().toMutable())

                gameScreen.camera.combined.set(randomObject.transform)
                info { "Teleported to $randomObject" }
            }
            containsKey(Input.Keys.U) {
                keys.remove(Input.Keys.U, 0)
                gameScreen.engine.removeAllEntities()
                gameScreen.terrainInstances.fill(null)
                gameScreen.overlayEntities.fill(null)
                gameScreen.underlayEntities.fill(null)
                info {
                    "Cleared entities"
                }
                gameScreen.dirty = true


            }
            containsKey(Input.Keys.C, true) {
                gameScreen.directionalLight.set(0.6f, 0.6f, 0.6f, -0.001f, -5f, -0.001f)

               // gameScreen.shadowLight2.set(0f, 0f, 1f, 1f, 5f, 1f)
            }
            containsKey(Input.Keys.V, true) {

                //gameScreen.dirty = true
            }
            containsKey(Input.Keys.K, true) {
                gameScreen.drawFBO = !gameScreen.drawFBO
                info {
                    "Drawing fbo: ${gameScreen.drawFBO}"
                }
               // gameScreen.dirty = true
            }
            containsKey(Input.Keys.M, true) {
                mode = mode.next()
                info {
                    "Mode changed to $mode"
                }
            }
            containsKey(Input.Keys.Z, true) {
                gameScreen.directionalLight.set(0.2f, 0.2f, 0.2f, 5f, -10f, 5f)
                gameScreen.dirty = true
            }
        }
    }



    var mode = RotateMode.PITCH

    enum class RotateMode {
        PITCH, YAW, ROLL;

        fun next(): RotateMode {
            val nextOrdinal = if (ordinal + 1 >= values().size) 0 else ordinal + 1

            return values()[nextOrdinal]
        }
    }
}


inline fun IntIntMap.containsKey(keycode: Int, function: () -> Unit) {
    if(containsKey(keycode)){

        function()
    }
}

inline fun IntIntMap.containsKey(keycode: Int, resetKey: Boolean = false, function: () -> Unit) {
    if(containsKey(keycode)){
        if(resetKey){
            remove(keycode, keycode)
        }
        function()
    }
}
