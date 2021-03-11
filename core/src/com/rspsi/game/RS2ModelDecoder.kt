package com.rspsi.game

import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer


open class RS2ModelDecoder {

    init {
        context.bindSingleton { this }
    }


    val textureProvider: RS2TextureProvider = context.inject()

    val fileProvider: CacheLibrary = context.inject()

    open fun canDecode(buffer: InputBuffer): Boolean {
        val length = buffer.remaining()
        buffer.offset = length - 2
        val revisionHash = buffer.readShort()

        return revisionHash == -1

    }

    open fun request(id: Int): RS2Model? {
        val data = fileProvider.data(7, id, 0)
        return data?.let {
            return decode(id, it)
        }
    }

    open fun decode(id: Int, data: ByteArray): RS2Model? {

        if(data[data.size - 1].toInt() == -1 && data[data.size - 2].toInt() == -1) {
            return null
        }
        val header = RS2ModelHeader(data)
        header.decode()

        var offset = 0

        header.vertexDirectionOffset = offset
        offset += header.vertexCount

        header.faceTypeOffset = offset
        offset += header.faceCount

        header.facePriorityOffset = offset

        if (header.facePriority == 255) {
            offset += header.faceCount
        } else {
            header.facePriorityOffset = (header.facePriority - 1)
        }

        header.faceBoneOffset = offset
        if (header.useFaceSkinning) {
            offset += header.faceCount
        } else {
            header.faceBoneOffset = (-1)
        }

        header.texturePointerOffset = offset
        if (header.useTextures) {
            offset += header.faceCount
        } else {
            header.texturePointerOffset = (-1)
        }

        header.boneOffset = offset
        if (header.useVertexSkinning) {
            offset += header.vertexCount
        } else {
            header.boneOffset = (-1)
        }

        header.faceAlphaOffset = offset
        if (header.useTransparency) {
            offset += header.faceCount
        } else {
            header.faceAlphaOffset = (-1)
        }

        header.faceDataOffset = offset
        offset += header.faceDataLength

        header.colourDataOffset = offset
        offset += header.faceCount * 2

        header.uvMapFaceOffset = offset
        offset += header.texturedFaceCount * 6

        header.xDataOffset = offset
        offset += header.xDataLength

        header.yDataOffset = offset
        offset += header.yDataLength

        header.zDataOffset = offset
        offset += header.zDataLength


        return RS2Model(id, header).decode()

    }


    companion object


}

