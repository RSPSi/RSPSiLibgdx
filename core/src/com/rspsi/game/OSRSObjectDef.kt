package com.rspsi.game

import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.readUTriByte


class OSRSObjectDef {
    init {
        context.bindSingleton {
            this
        }
    }


    val fileProvider: CacheLibrary = context.inject()

    fun request(id: Int): RS2ObjectDefinition? {
        val objectsArchive = fileProvider.index(RS2CacheInfo.Indexes.CONFIGS).archive(RS2CacheInfo.ConfigTypes.OBJECT)
        val data = objectsArchive?.file(id)?.data
        return data?.let { decode(id, it) }
    }


    fun decode(id: Int, data: ByteArray): RS2ObjectDefinition {
        val buffer = InputBuffer(data)
        val definition = RS2ObjectDefinition()
        definition.id = id
        var interactive = -1
        var lastOpcode = -1
        do {
            val opcode: Int = buffer.readUnsignedByte()
            if (opcode == 0) {
                break
            }
            if (opcode == 1) {
                val count: Int = buffer.readUnsignedByte()
                if (count > 0) {
                    if (definition.modelIds.isEmpty()) {
                        definition.modelIds.clear()
                        definition.modelTypes.clear()
                        for (i in 0 until count) {
                            definition.modelIds.add(i, buffer.readUnsignedShort())
                            definition.modelTypes.add(i, buffer.readUnsignedByte())
                        }
                    } else {
                        buffer.offset += count * 3
                    }
                }
            } else if (opcode == 2) {
                definition.name = buffer.readString()
            } else if (opcode == 5) {
                val count: Int = buffer.readUnsignedByte()
                if (count > 0) {
                    if (definition.modelIds.isEmpty()) {
                        definition.modelTypes.clear()
                        for (i in 0 until count) {
                            definition.modelIds.add(i, buffer.readUnsignedShort())
                        }
                    } else {
                        buffer.offset += count * 2
                    }
                }
            } else if (opcode == 14) {
                definition.width = (buffer.readUnsignedByte())
            } else if (opcode == 15) {
                definition.length = (buffer.readUnsignedByte())
            } else if (opcode == 17) {
                definition.solid = (false)
            } else if (opcode == 18) {
                definition.impenetrable = (false)
            } else if (opcode == 19) {
                interactive = buffer.readUnsignedByte()
                if (interactive == 1) {
                    definition.interactive = (true)
                }
            } else if (opcode == 21) {
                definition.contouredGround = (true)
            } else if (opcode == 22) {
                definition.delayShading = (true)
            } else if (opcode == 23) {
                definition.occludes = (true)
            } else if (opcode == 24) {
                var animation: Int = buffer.readUnsignedShort()
                if (animation == 65535) {
                    animation = -1
                }
                definition.animation = (animation)
            } else if (opcode == 27) {
                //setInteractType(1);
            } else if (opcode == 28) {
                definition.decorDisplacement = (buffer.readUnsignedByte())
            } else if (opcode == 29) {
                definition.ambientLighting = (buffer.readByte())
            } else if (opcode == 39) {
                definition.lightDiffusion = (buffer.readByte())
            } else if (opcode in 30..38) {
                definition.interactions[opcode - 30] =  buffer.readString()
                if (definition.interactions[opcode - 30].equals("hidden", ignoreCase = true)) {
                    definition.interactions.removeAt(opcode - 30)
                }
            } else if (opcode == 40) {
                val count: Int = buffer.readUnsignedByte()
                definition.originalColours.clear()
                definition.replacementColours.clear()
                for (i in 0 until count) {
                    definition.originalColours.add(i, buffer.readUnsignedShort())
                    definition.replacementColours.add(i, buffer.readUnsignedShort())
                }
            } else if (opcode == 41) {
                val count: Int = buffer.readUnsignedByte()
                definition.retextureToFind.clear()
                definition.textureToReplace.clear()
                for (i in 0 until count) {
                    definition.retextureToFind.add(i, buffer.readUnsignedShort())
                    definition.textureToReplace.add(i, buffer.readUnsignedShort())
                }
            } else if (opcode == 60) {
                //definition.setMinimapFunction(buffer.readUnsignedShort());
            } else if (opcode == 62) {
                definition.inverted = (true)
            } else if (opcode == 64) {
                definition.castsShadow = (false)
            } else if (opcode == 65) {
                definition.scaleX = (buffer.readUnsignedShort())
            } else if (opcode == 66) {
                definition.scaleY = (buffer.readUnsignedShort())
            } else if (opcode == 67) {
                definition.scaleZ = (buffer.readUnsignedShort())
            } else if (opcode == 68) {
                definition.mapscene = (buffer.readUnsignedShort())
            } else if (opcode == 69) {
                definition.surroundings = (buffer.readUnsignedByte()) //Not used in OSRS?
            } else if (opcode == 70) {
                definition.translateX = (buffer.readShort())
            } else if (opcode == 71) {
                definition.translateY = (buffer.readShort())
            } else if (opcode == 72) {
                definition.translateZ = (buffer.readShort())
            } else if (opcode == 73) {
                definition.obstructsGround = (true)
            } else if (opcode == 74) {
                definition.hollow = (true)
            } else if (opcode == 75) {
                definition.supportItems = (buffer.readUnsignedByte())
            } else if (opcode == 77 || opcode == 92) {
                var varbit: Int = buffer.readUnsignedShort()
                if (varbit == 65535) {
                    varbit = -1
                }
                var varp: Int = buffer.readUnsignedShort()
                if (varp == 65535) {
                    varp = -1
                }
                var var3 = -1
                if (opcode == 92) {
                    var3 = buffer.readUnsignedShort()
                    if (var3 == 65535) var3 = -1
                }
                val count: Int = buffer.readUnsignedByte()
                definition.morphisms.clear()
                for (i in 0..count) {
                    var morphism = buffer.readUnsignedShort()
                    if (morphism == 65535) {
                        morphism = -1
                    }
                    definition.morphisms.add(i, morphism)
                }
                definition.morphisms.add(count + 1, var3)
                definition.varbit = (varbit)
                definition.varp = (varp)
            } else if (opcode == 78) { //TODO Figure out what these do in OSRS
                //First short = ambient sound
                buffer.offset +=3
            } else if (opcode == 79) {
                buffer.offset += 5
                val count: Int = buffer.readUnsignedByte()

                buffer.offset += 2 * count
            } else if (opcode == 81) {

                buffer.offset++
            } else if (opcode == 82) {
                definition.areaId = (buffer.readUnsignedShort()) //AreaType
            } else if (opcode == 249) {
                val var1: Int = buffer.readUnsignedByte()
                for (var2 in 0 until var1) {
                    val b = buffer.readUnsignedByte() == 1
                    val var5: Int = buffer.readUTriByte()
                    if (b) {
                        buffer.readString()
                    } else {
                        buffer.readInt()
                    }
                }
            } else {
                println("ObjId: " + id + ", Unrecognised object opcode " + opcode + " last;" + lastOpcode + "ID: " + id)
                continue
            }
            lastOpcode = opcode
        } while (true)

        if (interactive == -1) {
            definition.interactive = (definition.modelIds.isNotEmpty() && (definition.modelTypes.isEmpty() || definition.modelTypes[0] == 10) || definition.interactions.isNotEmpty())
        }

        if (definition.hollow) {
            definition.solid = (false)
            definition.impenetrable = (false)
        }

        if (definition.supportItems == -1) {
            definition.supportItems = (if (definition.solid) 1 else 0)
        }


        return definition
    }

    
    
