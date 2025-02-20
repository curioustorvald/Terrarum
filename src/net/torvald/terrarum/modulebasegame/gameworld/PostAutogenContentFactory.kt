package net.torvald.terrarum.modulebasegame.gameworld

/**
 * Created by minjaesong on 2025-02-16.
 */
object PostAutogenContentFactory {

    operator fun invoke(type: String, args: List<Any>?): PostContents {
        TODO()
    }

    @PostContentAutogenFunction
    private fun moneyorder(args: List<Any>?): PostContents {
        val UUID_SENDER = args!![0].toString()
        val UUID_RECIPIENT = args!![1].toString()

        val xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <!DOCTYPE btexdoc SYSTEM "btexdoc.dtd">
        <btexdoc cover="post" inner="post" papersize="post">
            <cover hue="210">
                <title><v fromgame="POSTAL_TITLE_MONEY_ORDER"/></title>
                <postsender>${UUID_SENDER}</postsender>
                <postrecipient>${UUID_RECIPIENT}</postrecipient>
            </cover>
            <manuscript>
                <v fromgame="POSTAL_CONTENT_MONEY_ORDER"/>
            </manuscript>
        </btexdoc>
        """.trimIndent()

        return PostContents("btex", xml)
    }

}

annotation class PostContentAutogenFunction