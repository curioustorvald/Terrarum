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
    val recipient: String, // an identifier for an actor, an entity, etc.; actor identifier is always "UUID:<actor UUID>"
    val postmarkDate: Long, // world TIME_T
    val postType: Int = 0, // 0: regular post; 128: sent by system/NPCs; may be extended with future additions such as registered post
    val postContents: PostContents,
    val parcel: FixtureInventory?,
) {
    @Transient val postage = GamePostalService.calculateBasePostage(this).coerceAtLeast(1)
}

data class PostContents(
    val type: String, // "text", "btex"
    val contentsRaw: String, // plain text for "text"; xml for "btex"
    val encryption: String = "none", // "none", "end2end"
    val contentsExtra: Any? = null,
)


/*

Serialised Post Format

Endianness: little

Chunks:
- MAGIC
- HEADER
- CONTENTSRAW
- CONTENTSEXTRA
- PARCEL

When the mail is "sealed" (having encryption="end2end"), each chunk after the HEADER are encrypted.
Encryption method: AES-128 using ((128 bits Sender UUID) xor (128 bits Recipient UUID)) + ((float64 parcel weight) shake (date in postmark))

MAGIC: 'TeMeSbTR'
HEADER:
    - Int8   Protocol version (always 0x01)
    - Int8   Post type
    - Int8   Generic flags
    - Uint24 Offset to CONTENTSRAW
    - Uint24 Offset to PARCEL (0xFFFFFF to denote null)
    - Uint24 Offset to CONTENTSEXTRA (0xFFFFFF to denote null)
    - Int128 The world UUID
    - Int64  Date in Postmark (ingame world TIME_T)
    - Uint16 Length of Sender string
    - Bytes  Sender String
    - Uint16 Length of Recipient string
    - Bytes  Recipient String

    Post Type
    - 0: text, 1: btex

    Flags
    - 1: not posted (players can edit the contents)
    - 128: sealed

    Notes: post is either:
          1. always pre-paid when posted
          2. not yet encrypted when in not-posted status (can always calculate postage on the fly)
        so no postage information is needed for the Serialised Format

CONTENTSRAW
    - Uint48 unzipped size
    - Bytes  payload in Zstd compression

CONTENTSEXTRA
    - Uint48 unzipped size
    - Bytes  payload in Zstd compression

PARCEL
    - Uint48 unzipped size
    - Bytes  FixtureInventory JSON string in Zstd compression

 */