package com.rspsi.game

import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.graphics.VertexAttributes
import com.badlogic.gdx.graphics.g3d.Attributes
import com.badlogic.gdx.graphics.g3d.Renderable
import com.badlogic.gdx.graphics.g3d.attributes.*
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader
import com.badlogic.gdx.graphics.g3d.utils.RenderContext
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import com.badlogic.gdx.utils.GdxRuntimeException

interface RS2ShaderProvider: Disposable {
    fun getShader(renderable: RenderableGameObject): RS2Shader
}

interface RS2Shader: Disposable {

    fun init()

    fun compareTo(other: RS2Shader): Int

    fun canRender(renderable: RenderableGameObject): Boolean

    fun begin(camera: Camera, context: RenderContext)

    fun render(renderableGameObject: RenderableGameObject)

    fun end()
}

abstract class BaseRS2ShaderProvider: RS2ShaderProvider {
    val shaders = mutableListOf<RS2Shader>()

    override fun getShader(renderable: RenderableGameObject): RS2Shader {
        val suggestedShader: RS2Shader? = renderable.shader
        if (suggestedShader != null && suggestedShader.canRender(renderable)) return suggestedShader
        for (shader in shaders) {
            if (shader.canRender(renderable)) return shader
        }
        val shader: RS2Shader = createShader(renderable)
        if (!shader.canRender(renderable)) throw GdxRuntimeException("unable to provide a shader for this renderable")
        shader.init()
        shaders.add(shader)
        return shader
    }

    abstract fun createShader(renderable: RenderableGameObject): RS2Shader

    override fun dispose() {
        shaders.forEach { it.dispose() }
        shaders.clear()
    }

}

open class DefaultRS2ShaderProvider(val vertexShader: String, val fragmentShader: String): BaseRS2ShaderProvider() {

    override fun createShader(renderable: RenderableGameObject): RS2Shader {
        return DefaultRS2Shader(renderable, ShaderProgram(vertexShader, fragmentShader))
    }

}

open class DefaultRS2Shader(val renderable: RenderableGameObject, val shaderProgram: ShaderProgram) : RS2Shader {

    override fun init() {

    }

    private fun and (mask: Long, flag: Long): Boolean {
        return (mask and flag) == flag
    }
    private fun or (mask: Long, flag: Long): Boolean {
        return (mask and flag) != 0L
    }

