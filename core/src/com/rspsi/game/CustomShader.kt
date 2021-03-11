package com.rspsi.game

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.g3d.Attributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.Shader
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.DefaultShaderProvider
import com.rspsi.ext.TriplanarAttribute
import ktx.log.info
import kotlin.random.Random

private val tmpAttributes = Attributes()

private fun combineAttributes(renderable: Renderable): Attributes {
    tmpAttributes.clear()
    if (renderable.environment != null) tmpAttributes.set(renderable.environment)
    if (renderable.material != null) tmpAttributes.set(renderable.material)
    return tmpAttributes
}
private fun customPrefix(renderable: Renderable, config: DefaultShader.Config): String {
    var prefix = DefaultShader.createPrefix(renderable, config)
    val attributes = combineAttributes(renderable)
    val attributesMask = attributes.mask

    if (attributes.has(TriplanarAttribute.triplanarId)) {
        prefix += "#define triplanarFlag\n"
        info {
            "Defined triplanar flag"
        }
    }

    return prefix
}



class CustomShader(renderable: Renderable, config: Config) : DefaultShader(renderable, config, customPrefix(renderable, config)) {

    var u_cameraPos2 = 0
    var u_triplanarScale = 0
    var u_randomUV = 0

    init {
        u_cameraPos2 = register(Uniform("u_cameraPos2"))
        u_triplanarScale = register(Uniform("u_triplanarScale"))
        u_randomUV = register(Uniform("u_randomUV"))
    }

    val random = Random(69)

    override fun render(renderable: Renderable?, combinedAttributes: Attributes) {
        renderable?.let {
            if(combinedAttributes.has(TriplanarAttribute.triplanarId)) {

                program.setUniformf(u_randomUV, random.nextFloat(), random.nextFloat())
                program.setUniformf(
                    u_cameraPos2,
                    camera.position.x,
                    camera.position.y,
                    camera.position.z,
                    1.1881f / (camera.far * camera.far)
                )
                program.setUniformf(u_triplanarScale, 10f)
            }
        }
        super.render(renderable, combinedAttributes)
    }
}

class CustomShaderProvider(vertFile: FileHandle, fragFile: FileHandle): DefaultShaderProvider(vertFile, fragFile) {

    init {
    }

    override fun createShader(renderable: Renderable): Shader {
        return RayPickingShader(renderable, config)
    }

}