package com.rspsi.game

import com.badlogic.ashley.core.Component
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.graphics.g3d.Model
import com.badlogic.gdx.graphics.g3d.ModelCache
import com.badlogic.gdx.graphics.g3d.decals.Decal
import com.badlogic.gdx.math.Quaternion
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody
import com.rspsi.ext.ImmutableVector3
import ktx.ashley.allOf



class LocalPositionComponent(var vector3: ImmutableVector3 = ImmutableVector3.ZERO) : Component

class WorldPositionComponent(var vector3: ImmutableVector3 = ImmutableVector3.ZERO) : Component

class VelocityComponent(var vector3: ImmutableVector3 = ImmutableVector3.ZERO) : Component

class TranslateComponent(var vector3: ImmutableVector3 = ImmutableVector3.ZERO) : Component

class RotateComponent(var yaw: Float = 0f, var pitch: Float = 0f, var roll: Float = 0f) : Component {
    fun quaternion(): Quaternion {
        return Quaternion().setEulerAngles(yaw, pitch, roll)
    }
}

class ScaleComponent(var vector3: ImmutableVector3 = ImmutableVector3(1f, 1f, 1f)) : Component

class DecalComponent: Component {
    lateinit var decal: Decal
}

class RigidBodyComponent: Component {
    lateinit var rigidBody: btRigidBody
}

class MeshComponent : Component {
    var gdxMeshes = mutableMapOf<String, Model>()
}
class MeshProviderComponent : Component {
    var provider: () -> MutableMap<String, Model>? = { null }
}

class ModelCacheComponent: Component {
    lateinit var modelCaches: Array<ModelCache>
}

class NameComponent(var name: String = ""): Component

class WeightComponent(var weight: Float = 0f): Component

class PropertiesComponent(var props: MutableMap<String, Any> = mutableMapOf()): Component

class IdentityComponent(var id: Int = -1): Component

class RS2ObjectComponent : Component {
    lateinit var rs2Object: RS2Object
}

class RS2ModelComponent: Component {
    var models = mutableMapOf<String, RS2Model>()
}

class RS2TileMapComponent(val tileMap: MutableMap<ImmutableVector3, RS2Tile> = mutableMapOf()): Component

class TransparencyComponent(var transparency: Float = 1f): Component

class DefaultComponents {
    companion object {

        fun get(): Family {

            return allOf(
                    LocalPositionComponent::class,
                    VelocityComponent::class,
                    TranslateComponent::class,
                    RotateComponent::class,
                    ScaleComponent::class,
                    MeshComponent::class,
                    NameComponent::class,
                    TransparencyComponent::class,
                    PropertiesComponent::class
            ).get()
        }


    }
}