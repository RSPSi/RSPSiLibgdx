package com.rspsi.game

import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import ktx.log.info

class RS2AreaDecoder {

    init {
        context.bindSingleton { this }
    }

    val definitions = mutableMapOf<Int, RS2Area>()

    val cacheLibrary: CacheLibrary = context.inject()
    fun decode() {
        val areaArchive = cacheLibrary.index(RS2CacheInfo.Indexes.CONFIGS).archive(RS2CacheInfo.ConfigTypes.AREA)

        areaArchive?.files?.forEach { (index, file) ->

            file.data?.let {
                val buffer = InputBuffer(it)
                val area = RS2Area(index)
                while(true) {

                    when(val opcode = buffer.readUnsignedByte()) {
                        0 -> break
                        1 -> area.spriteId = buffer.readBigSmart()
                        2 -> area.field3294 = buffer.readBigSmart()
                        3 -> area.name = buffer.readString()
                        4 -> area.field3296 = buffer.read24BitInt()
                        5 -> buffer.read24BitInt()
                        6 -> area.field3310 = buffer.readUnsignedByte()
                        7 ->  {
                            val mask = buffer.readUnsignedByte()
                            /*if (mask and 1 === 0) {
                            }

                            if (mask and 2 === 2) {
                            }*/
                        }
                        8 -> buffer.readUnsignedByte()
                        in 10..14 -> area.field3298[opcode - 10] = buffer.readString()
                        15 -> {
                            val var3: Int = buffer.readUnsignedByte()
                            area.field3300 = IntArray(var3 * 2)


                            for(var4 in 0 until var3 * 2) {
                                area.field3300[var4] = buffer.readShort()
                            }

                            val unusedValue = buffer.readInt()

                            val count = buffer.readUnsignedByte()

                            area.field3292 = IntArray(count)

                            for(var5 in 0 until count) {
                                area.field3292[var5] = buffer.readInt()
                            }


                            area.field3309 = ByteArray(var3)

                            for(var5 in 0 until var3) {
                                area.field3309[var5] = buffer.readByte()
                            }
                        }
                        16 -> {}
                        17 -> area.field3308 = buffer.readString()
                        18 -> buffer.readBigSmart()
                        19 -> area.field3297 = buffer.readUnsignedShort()
                        21 -> buffer.readInt()
                        22 -> buffer.readInt()
                        23 -> buffer.read24BitInt()
                        24 -> buffer.readInt()//readshort twice
                        25 -> buffer.readBigSmart()
                        28 -> buffer.readUnsignedByte()
                        29 -> buffer.readUnsignedByte()
                        30 -> buffer.readUnsignedByte()


                        else -> {
                            info { "Invalid area opcode $opcode"}
                            break
                        }
                    }

                }

                definitions[index] = area
            }
        }

        info {
            "Loaded ${definitions.size} area definitions"
        }
    }
}

class RS2Area(val id: Int = 0) {

    var field3292: IntArray = IntArray(0)
    var spriteId: Int = -1
    var field3294: Int = -1
    var name: String? = null
    var field3296: Int = 0
    var field3297: Int = -1
    var field3298: Array<String?> = arrayOfNulls(5)
    var field3300: IntArray = IntArray(0)
    var field3308: String? = null
    var field3309: ByteArray = ByteArray(0)
    var field3310: Int = 0
}