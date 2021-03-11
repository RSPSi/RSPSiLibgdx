package com.rspsi.game.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration
import com.rspsi.game.GameView
import ktx.log.info

object DesktopLauncher {
    @JvmStatic
    fun main(arg: Array<String>) {
        val config = Lwjgl3ApplicationConfiguration()
        val gameView = GameView()
        config.setTitle("RSPSi v2.0.0")
        config.setWindowedMode(900, 800)
        config.useOpenGL3(true, 3, 2)
        //config.setBackBufferConfig(8, 8, 8, 8, 24, 0, 0)
        config.enableGLDebugOutput(true, System.out)
        Lwjgl3Application(gameView, config)

    }
}