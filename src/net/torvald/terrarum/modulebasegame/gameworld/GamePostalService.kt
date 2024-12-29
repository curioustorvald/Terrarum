package net.torvald.terrarum.modulebasegame.gameworld

import net.torvald.terrarum.modulebasegame.gameactors.FixtureInventory
import kotlin.math.ceil

/**
 * Note: this implementation of the postal service won't reach outside a world; the attempt to interworld-delivery
 * must be made when the other world is being loaded (perhaps using the world loading hook)
 *
 * Created by minjaesong on 2024-12-29.
 */
class GamePostalService {


    companion object {
        private val reXmlTag = Regex("""<[^>]+>""")
        private val reIndents = Regex("""^ +""")
        private val reComments = Regex("""<!--|-->""")

        private fun removeTags(xml: String): String {
            return xml.replace(reXmlTag, "").replace(reIndents, "").replace(reComments, "")
        }

        fun calculateBasePostage(post: Post): Int {
            val baseUnits = when (post.postContents.type) {
                "text" -> {
                    post.postContents.contentsRaw.length / 1200.0
                }
                "btex" -> {
                    removeTags(post.postContents.contentsRaw).length / 1200.0
                }
                else -> {
                    TODO()
                }
            }

            val paperUnit = ceil(baseUnits)
            val parcelWeight = ceil(post.parcel?.totalWeight ?: 0.0) // 1 credit per 1 kg

            val extraCharge = when (post.postType) {
                0 -> 1.0
                128 -> 0.0
                else -> 1.0
            }

            return ceil((paperUnit + parcelWeight) * extraCharge).toInt()
        }
    }
}

data class Post(
    val sender: String, // an identifier for an actor, an entity, etc.; actor identifier is always "UUID:<actor UUID>"
    val receiver: String, // an identifier for an actor, an entity, etc.; actor identifier is always "UUID:<actor UUID>"
    val postmarkDate: Long, // world TIME_T
    val postType: Int = 0, // 0: regular post; 128: sent by system/NPCs; may be extended with future additions such as registered post
    val postContents: PostContents,
    val parcel: FixtureInventory?,
) {
    @Transient val basePostage = GamePostalService.calculateBasePostage(this)
}

data class PostContents(
    val type: String, // "text", "btex"
    val contentsRaw: String, // plain text for "text"; xml for "btex"
    val contentsExtra: Any? = null
)