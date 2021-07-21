package com.rspsi.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Material
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder
import com.badlogic.gdx.math.Matrix4
import com.badlogic.gdx.math.Quaternion
import com.displee.cache.CacheLibrary
import com.displee.io.impl.InputBuffer
import com.rspsi.ext.*
import com.rspsi.game.RS2Object.Types.WALL
import ktx.ashley.entity
import ktx.ashley.get
import ktx.ashley.mapperFor
import ktx.ashley.with
import ktx.log.info
import ktx.math.ImmutableVector2
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

class RS2ObjectsDecoder {

    init {
        context.bindSingleton { this }
    }

    val areaDecoder: RS2AreaDecoder = context.inject()
    val spriteProvider: RS2SpriteProvider = context.inject()
    val cacheLibrary: CacheLibrary = context.inject()
    val objDefinitionProvider: OSRSObjectDef = context.inject()
    val engine: Engine = context.inject()
    val modelDecoder: RS2ModelDecoder = context.inject()
    val xteaProvider: XTEAProvider = context.inject()
    val textureProvider: RS2TextureProvider = context.inject()
    val wallRotations = intArrayOf(1, 2, 4, 8)

    fun getOffset(x: Float, y: Float, width: Float, length: Float, rotation: Int) = ImmutableVector2(
        when (rotation and 3) {
            Orientation.north -> y
            Orientation.east -> x
            Orientation.south -> 7 - y - (width - 1)
            else -> 7 - x - (length - 1)
        },
        when (rotation and 3) {
            Orientation.north -> x
            Orientation.east -> 7 - y - (width - 1)
            Orientation.south -> 7 - x - (length - 1)

            else -> y
        }
    ) withZ 0


    fun request(regionId: Int) {

        val x: Int = (regionId shr 8) and 0xFF
        val y: Int = regionId and 0xFF

        val xteas = xteaProvider.xteas[regionId]
        val data = cacheLibrary.data(RS2CacheInfo.Indexes.MAPS, "l${x}_${y}", xteas)

        data?.let {
            decode(regionId, data)
        }


    }

    private var normalMergeIdx = 0
    private fun mergeNormals(first: RS2Model, second: RS2Model, dx: Float, dy: Float, dz: Float, flag: Boolean) {
        normalMergeIdx++
        val mergeableVerts = mutableMapOf<RS2Vertex, Int>()
        var count = 0
        for (firstVertex in first.vertices) {
            val y: Float = firstVertex.position.y - dy
            if (y <= second.boundary.minimumY) {
                val x: Float = firstVertex.position.x - dx
                if (x >= second.boundary.minimumX && x <= second.boundary.maximumX) {
                    val z: Float = firstVertex.position.z - dz
                    if (z >= second.boundary.minimumZ && z <= second.boundary.maximumZ) {
                        for (secondVertex in second.vertices) {
                            val pos = ImmutableVector3(x, y, z)
                            if (pos == secondVertex.position) {
                                mergeableVerts[firstVertex] = normalMergeIdx
                                mergeableVerts[secondVertex] = normalMergeIdx

                                count++
                            }
                        }
                    }
                }
            }

        }
        if (count < 3 || !flag) return
        for (face in first.faces) {
            if (mergeableVerts[first.vertices[face.a]] == normalMergeIdx &&
                mergeableVerts[first.vertices[face.b]] == normalMergeIdx &&
                mergeableVerts[first.vertices[face.c]] == normalMergeIdx
            ) {
                face.type = -1
            }
        }

        for (face in second.faces) {
            if (mergeableVerts[first.vertices[face.a]] == normalMergeIdx &&
                mergeableVerts[first.vertices[face.b]] == normalMergeIdx &&
                mergeableVerts[first.vertices[face.c]] == normalMergeIdx
            ) {
                face.type = -1
            }
        }

    }


    fun decode(regionId: Int, data: ByteArray) {

        val gameObjects = mutableListOf<RS2Object>()
        val buffer = InputBuffer(data)

        var idOffset = buffer.readSmart2()
        var id = -1

        while (idOffset != 0) {
            id += idOffset

            var packedPositions = 0
            var packedOffset = buffer.readSmart2()

            while (packedOffset != 0) {

                packedPositions += packedOffset - 1

                val localY = packedPositions and 0x3F
                val localX = (packedPositions shr 6) and 0x3F
                val plane = (packedPositions shr 12)

                val packedAttributes = buffer.readUnsignedByte()
                val type = packedAttributes shr 2
                val orientation = packedAttributes and 3

                gameObjects.add(
                    RS2Object().apply {
                        this.position = ImmutableVector3(localX, localY, plane)
                        this.id = id
                        this.rotation = orientation
                        this.type = type
                    }
                )
                packedOffset = buffer.readSmart2()
            }

            idOffset = buffer.readSmart2()
        }

        return spawn(gameObjects)
    }


