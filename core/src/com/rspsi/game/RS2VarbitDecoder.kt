package com.rspsi.game

import com.displee.cache.CacheLibrary
import com.displee.cache.index.Index
import com.displee.io.impl.InputBuffer
import ktx.log.info

class RS2VarbitDecoder {

    init {
        context.bindSingleton { this }
    }

    val cacheLibrary: CacheLibrary = context.inject()

    val varbits = mutableMapOf<Int, RS2Varbit>()


    fun decode() {
        val varbitArchive = cacheLibrary.index(RS2CacheInfo.Indexes.CONFIGS).archive(RS2CacheInfo.ConfigTypes.VARBIT)

        varbitArchive?.files?.forEach { (index, file) ->
            val varbit = RS2Varbit(0, 0, 0)
            file.data?.let {
                val buffer = InputBuffer(it)

                while(true) {
                    val opcode = buffer.readUnsignedByte()
                    if(opcode == 0) {
                        break
                    } else if(opcode == 1) {
                        varbit.index = buffer.readUnsignedShort()
                        varbit.leastSignificantBit = buffer.readUnsignedByte()
                        varbit.mostSignificantBit = buffer.readUnsignedByte()
                    } else {
                        info { "Invalid varbit opcode $opcode"}
                        break
                    }
                }

                varbits[index] = varbit

            }
        }

        info {
            "Loaded ${varbits.size} area definitions"
        }

    }

    /*

    fun value() {
        val value: Int = varps.get(index)
        val mask = (1 shl mostSignificantBit - leastSignificantBit + 1) - 1
        return value shr leastSignificantBit and mask
    }
     */
}

data class RS2Varbit(var index: Int, var leastSignificantBit: Int, var mostSignificantBit: Int)