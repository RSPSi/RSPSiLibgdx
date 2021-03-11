package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.g3d.Attribute
import com.badlogic.gdx.graphics.g3d.Attributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.FrameBuffer
import com.rspsi.ext.ImmutableColor
import com.rspsi.ext.withAlpha
import ktx.log.info


class PickableAttribute(var color: ImmutableColor) : Attribute(alias) {

    constructor(rgb888: Int): this(ImmutableColor.Companion.rgb888ToColor(rgb888))
    companion object {
        var alias = register("pickableObject")

        const val uniformName = "u_renderableId"

        init {
            alias = register("pickableObject")
            info {
                "Alias registered to $alias"
            }
        }
    }

    override fun compareTo(other: Attribute): Int =
        when (other) {
            is PickableAttribute -> {
                color.toIntBits() - other.color.toIntBits()
            }
            else -> {
                (type - other.type).toInt()
            }
        }

    override fun copy(): PickableAttribute {
        return PickableAttribute(color)
    }
}

class RayPickingShader(val renderable: Renderable, val config: Config) :
    DefaultShader(renderable, config, generatePrefix(renderable, config)) {

    companion object {

        private val tmpAttributes = Attributes()

        fun getAttributes(renderable: Renderable): Attributes {
            tmpAttributes.clear()
            if (renderable.environment != null) tmpAttributes.set(renderable.environment)
            if (renderable.material != null) tmpAttributes.set(renderable.material)

            return tmpAttributes
        }

        fun generatePrefix(renderable: Renderable, config: Config): String {
            var prefix = createPrefix(renderable, config)
            val combinedAttributes = getAttributes(renderable)

            if (combinedAttributes.has(PickableAttribute.alias)) {
                prefix += "#define rayPickRendering\n"
            }
            return prefix
        }
    }

    var unqiueColour = Uniform(PickableAttribute.uniformName, PickableAttribute.alias)

    val uniqueColourSetter: Setter = object : LocalSetter() {
        override fun set(shader: BaseShader, inputID: Int, renderable: Renderable, combinedAttributes: Attributes) {
            shader[inputID] = (combinedAttributes[PickableAttribute.alias] as PickableAttribute).color.toMutable()
        }
    }

    var uniqueColourUniformId = register(PickableAttribute.uniformName, unqiueColour, uniqueColourSetter)

    lateinit var camera: Camera
    lateinit var context: RenderContext
    override fun begin(camera: Camera?, context: RenderContext?) {
        camera?.let { this.camera = camera }
        context?.let { this.context = context }
       super.begin(camera, context)
    }


}