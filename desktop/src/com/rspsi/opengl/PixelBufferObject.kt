package com.rspsi.opengl

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL30
import com.badlogic.gdx.graphics.GL30.*
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.badlogic.gdx.utils.Disposable
import com.rspsi.ext.ImmutableColor
import ktx.graphics.use
import ktx.log.info
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.ARBVertexArrayBGRA.GL_BGRA
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.glGetTexImage
import org.lwjgl.opengl.GL15
import org.lwjgl.opengl.GL15.GL_READ_ONLY
import org.lwjgl.opengl.GL15.glMapBuffer
import java.nio.ByteBuffer
import java.nio.IntBuffer

class PixelBufferObject(var width: Int, var height: Int, val pixmapFormat: Pixmap.Format): Disposable {

    var buffer: ByteBuffer =  BufferUtils.createByteBuffer(width * height * 4)
    var pixels = IntArray(width * height)
    var pboOffsets = intArrayOf(-1, -1)

    fun bind() {
        for(pbo in pboOffsets.indices) {
            with(Gdx.gl) {
                pboOffsets[pbo] = glGenBuffer()
                glBindBuffer(GL_PIXEL_PACK_BUFFER, pboOffsets[pbo])
                glBufferData(GL_PIXEL_PACK_BUFFER, 4, buffer, GL_STREAM_READ)
                glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            }
        }
    }

    fun read(x: Int, y: Int, frameBuffer: FrameBuffer?): ImmutableColor {
        var result = ImmutableColor()


        frameBuffer?.use {
            with(Gdx.gl30) {

                GL15.glBindBuffer(GL_PIXEL_PACK_BUFFER, pboOffsets[0])

                GL11.glReadPixels(x, frameBuffer.height - y, 1, 1, Pixmap.Format.toGlFormat(pixmapFormat), GL_UNSIGNED_BYTE, 0)
                // glGetTexImage(GL_TEXTURE_2D, 0, Pixmap.Format.toGlFormat(pixmapFormat), GL_UNSIGNED_INT, pixels)

                GL15.glBindBuffer(GL_PIXEL_PACK_BUFFER, pboOffsets[1])

                val readBuffer = GL15.glMapBuffer(GL_PIXEL_PACK_BUFFER, GL_READ_ONLY, 4, buffer)

                if (readBuffer != null && readBuffer.hasRemaining()) {
                    var byteBuffer = (0 until readBuffer.remaining()).map { readBuffer.get() }.toByteArray()
                    result = ImmutableColor.rgba8888ToColor(byteBuffer)
                    glUnmapBuffer(GL_PIXEL_PACK_BUFFER)

                }

                glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            }
        }

        val tmp = pboOffsets[1]
        pboOffsets[1] = pboOffsets[0]
        pboOffsets[0] = tmp

        return result
    }

    override fun dispose() {
        with(Gdx.gl){
            glBindBuffer(GL_PIXEL_PACK_BUFFER, 0)
            pboOffsets.forEach {
                glDeleteBuffer(it)
            }
        }
    }

    fun resize(width: Int, height: Int) {
        this.width = width
        this.height = height
        this.pixels = IntArray(width * height)
        this.buffer = BufferUtils.createByteBuffer(width * height * 4)
    }


}