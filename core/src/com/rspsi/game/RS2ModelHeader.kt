package com.rspsi.game

import com.displee.io.impl.InputBuffer
import com.rspsi.ext.create
import com.rspsi.ext.readUBoolean

open class RS2ModelHeaderHD(data: ByteArray): RS2ModelHeader(data)

open class RS2ModelHeader(val data: ByteArray) {

    var vertexCount = 0//var10
    var faceCount = 0//var11
    var texturedFaceCount = 0//var12

    var useTextures = false//var13
    var facePriority = 0//var14
    var useTransparency = false//var30
    var useFaceSkinning = false //var15
    var useVertexSkinning = false //var28
    var faceDataLength = 0 // var23


    var xDataLength = 0//var27
    var yDataLength = 0//var20
    var zDataLength = 0//var36

    var xDataOffset = 0//var34
    var yDataOffset = 0//var35
    var zDataOffset = 0//var10000
    var colourDataOffset = 0//var17

    var texturePointerOffset = 0//var42
    var faceAlphaOffset = 0//var29
    var faceDataOffset = 0//var44
    var facePriorityOffset = 0//var25
    var faceBoneOffset = 0//var4
    var faceTypeOffset = 0//var24
    var uvMapFaceOffset = 0//var32
    var vertexDirectionOffset = 0//var16
    var boneOffset = 0//var37

    open fun decode() {
        val buffer = InputBuffer.create(data, data.size - 18)
        vertexCount = buffer.readUnsignedShort()
        faceCount = buffer.readUnsignedShort()
        texturedFaceCount = buffer.readUnsignedByte()

        useTextures = buffer.readUBoolean()
        facePriority = buffer.readUnsignedByte()
        useTransparency = buffer.readUBoolean()
        useFaceSkinning = buffer.readUBoolean()
        useVertexSkinning = buffer.readUBoolean()

        xDataLength = buffer.readUnsignedShort()
        yDataLength = buffer.readUnsignedShort()
        zDataLength = buffer.readUnsignedShort()

        faceDataLength = buffer.readUnsignedShort()




    }



    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RS2ModelHeader

        if (!data.contentEquals(other.data)) return false
        if (vertexCount != other.vertexCount) return false
        if (faceCount != other.faceCount) return false
        if (texturedFaceCount != other.texturedFaceCount) return false
        if (useTextures != other.useTextures) return false
        if (facePriority != other.facePriority) return false
        if (useTransparency != other.useTransparency) return false
        if (useFaceSkinning != other.useFaceSkinning) return false
        if (useVertexSkinning != other.useVertexSkinning) return false
        if (xDataOffset != other.xDataOffset) return false
        if (yDataOffset != other.yDataOffset) return false
        if (zDataOffset != other.zDataOffset) return false
        if (faceDataLength != other.faceDataLength) return false
        if (colourDataOffset != other.colourDataOffset) return false
        if (texturePointerOffset != other.texturePointerOffset) return false
        if (faceAlphaOffset != other.faceAlphaOffset) return false
        if (faceDataOffset != other.faceDataOffset) return false
        if (facePriorityOffset != other.facePriorityOffset) return false
        if (faceBoneOffset != other.faceBoneOffset) return false
        if (faceTypeOffset != other.faceTypeOffset) return false
        if (uvMapFaceOffset != other.uvMapFaceOffset) return false
        if (vertexDirectionOffset != other.vertexDirectionOffset) return false
        if (boneOffset != other.boneOffset) return false

        return true
    }

    override fun hashCode(): Int {
        var result = data.contentHashCode()
        result = 31 * result + vertexCount
        result = 31 * result + faceCount
        result = 31 * result + texturedFaceCount
        result = 31 * result + useTextures.hashCode()
        result = 31 * result + facePriority
        result = 31 * result + useTransparency.hashCode()
        result = 31 * result + useFaceSkinning.hashCode()
        result = 31 * result + useVertexSkinning.hashCode()
        result = 31 * result + xDataOffset
        result = 31 * result + yDataOffset
        result = 31 * result + zDataOffset
        result = 31 * result + faceDataLength
        result = 31 * result + colourDataOffset
        result = 31 * result + texturePointerOffset
        result = 31 * result + faceAlphaOffset
        result = 31 * result + faceDataOffset
        result = 31 * result + facePriorityOffset
        result = 31 * result + faceBoneOffset
        result = 31 * result + faceTypeOffset
        result = 31 * result + uvMapFaceOffset
        result = 31 * result + vertexDirectionOffset
        result = 31 * result + boneOffset
        return result
    }


}