    fun buildModel(
        gameObject: RS2Object,
        model: RS2Model,
        def: OSRSObjectDef.RS2ObjectDefinition,
        type: Int = 10,
        rotation: Int = 0,
        tileMap: Map<ImmutableVector3, RS2Tile>,
        transform: Matrix4 = Matrix4()
    ): Model {
        var mdl = model.cpy()

        val i = (def.inverted)
        val b = (rotation > 3)

        val inverted = (i xor b)

        if (inverted)
            mdl.invert()


        mdl.faces.forEach { face ->
            def.retextureToFind.forEachIndexed { index, textureToFind ->
                if (face.textureId == textureToFind) {
                    face.textureId = def.textureToReplace[index]
                }
            }

            def.originalColours.forEachIndexed { index, colourToFind ->
                if (face.colour == colourToFind) {
                    face.colour = def.replacementColours[index]
                }
            }
        }

        mdl.splitVertices()
        mdl.setVertexColours()
        mdl.computeBounds()


        mdl.generateNormals(64, 768, -50, -10, -50, !def.delayShading)

        if (type == 0) {
            // mdl.showNormals = true
        }


        if (type == 4 && rotation > 3) {
            transform.rotate(Quaternion().setEulerAngles(0f, 256f, 0f))
            transform.translate(ImmutableVector3(45f, 0f, -45f) / 128f)
        }
        transform.rotate(Quaternion().setEulerAngles(90f * (rotation and 3), 0f, 0f))

        val pos = gameObject.position
        val aY = tileMap[pos]?.data?.tileHeight ?: 0f
        val bY = tileMap[pos east 1]?.data?.tileHeight ?: 0f
        val cY = tileMap[pos northeast 1]?.data?.tileHeight ?: 0f
        val dY = tileMap[pos north 1]?.data?.tileHeight ?: 0f

        if (def.contouredGround) {
            val avgHeight: Float = (aY + bY + cY + dY) / 4f
            for (vertex in model.vertices) {
                val x: Float = vertex.position.x
                val z: Float = vertex.position.z
                val l2: Float = aY + (bY - aY) * (x + 64f) / 128f
                val i3: Float = dY + (cY - dY) * (x + 64f) / 128f
                val j3 = l2 + (i3 - l2) * (z + 64f) / 128f
                val zOffset = j3 - avgHeight
                vertex.position += ImmutableVector3(0f, 0f, zOffset)
            }
        }
        return mdl.build(transform)
    }


    fun buildRS2Models(
        gameObject: RS2Object,
    ): MutableMap<Int, RS2Model> {

        val rs2Models = mutableMapOf<Int, RS2Model>()
        val def = objDefinitionProvider.request(gameObject.id)

        if (def == null) {
            info { "Failed to find definition for ${gameObject.id}" }
            return rs2Models
        }
        val modelIds = def.findModelIds(gameObject.type)

        modelIds.forEach { id ->
            modelDecoder.request(id)?.let { rs2Models[id] = it }
        }

        when {
            rs2Models.isEmpty() -> {

            }
        }

        return rs2Models

    }


