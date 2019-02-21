import com.badlogic.gdx.graphics.Color
import com.google.gson.GsonBuilder
import net.torvald.terrarum.gameactors.Actor
import org.dyn4j.geometry.Vector2

/**
 * My anecdotes: GSON does NOT like anonymous class!
 *
 * Created by minjaesong on 2019-02-22
 */
object GsonTest {

    operator fun invoke() {
        val jsonString = GsonBuilder()
                .setPrettyPrinting()
                .serializeNulls()
                .create()
                .toJson(GsonTestActor())


        println(jsonString)
    }

}

class GsonTestActor : Actor(Actor.RenderOrder.MIDDLE) {
    override fun update(delta: Float) {
        TODO("not implemented")
    }

    override fun onActorValueChange(key: String, value: Any?) {
        TODO("not implemented")
    }

    override fun dispose() {
        TODO("not implemented")
    }

    override fun run() {
        TODO("not implemented")
    }

    init {
        referenceID = 42
    }

    private val mysecretnote = "note"
}

class GsonTestClass : GsonTestInterface {
    var foo = 1.567f
    val bar = "stingy"
    val baz = Color(0f, 0.2f, 0.4f, 1f)
    val fov = Vector2(1.324324, -0.4321)

    val bazget: Color
        get() = Color.CHARTREUSE

    @Transient override var superfoo = 42
    @Transient val tbar = "i'm invisible"
}

interface GsonTestInterface {
    var superfoo: Int
}

fun main(args: Array<String>) {
    GsonTest.invoke()
}