    open fun createPrefix(renderable: Renderable, config: DefaultShader.Config): String? {
        val attributes = Attributes()//DefaultShader.combineAttributes(renderable)
        var prefix = ""
        val attributesMask = attributes.mask
        val vertexMask = renderable.meshPart.mesh.vertexAttributes.mask
        if (and(vertexMask, VertexAttributes.Usage.Position.toLong())) prefix += "#define positionFlag\n"
        if (or(
                vertexMask,
                (VertexAttributes.Usage.ColorUnpacked or VertexAttributes.Usage.ColorPacked).toLong()
            )
        ) prefix += "#define colorFlag\n"
        if (and(vertexMask, VertexAttributes.Usage.BiNormal.toLong())) prefix += "#define binormalFlag\n"
        if (and(vertexMask, VertexAttributes.Usage.Tangent.toLong())) prefix += "#define tangentFlag\n"
        if (and(vertexMask, VertexAttributes.Usage.Normal.toLong())) prefix += "#define normalFlag\n"
        if (and(vertexMask, VertexAttributes.Usage.Normal.toLong()) || and(
                vertexMask,
                (VertexAttributes.Usage.Tangent or VertexAttributes.Usage.BiNormal).toLong()
            )
        ) {
            if (renderable.environment != null) {

                prefix = """
                    #define lightingFlag
                    #define ambientCubemapFlag
                    
                    #define numDirectionalLights ${config.numDirectionalLights}
                    #define numPointLights ${config.numPointLights}
                    #define numSpotLights ${config.numSpotLights}
                    
                """.trimIndent()



                if(attributes.has(ColorAttribute.Fog))
                    prefix += "#define fogFlag\n"
                if (renderable.environment.shadowMap != null) prefix += "#define shadowMapFlag\n"
                if (attributes.has(CubemapAttribute.EnvironmentMap)) prefix += "#define environmentCubemapFlag\n"
            }
        }
        val n = renderable.meshPart.mesh.vertexAttributes.size()
        for (i in 0 until n) {
            val attr = renderable.meshPart.mesh.vertexAttributes[i]
            if (attr.usage == VertexAttributes.Usage.BoneWeight) prefix += """
     #define boneWeight${attr.unit}Flag
     
     """.trimIndent() else if (attr.usage == VertexAttributes.Usage.TextureCoordinates) prefix += """
     #define texCoord${attr.unit}Flag
     
     """.trimIndent()
        }
        if (attributesMask and BlendingAttribute.Type == BlendingAttribute.Type) prefix += """
     #define ${BlendingAttribute.Alias}Flag
     
     """.trimIndent()
        if (attributesMask and TextureAttribute.Diffuse == TextureAttribute.Diffuse) {
            prefix += """
            #define ${TextureAttribute.DiffuseAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.DiffuseAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and TextureAttribute.Specular == TextureAttribute.Specular) {
            prefix += """
            #define ${TextureAttribute.SpecularAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.SpecularAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and TextureAttribute.Normal == TextureAttribute.Normal) {
            prefix += """
            #define ${TextureAttribute.NormalAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.NormalAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and TextureAttribute.Emissive == TextureAttribute.Emissive) {
            prefix += """
            #define ${TextureAttribute.EmissiveAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.EmissiveAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and TextureAttribute.Reflection == TextureAttribute.Reflection) {
            prefix += """
            #define ${TextureAttribute.ReflectionAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.ReflectionAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and TextureAttribute.Ambient == TextureAttribute.Ambient) {
            prefix += """
            #define ${TextureAttribute.AmbientAlias}Flag
            
            """.trimIndent()
            prefix += """
            #define ${TextureAttribute.AmbientAlias}Coord texCoord0
            
            """.trimIndent() // FIXME implement UV mapping
        }
        if (attributesMask and ColorAttribute.Diffuse == ColorAttribute.Diffuse) prefix += """
     #define ${ColorAttribute.DiffuseAlias}Flag
     
     """.trimIndent()
        if (attributesMask and ColorAttribute.Specular == ColorAttribute.Specular) prefix += """
     #define ${ColorAttribute.SpecularAlias}Flag
     
     """.trimIndent()
        if (attributesMask and ColorAttribute.Emissive == ColorAttribute.Emissive) prefix += """
     #define ${ColorAttribute.EmissiveAlias}Flag
     
     """.trimIndent()
        if (attributesMask and ColorAttribute.Reflection == ColorAttribute.Reflection) prefix += """
     #define ${ColorAttribute.ReflectionAlias}Flag
     
     """.trimIndent()
        if (attributesMask and FloatAttribute.Shininess == FloatAttribute.Shininess) prefix += """
     #define ${FloatAttribute.ShininessAlias}Flag
     
     """.trimIndent()
        if (attributesMask and FloatAttribute.AlphaTest == FloatAttribute.AlphaTest) prefix += """
     #define ${FloatAttribute.AlphaTestAlias}Flag
     
     """.trimIndent()
        if (renderable.bones != null && config.numBones > 0) prefix += """
     #define numBones ${config.numBones}
     
     """.trimIndent()
        return prefix
    }

    override fun compareTo(other: RS2Shader): Int {
        return 0
    }

    override fun canRender(RenderableGameObject: RenderableGameObject): Boolean {
        return false
    }

    override fun begin(camera: Camera, context: RenderContext) {

    }

    override fun render(RenderableGameObject: RenderableGameObject) {

    }

    override fun end() {

    }

    override fun dispose() {

    }

}