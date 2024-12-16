package net.torvald.terrarum

import com.badlogic.gdx.files.FileHandle
import com.badlogic.gdx.graphics.glutils.ShaderProgram
import com.badlogic.gdx.utils.Disposable
import net.torvald.terrarum.utils.CSVFetcher

/**
 * `default`, `default-batch`, and `default-shaperenderer` is pre-allocated. `default` is identical to `default-batch`.
 *
 * Created by minjaesong on 2024-12-16.
 */
object ShaderMgr : Disposable {

    private val mapping = HashMap<String, ShaderProgram>()

    fun compile(path: FileHandle) {
        CSVFetcher.readFromString(path.readString("utf-8")).forEach {
            val vert = it["vert"]
            val frag = it["frag"]
            val name = it["name"]

            mapping[name] = App.loadShaderFromClasspath("shaders/$vert", "shaders/$frag")
        }


        mapping["default"] = DefaultGL32Shaders.createSpriteBatchShader()
        mapping["default-batch"] = DefaultGL32Shaders.createSpriteBatchShader()
        mapping["default-shaperenderer"] = DefaultGL32Shaders.createShapeRendererShader()
    }

    operator fun get(name: String): ShaderProgram = mapping[name]!!

    override fun dispose() {
        mapping.values.forEach { it.dispose() }
    }

}