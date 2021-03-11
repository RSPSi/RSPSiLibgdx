package com.rspsi.ext

import com.badlogic.gdx.graphics.g2d.Sprite
import com.kotcrab.vis.ui.widget.VisImage
import ktx.scene2d.KWidget
import ktx.scene2d.Scene2dDsl
import ktx.scene2d.actor

@Scene2dDsl
inline fun <S> KWidget<S>.visImage(
    sprite: Sprite? = null,
    init: VisImage.(S) -> Unit = {}
): VisImage {
    return actor(VisImage(sprite), init)
}