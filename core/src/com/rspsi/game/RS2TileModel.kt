package com.rspsi.game

import com.badlogic.gdx.math.Vector3
import com.rspsi.ext.ImmutableVector3

data class RS2TileModel(var hslRaw: Int = 0,
                        var hslLightClamped: Int = 0,
                        var rgbLit: Int = 0,
                        var rgbRaw: Int = 0,
                        var hslLit: Int = 0,
                        var heightLighting: Int = 0,
                        var localHeights: MutableMap<ImmutableVector3, Float> = mutableMapOf(),
                        var localColours: MutableMap<ImmutableVector3, Int> = mutableMapOf(),
                        var localOverlayColours: MutableMap<ImmutableVector3, Int> = mutableMapOf()
)