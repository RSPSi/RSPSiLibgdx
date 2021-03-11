package com.rspsi.game

object Orientation {

    const val north = 0
    const val east = 1
    const val south = 2
    const val west = 3

    val northEast = (north + 1) shl 2 + east
    val southEast = (south + 1) shl 2 + east
    val southWest = (south + 1) shl 2 + west
    val northWest = (north + 1) shl 2 + west
}