package net.torvald.terrarum

/*import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import net.torvald.random.HQRNG
import net.torvald.terrarum.gameactors.Actor
import net.torvald.terrarum.gameitems.GameItem
import net.torvald.terrarum.gameworld.BlockLayer
import net.torvald.terrarum.gameworld.WorldTime
import net.torvald.terrarum.savegame.ByteArray64
import net.torvald.terrarum.utils.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.util.classSetOf
import org.jetbrains.uast.util.isInstanceOf
import java.math.BigInteger
import java.util.*

/**
 * https://medium.com/@vanniktech/writing-your-first-lint-check-39ad0e90b9e6
 * https://medium.com/mobile-app-development-publication/making-custom-lint-for-kotlin-code-8a6c203bf474
 * https://github.com/googlesamples/android-custom-lint-rules
 * https://googlesamples.github.io/android-custom-lint-rules/api-guide.html
 *
 * Created by minjaesong on 2023-05-22.
 */
class UnserialisableTypeIssueRegistry : IssueRegistry() {
    override val issues = listOf(ISSUE_SAVEGAME_UNSERIALISABLE_TYPE_USED_WITHOUT_TRANSIENT)
}

val ISSUE_SAVEGAME_UNSERIALISABLE_TYPE_USED_WITHOUT_TRANSIENT = Issue.create("TerrarumNonTransientUnserialisableType",
    "Unserialisable Type Used Without Care",
    "Unserialisable type is used on the potentially serialised class without @Transient annotation",
    Category.CORRECTNESS,
    9,
    Severity.ERROR,
    Implementation(TerrarumNonTransientUnserialisableType::class.java, EnumSet.of(Scope.JAVA_FILE))
)

class TerrarumNonTransientUnserialisableType : Detector(), Detector.UastScanner {
    override fun getApplicablePsiTypes() = listOf(UClass::class.java)
    override fun createUastHandler(context: JavaContext) = TerrarumNonTransientUnserialisableTypeHandler(context)

    class TerrarumNonTransientUnserialisableTypeHandler(private val context: JavaContext) : UElementHandler() {
        override fun visitClass(clazz: UClass) {
            /*if (clazz.name?.isDefinedCamelCase() == false) {
                context.report(ISSUE_SAVEGAME_UNSERIALISABLE_TYPE_USED_WITHOUT_TRANSIENT, clazz,
                    context.getNameLocation(clazz),
                    "Not named in defined camel case.")
            }*/
        }

        override fun visitField(node: UField) {
            if (node.uastParent.isInstanceOf(classSetOf(Actor::class.java, GameItem::class.java)) &&
                !node.hasAnnotation("Transient") &&
                !node.isInstanceOf(classSetOf(
                    // primitives
                    String::class.java,
                    Array<String>::class.java,
                    Boolean::class.java,
                    Byte::class.java,
                    ByteArray::class.java,
                    ByteArray64::class.java,
                    Short::class.java,
                    ShortArray::class.java,
                    Int::class.java,
                    IntArray::class.java,
                    Long::class.java,
                    LongArray::class.java,
                    Float::class.java,
                    FloatArray::class.java,
                    Double::class.java,
                    DoubleArray::class.java,
                    // has serialiser on net.torvald.terrarum.serialise.Common
                    BigInteger::class.java,
                    ZipCodedStr::class.java,
                    BlockLayer::class.java,
                    WorldTime::class.java,
                    HashArray::class.java,
                    HashedWirings::class.java,
                    HashedWiringGraph::class.java,
                    WiringGraphMap::class.java,
                    UUID::class.java,
                    HQRNG::class.java,
                ))) {

                context.report(
                    ISSUE_SAVEGAME_UNSERIALISABLE_TYPE_USED_WITHOUT_TRANSIENT,
                    node,
                    context.getNameLocation(node),
                    "Unserialisable type is used on the potentially serialised class without @Transient annotation"
                )
            }
        }
    }
}*/
