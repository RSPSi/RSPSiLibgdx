package com.rspsi.game

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.systems.IntervalSystem
import com.badlogic.ashley.utils.ImmutableArray
import com.badlogic.gdx.utils.Array
import ktx.ashley.allOf
import ktx.ashley.get
import ktx.ashley.mapperFor

open class MovementSystem(time: Float): IntervalSystem(time){
    var entities = ImmutableArray<Entity>(Array())

    override fun addedToEngine(engine: Engine?) {
        var family = allOf(LocalPositionComponent::class, VelocityComponent::class).get()
        entities = engine!!.getEntitiesFor(family)
    }
    override fun updateInterval() {
        entities.forEach {
        }
    }


    companion object {
        val positionMapper = mapperFor<LocalPositionComponent>()
        val velocityMapper = mapperFor<VelocityComponent>()
    }

}