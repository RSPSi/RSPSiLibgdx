package com.rspsi.game

import com.rspsi.ext.ImmutableVector3

class RS2Tile(position: ImmutableVector3) {

    val data = RS2TileData(position)
    val model = RS2TileModel()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RS2Tile

        if (data != other.data) return false
        if (model != other.model) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.hashCode()
        result = 31 * result + model.hashCode()
        return result
    }
}