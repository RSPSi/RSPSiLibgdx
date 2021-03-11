package com.rspsi.game


import ktx.log.debug
import org.pf4j.LegacyExtensionFinder
import org.pf4j.PluginClassLoader
import org.pf4j.PluginManager
import org.pf4j.processor.ExtensionStorage
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import java.net.URLClassLoader
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.jar.JarFile


class ScanningExtensionFinder(pluginManager: PluginManager?) : LegacyExtensionFinder(pluginManager) {

    init {
        checkForExtensionDependencies = true
    }
    override fun readPluginsStorages(): Map<String, Set<String>>? {
        debug { "Reading extensions storages from plugins" }
        val result: MutableMap<String, Set<String>> = LinkedHashMap()
        val plugins = pluginManager.plugins
        for (plugin in plugins) {
            val pluginId = plugin.descriptor.pluginId
            debug { "Reading extensions storage from plugin '$pluginId'" }
            val bucket: Set<String> = HashSet()
            try {
                debug { "Read '$EXTENSIONS_RESOURCE'" }
                val pluginClassLoader = plugin.pluginClassLoader as PluginClassLoader

                debug { "urls: ${pluginClassLoader.urLs}" }
                if(pluginClassLoader.urLs.isEmpty()){
                    pluginClassLoader.addFile(plugin.pluginPath.toFile())
                }
                debug { "urls2: ${pluginClassLoader.urLs}" }
                pluginClassLoader.getResourceAsStream(EXTENSIONS_RESOURCE).use { resourceStream ->
                    if (resourceStream == null) {
                        debug { "Cannot find '$EXTENSIONS_RESOURCE'" }
                    } else {
                        collectExtensions(resourceStream, bucket)
                    }
                }
                debugExtensions(bucket)
                result[pluginId] = bucket
            } catch (e: IOException) {
                debug { e.toString() }
            }
        }
        return result
    }


    @Throws(IOException::class)
    private fun collectExtensions(urls: Enumeration<URL>, bucket: Set<String>) {
        while (urls.hasMoreElements()) {
            val url = urls.nextElement()
            debug { "Read ${url.file}" }
            collectExtensions(url.openStream(), bucket)
        }
    }

    @Throws(IOException::class)
    private fun collectExtensions(inputStream: InputStream, bucket: Set<String>) {

        InputStreamReader(inputStream, StandardCharsets.UTF_8).use { reader -> ExtensionStorage.read(reader, bucket) }

    }




}