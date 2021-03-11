package com.rspsi.ext

import com.displee.io.impl.InputBuffer

fun InputBuffer.readUBoolean(): Boolean {
    return this.readUnsignedByte() == 1
}

fun InputBuffer.Companion.create(data: ByteArray, beginOffset: Int = 0): InputBuffer {
    val buffer = InputBuffer(data)
    buffer.offset = beginOffset
    return buffer
}

fun InputBuffer.readUTriByte(): Int {
    return ((this.readUnsignedByte() shl 16) + (this.readUnsignedByte() shl 8)
            + this.readUnsignedByte())

}


fun InputBuffer.decryptXTEA2(keys: IntArray, start: Int, end: Int) : ByteArray {
    var data: ByteArray = byteArrayOf()
    val l = offset
    offset = start
    val i1 = (end - start) / 8
    for (j1 in 0 until i1) {
        var k1: Int = readInt()
        var l1: Int = readInt()
        var sum = -0x3910c8e0
        val delta = -0x61c88647
        var k2 = 32
        while (k2-- > 0) {
            l1 -= keys[sum and 0x1c84 ushr 11] + sum xor (k1 ushr 5 xor k1 shl 4) + k1
            sum -= delta
            k1 -= (l1 ushr 5 xor l1 shl 4) + l1 xor keys[sum and 3] + sum
        }
        val outputBuffer = toOutputBuffer()
        outputBuffer.offset -= 8
        outputBuffer.writeInt(k1).writeInt(l1)
        data = outputBuffer.raw()
    }
    offset = l
    return data
}