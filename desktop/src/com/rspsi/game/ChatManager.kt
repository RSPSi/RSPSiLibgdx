package com.rspsi.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input.Keys.ENTER
import com.badlogic.gdx.scenes.scene2d.Stage
import com.kotcrab.vis.ui.widget.VisTextField
import ktx.actors.onKeyDown
import ktx.actors.onKeyUp
import ktx.log.info
import ktx.scene2d.actors
import ktx.scene2d.vis.visTextField

class ChatManager {

    init {
        context.bindSingleton { this }
    }



    lateinit var gameUI: GameUI
    lateinit var fontManager: FontManager
    lateinit var stage: Stage
    lateinit var keyboardInputAdapter: MainInputController
    lateinit var chatEntryBox: VisTextField

    fun init() {
        keyboardInputAdapter = context.inject()
        gameUI = context.inject()
        stage = gameUI.gameStage
        fontManager = context.inject()

        stage.actors {
            chatEntryBox = visTextField {
                x = 10f
                y = 10f
                width = 300f
                height = 40f
                isVisible = false
                this.style.font = fontManager.fontMap["JetBrainsMono Light_32"]
                info {
                    "set font to ${this.style.font}"
                }
            }
        }

        chatEntryBox.onKeyUp { key ->
            keyboardInputAdapter.keyUp(key)
        }
        chatEntryBox.onKeyDown { key ->
            keyboardInputAdapter.keyDown(key)
        }
    }
    fun update(deltaTime: Float): Boolean {
        with(keyboardInputAdapter.keys) {
            containsKey(ENTER, true) {
                info {
                    "Contains key"
                }
                chatEntryBox.isVisible = !chatEntryBox.isVisible


                if(chatEntryBox.isVisible) {

                    stage.keyboardFocus = chatEntryBox
                    Gdx.input.inputProcessor = stage
                } else {
                    stage.keyboardFocus =  null
                    Gdx.input.inputProcessor = keyboardInputAdapter
                }

                return true
            }


        }

        return chatEntryBox.isVisible
    }
}