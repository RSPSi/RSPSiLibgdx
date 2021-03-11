package com.rspsi.game

import com.beust.klaxon.JsonArray
import com.beust.klaxon.Klaxon
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import ktx.inject.Context
import ktx.log.info
import java.io.File
import kotlin.jvm.Throws

class XTEAProvider {

    init {
        context.bindSingleton { this }
    }
    val xteas = mutableMapOf<Int, IntArray>()

    fun decode(xteaFile: File){
        xteas.clear()
        val xteaList = Klaxon().parseArray<XTEA>(xteaFile)

        xteaList?.forEach { xteas[it.mapsquare] = it.key }
        info {

            "Loaded ${xteas.size} xteas"
        }
    }

    @Throws(java.lang.IllegalStateException::class)
    fun insertOrFail(xtea: XTEA) {
      if(xteas.containsKey(xtea.mapsquare))
          throw IllegalStateException()


    }

    fun replaceOrInsert(xtea: XTEA) {

    }
}

data class XTEA(val mapsquare: Int, val key: IntArray = intArrayOf(0, 0, 0, 0)) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as XTEA

        if (mapsquare != other.mapsquare) return false
        if (!key.contentEquals(other.key)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = mapsquare
        result = 31 * result + key.contentHashCode()
        return result
    }

}