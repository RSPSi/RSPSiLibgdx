package com.rspsi.ext

import com.badlogic.gdx.graphics.g3d.Attribute


class TriplanarAttribute : Attribute(triplanarId){
    companion object {
        const val alias: String = "triplanar"
        val triplanarId = register(alias)
    }


    override fun compareTo(other: Attribute): Int {
        if (type != other.type) return (type - other.type).toInt()
        return 0
    }

    override fun copy(): Attribute {
        val triplanarAttribute = TriplanarAttribute()
        return triplanarAttribute
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as TriplanarAttribute
        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + alias.hashCode()
        return result
    }


}