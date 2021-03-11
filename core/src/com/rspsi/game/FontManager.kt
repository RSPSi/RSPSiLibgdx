package com.rspsi.game

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import ktx.freetype.generateFont
import ktx.inject.Context
import ktx.log.info


class FontManager {

    init {
        context.bindSingleton { this }
    }

    val fontMap = mutableMapOf<String, BitmapFont>()


    fun loadFonts() {
        Gdx.files.internal("fonts/").list().forEach { fontFile ->


            val fontName = fontFile.nameWithoutExtension().replace("-", " ")

            val fontGenerator = FreeTypeFontGenerator(fontFile)

            for(fontSize in 12..32 step 4) {
                fontMap["${fontName}_$fontSize"] = fontGenerator.generateFont {
                    size = fontSize
                }
                info {
                    "Loaded $fontName"
                }
            }

            fontGenerator.dispose()

        }

    }
}