    class RS2ObjectDefinition {

        fun findModelIds(type: Int): List<Int> {
            return if(modelTypes.isNotEmpty()){
                val indices = modelTypes.withIndex().filter { it.value == type }.map{ it.index }
                modelIds.withIndex().filter { indices.contains(it.index) }.map { it.value }
            } else {
                modelIds
            }
        }

        var ambientLighting: Byte = 0
        var animation = 0
        var castsShadow = false
        var contouredGround = false
        var decorDisplacement = 0
        var delayShading = false
        var description: String? = null
        var hollow = false
        var id = -1
        var impenetrable = false
        var interactions = arrayListOf("", "", "", "", "")
        var interactive = false
        var inverted = false
        var length = 0
        var lightDiffusion: Byte = 0
        var mapscene = 0
        var minimapFunction = 0
        var modelIds = arrayListOf<Int>()
        var modelTypes = arrayListOf<Int>()
        var morphisms = arrayListOf<Int>()
        var varbit = 0
        var varp = 0
        var name: String? = null
        var obstructsGround = false
        var occludes = false
        var originalColours = arrayListOf<Int>()
        var replacementColours = arrayListOf<Int>()
        var retextureToFind = arrayListOf<Int>()
        var textureToReplace = arrayListOf<Int>()
        var scaleX = 0
        var scaleY = 0
        var scaleZ = 0
        var solid = false
        var supportItems = 0
        var surroundings = 0
        var translateX = 0
        var translateY = 0
        var translateZ = 0
        var width = 0

        var areaId = -1
    }
    
}