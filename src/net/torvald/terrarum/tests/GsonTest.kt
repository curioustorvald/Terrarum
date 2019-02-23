
import net.torvald.terrarum.utils.JsonWriter
import org.dyn4j.geometry.Vector2

/**
 * My anecdotes: GSON does NOT like anonymous class!
 *
 * Created by minjaesong on 2019-02-22
 */
object GsonTest {

    private val testClass = GsonTestClass()

    init {
        testClass.foo = 42
        testClass.bar = 1f/42f
        testClass.baz = "According to all known laws of aviation, there is no way a bee should be able to fly."
        testClass.fov = Vector2(1.23432, -0.4)
    }


    operator fun invoke() {

        val gson = JsonWriter.getJsonBuilder()
        val jsonString = gson.toJson(testClass)

        println(jsonString)


        val deserialised = gson.fromJson(jsonString, GsonTestSuper::class.java)
        println(deserialised)
        println(deserialised as GsonTestClass) // ClassCastException
    }

}

open class GsonTestSuper(var foo: Int) {
    override fun toString() = "GsonTestSuper"
}

class GsonTestClass(
    foo: Int = 0,
    var bar: Float = 0f,
    var baz: String = "",
    var fov: Vector2 = Vector2(0.0,0.0)
) : GsonTestSuper(foo) {
    override fun toString() = "GsonTestClass(foo=$foo, bar=$bar, baz=$baz, fov=$fov)"
}

interface GsonTestInterface {
    var superfoo: Int
}

fun main(args: Array<String>) {
    GsonTest.invoke()
}