    fun getGdxModels(
        gameObject: RS2Object,
        rs2Models: MutableMap<String, RS2Model>,
        tileMap: Map<ImmutableVector3, RS2Tile>
    ): MutableMap<String, Model>? {

        val def = objDefinitionProvider.request(gameObject.id)

        if (def == null) {
            info { "Failed to find definition for ${gameObject.id} while getting GDX models" }
            return null
        }

        val builtModels = mutableMapOf<String, Model>()

        val area = if (def.areaId >= 0) areaDecoder.definitions[def.areaId] else null

        if (area != null) {
            val tileHeight = tileMap[gameObject.position]?.data?.tileHeight ?: 0f
            builtModels["area"] = ModelBuilder().createBox(
                1f, 1f, 1f,
                Material(TextureAttribute.createDiffuse(Texture(spriteProvider.spriteCache[area.spriteId]?.toPixmap()))),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.TextureCoordinates).toLong()
            )
            builtModels["area"]?.meshes?.forEach {
                it.transform(
                    Matrix4().translate(
                        ImmutableVector3(
                            0f,
                            ((tileHeight - 240f) / 240f) + 0.5f,
                            0f
                        )
                    )
                )
            }
        } else if (rs2Models.isEmpty()) {
            info { "Game object @$gameObject has null mdls | obj_${gameObject.id}_${gameObject.type}_${def.name}" }

            builtModels["box"] = ModelBuilder().createBox(
                1f, 1f, 1f,
                Material(ColorAttribute.createDiffuse(Color.GREEN)),
                (VertexAttributes.Usage.Position or VertexAttributes.Usage.ColorPacked).toLong()
            )


        } else {

            val oppositeRotation = gameObject.rotation + 1 and 3

            val transparency = rs2Models.values.map { rS2Model ->
                rS2Model.faces.map { face ->
                    if (face.textureId >= 0) textureProvider.textureDefinitions[face.textureId]?.let {
                        if (it.opaque) 0f else 1f
                    } ?: face.transparency else 1f
                }.maxOrNull() ?: 1f
            }.maxOrNull() ?: 1f

            when (gameObject.type) {

                in WALL -> {
                    rs2Models["wallpart_A"]?.let { model ->
                        builtModels["wallpart_A"] =
                            buildModel(gameObject, model, def, gameObject.type, 4 + gameObject.rotation, tileMap)
                    }
                    if (gameObject.type == 2) {
                        rs2Models["wallpart_B"]?.let { model ->
                            builtModels["wallpart_B"] =
                                buildModel(gameObject, model, def, gameObject.type, oppositeRotation, tileMap)
                        }
                    }
                }

                11 -> {

                    rs2Models["gameobject_rot45"]?.let { model ->
                        val yawOffset = Matrix4().setFromEulerAngles(45f, 0f, 0f)
                        builtModels["gameobject_rot45"] =
                            buildModel(gameObject, model, def, gameObject.type, gameObject.rotation, tileMap, yawOffset)
                    }

                }
                else -> {
                    rs2Models["gameobject"]?.let { model ->
                        builtModels["gameobject"] =
                            buildModel(gameObject, model, def, gameObject.type, gameObject.rotation, tileMap)
                    }
                }
            }
        }



        if (builtModels.isEmpty()) {

            info {
                "Failed to build models for $gameObject"
            }
            return null
        }

