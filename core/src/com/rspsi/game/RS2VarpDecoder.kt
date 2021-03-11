package com.rspsi.game

import com.displee.cache.CacheLibrary

class RS2VarpDecoder {

    init {
        context.bindSingleton { this }
    }

    var cacheLibrary: CacheLibrary = context.inject()
    fun decode() {
        val varpArchive = cacheLibrary.index(RS2CacheInfo.Indexes.CONFIGS).archive(RS2CacheInfo.ConfigTypes.VARPLAYER)
    }
}