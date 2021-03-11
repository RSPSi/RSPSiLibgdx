package com.rspsi.game

import com.badlogic.gdx.math.Vector3
import com.rspsi.ext.ImmutableVector3

data class RS2TileData(
        val position: ImmutableVector3, var flag: Int = 0, var tileHeight: Float = 0f, var tileHeightGenerated: Boolean = true,
        var overlayId: Int = 0, var overlayShape: Byte = 0, var overlayOrientation: Byte = 0, var underlayId: Int = 0,
) {

        override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as RS2TileData

                if (position != other.position) return false
                if (flag != other.flag) return false
                if (tileHeight != other.tileHeight) return false
                if (tileHeightGenerated != other.tileHeightGenerated) return false
                if (overlayId != other.overlayId) return false
                if (overlayShape != other.overlayShape) return false
                if (overlayOrientation != other.overlayOrientation) return false
                if (underlayId != other.underlayId) return false

                return true
        }

        override fun hashCode(): Int {
                var result = position.hashCode()
                result = 31 * result + flag
                result = 31 * result + tileHeight.hashCode()
                result = 31 * result + tileHeightGenerated.hashCode()
                result = 31 * result + overlayId
                result = 31 * result + overlayShape
                result = 31 * result + overlayOrientation
                result = 31 * result + underlayId
                return result
        }
}