        return builtModels
    }

    fun spawn(rs2Objects: MutableList<RS2Object>) {

        info {
            "Spawning ${rs2Objects.size} game objects!"
        }

        val terrain = engine.entities.first {
            it[mapperFor<NameComponent>()]?.name?.startsWith("landscape") ?: false
        }

        val rs2TileMapComponent = terrain[mapperFor<RS2TileMapComponent>()]

        rs2TileMapComponent?.tileMap?.let { tileMap ->
            rs2Objects.filterNot { RS2Object.Types.WALL_DECORATION.contains(it.type) }.forEach { gameObject ->
                buildEntity(gameObject, tileMap)
            }
            rs2Objects.filter { RS2Object.Types.WALL_DECORATION.contains(it.type) }.forEach { gameObject ->
                buildEntity(gameObject, tileMap)
            }
            bake(tileMap)
            rs2Objects.forEach { rs2Object ->
                findEntities(rs2Object.position) { entity ->
                    val entityObject = entity[mapperFor<RS2ObjectComponent>()]?.rs2Object

                    rs2Object == entityObject
                }.forEach { entity ->

                    entity[mapperFor<RS2ModelComponent>()]?.models?.let { rs2Models ->
                        MeshProviderComponent().apply {
                            this.provider = { getGdxModels(rs2Object, rs2Models, tileMap) }
                            entity.add(this)
                        }

                    }
                }
            }
        }


    }

    fun findEntities(position: ImmutableVector3, filter: (Entity) -> Boolean): List<Entity> {
        return engine.entities.filter {
            it[mapperFor<WorldPositionComponent>()]?.vector3 == position
        }.filter(filter)
    }

    fun findEntities(position: ImmutableVector3, vararg types: IntRange): List<Entity> {
        return engine.entities.filter {
            val rs2ObjectComponent = it[mapperFor<RS2ObjectComponent>()]
            val positionComponent = it[mapperFor<WorldPositionComponent>()]
            positionComponent?.vector3 == position
                    && types.any { typeRange -> typeRange.contains(rs2ObjectComponent?.rs2Object?.type ?: -1) }
        }
    }

    fun buildEntity(gameObject: RS2Object, tileMap: Map<ImmutableVector3, RS2Tile>) {


        val tileHeight = tileMap[gameObject.position]?.data?.tileHeight ?: 0f


        val def = objDefinitionProvider.request(gameObject.id)

        if (def == null) {
            info { "Failed to find definition for ${gameObject.id} while building entity" }
            return
        }

        val offset = if (def.width > 1 || def.length > 1)
            getOffset(
                (gameObject.position.x.toInt() and 7).toFloat(),
                (gameObject.position.y.toInt() and 7).toFloat(),
                def.width.toFloat(),
                def.length.toFloat(),
                gameObject.rotation
            )
        else
            ImmutableVector3.ZERO

        gameObject.applyVarbit()
        var modelsWithId = buildRS2Models(gameObject)
        if (modelsWithId.isEmpty()) {
            return
        }
        val modelsNamed = mutableMapOf<String, RS2Model>()

        val modelIndex = if (def.modelTypes.isEmpty()) 0 else def.modelTypes.indexOf(gameObject.type)
        when (gameObject.type) {
            in WALL -> {
                modelsWithId[def.modelIds[modelIndex]]?.let { model ->
                    modelsNamed["wallpart_A"] = model//TODO make this better
                    if (gameObject.type == 2)
                        modelsNamed["wallpart_B"] = model
                }
            }

            11 -> {
                modelsWithId[def.modelIds[modelIndex]]?.let { model ->
                    modelsNamed["gameobject_rot45"] = model
                }

            }
            else -> {
                modelsWithId[def.modelIds[modelIndex]]?.let { model ->
                    modelsNamed["gameobject"] = model
                }
            }
        }


        /* */
        var zDisplacement = 0f

        if (gameObject.type == 5) {
            zDisplacement = findEntities(gameObject.position, WALL).map {
                val rs2ObjectComponent = it[mapperFor<RS2ObjectComponent>()]
                rs2ObjectComponent?.let {
                    val definition = objDefinitionProvider.request(rs2ObjectComponent.rs2Object.id)
                    definition?.decorDisplacement?.toFloat() ?: 0f
                } ?: 0f
            }.maxOrNull() ?: 16f


        }

        zDisplacement /= 128f
        engine.entity {

            with<TransparencyComponent> {
                this.transparency = 1f
            }
            with<NameComponent> {
                name = "obj_${gameObject.id}_${gameObject.type}_${def.name}"
            }
            with<WorldPositionComponent> {
                vector3 = gameObject.position
            }
            with<LocalPositionComponent> {
                vector3 = ImmutableVector3(0f, (tileHeight / 128f), 0f)
            }
            with<TranslateComponent> {
                vector3 = ImmutableVector3(0.5f, zDisplacement, 0.5f)
            }

            with<RS2ObjectComponent> {
                this.rs2Object = gameObject
            }

            with<RS2ModelComponent> {
                this.models.putAll(modelsNamed)
            }


        }
    }



    private fun surroundingTilesNE(position: ImmutableVector3, radius: Int = 1): MutableList<ImmutableVector3> {

        return mutableListOf(position, position east 1, position north 1, position northeast 1)
    }

    private fun getHeight(position: ImmutableVector3, tileMap: Map<ImmutableVector3, RS2Tile>) =
        tileMap[position]?.data?.tileHeight ?: 0f

    private fun getAverageHeight(position: ImmutableVector3, tileMap: Map<ImmutableVector3, RS2Tile>, radius: Int = 1) =
        surroundingTilesNE(position, radius).map { getHeight(it, tileMap) }.average().toFloat()

    fun mergeNearbyModels(
        model: RS2Model, position: ImmutableVector3, size: ImmutableVector2, tileMap: Map<ImmutableVector3, RS2Tile>,
        averageTileHeight: Float, adjacentPositions: List<ImmutableVector3>
    ) {
        adjacentPositions
            .flatMap { adjacent -> findEntities(adjacent, WALL) }
            .forEach { wall ->
                val wallPos = wall[mapperFor<WorldPositionComponent>()]?.vector3 ?: return@forEach
                val models = wall[mapperFor<RS2ModelComponent>()]?.models ?: return@forEach

                val wallAvg = averageTileHeight - getAverageHeight(wallPos, tileMap, 1)
                val dx = (wallPos.x - position.x) * 128f + (1f - size.x) * 64f
                val dy = (wallPos.y - position.y) * 128f + (1f - size.y) * 64f
                models["wallpart_A"]?.let { otherPartA ->
                    mergeNormals(model, otherPartA, dx, wallAvg, dy, true)
                }
                models["wallpart_B"]?.let { otherPartB ->
                    mergeNormals(model, otherPartB, dx, wallAvg, dy, true)
                }

            }
        adjacentPositions
            .flatMap { adjacent -> findEntities(adjacent, RS2Object.Types.INTERACTIVE) }
            .forEach { interactive ->

                val interactivePos = interactive[mapperFor<WorldPositionComponent>()]?.vector3 ?: return@forEach
                val models = interactive[mapperFor<RS2ModelComponent>()]?.models ?: return@forEach
                val rs2Object = interactive[mapperFor<RS2ObjectComponent>()]?.rs2Object ?: return@forEach
                val def = objDefinitionProvider.request(rs2Object.id) ?: return@forEach

                val wallAvg = averageTileHeight - getAverageHeight(interactivePos, tileMap, 1)
                val deltaX = when (rs2Object.rotation) {
                    1, 3 -> def.length
                    else -> def.width
                } + 1
                val deltaY = when (rs2Object.rotation) {
                    1, 3 -> def.width
                    else -> def.length
                } + 1
                val dx = (interactivePos.x - position.x) * 128f + (deltaX - size.x) * 64f
                val dy = (interactivePos.y - position.y) * 128f + (deltaY - size.y) * 64f
                models["gameobject"]?.let { otherPartA ->
                    mergeNormals(model, otherPartA, dx, wallAvg, dy, true)
                }
                models["gameobject_rot45"]?.let { otherPartB ->
                    mergeNormals(model, otherPartB, dx, wallAvg, dy, true)
                }
            }


    }

    fun bake(tileMap: Map<ImmutableVector3, RS2Tile>) {
        val sizeX = 1f
        val sizeY = 1f
        var flag = true
        tileMap.forEach { (position, tile) ->

            val averageTileHeight = getAverageHeight(position, tileMap, 1)
            val entitiesOnTile = findEntities(position, RS2Object.Types.ANY)
            val adjacentPositions = position.surrounding(radius = 1f, step = 1f, includeY = false)
            entitiesOnTile.forEach { entity ->

                val rs2ObjectComponent = entity[mapperFor<RS2ObjectComponent>()]
                val rs2ModelComponent = entity[mapperFor<RS2ModelComponent>()]
                entity[mapperFor<TranslateComponent>()]?.vector3?.let { wallTranslation ->
                    rs2ObjectComponent?.rs2Object?.let { rs2Object ->
                        rs2ModelComponent?.models?.let { rs2Models ->
                            when (rs2Object.type) {
                                in WALL -> {
                                    rs2Models["wallpart_A"]?.let { wallModel ->
                                        mergeNearbyModels(
                                            wallModel,
                                            position,
                                            1 withY 1,
                                            tileMap,
                                            averageTileHeight,
                                            adjacentPositions
                                        )

                                        rs2Models["wallpart_B"]?.let { wallModelB ->
                                            mergeNearbyModels(
                                                wallModelB,
                                                position,
                                                1 withY 1,
                                                tileMap,
                                                averageTileHeight,
                                                adjacentPositions
                                            )
                                            mergeNormals(wallModel, wallModelB, 0f, 0f, 0f, true)
                                        }
                                    }

                                }
                            }
                        }
                    }
                }


            }
        }

    }

    companion object {
        private val COSINE_VERTICES = intArrayOf(1, 0, -1, 0)
        private val SINE_VERTICIES = intArrayOf(0, -1, 0, 1)
    }
}


class RS2Object(
    var position: ImmutableVector3 = ImmutableVector3.ZERO,
    var id: Int = -1,
    var rotation: Int = 0,
    var type: Int = 10,
) {


    fun applyVarbit() {
    }

    override fun toString(): String {
        return "RS2Object(position=$position, id=$id, rotation=$rotation, type=$type)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RS2Object

        if (position != other.position) return false
        if (id != other.id) return false
        if (rotation != other.rotation) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = position.hashCode()
        result = 31 * result + id
        result = 31 * result + rotation
        result = 31 * result + type
        return result
    }

    object Types {
        fun getGroup(type: Int): IntRange {
            return validGroups.firstOrNull { it.contains(type) } ?: UNREGISTERED
        }

        val UNREGISTERED = -1..-1
        val ANY = 0..22
        val WALL = 0..3
        val WALL_DECORATION = 4..8
        val INTERACTIVE = 9..11
        val ROOF_PIECE = 12..21
        val FLOOR = 22..22

        //TODO This should probably be elsewhere
        val validGroups = mutableListOf(WALL, WALL_DECORATION, INTERACTIVE, ROOF_PIECE,  FLOOR)
    }


}