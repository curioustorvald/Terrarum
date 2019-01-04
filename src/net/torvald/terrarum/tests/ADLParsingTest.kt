package net.torvald.terrarum.tests

import net.torvald.spriteassembler.ADProperties
import java.io.StringReader

/**
 * Created by minjaesong on 2019-01-05.
 */
class ADLParsingTest {

    val TEST_STR = """
        SPRITESHEET=sprites/test
        EXTENSION=.tga.gz

        ANIM_RUN=DELAY 0.15;ROW 2
        ANIM_RUN_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_FWD_LEFT;ARM_FWD_RIGHT;LEG_LEFT;LEG_RIGHT
        ANIM_RUN_1=LEG_RIGHT 1,-1;LEG_LEFT -1,0
        ANIM_RUN_2=ALL 0,-1;LEG_RIGHT 0,1;LEG_LEFT 0,-1
        ANIM_RUN_3=LEG_RIGHT -1,0;LEG_LEFT 1,-1
        ANIM_RUN_4=ALL 0,-1;LEG_RIGHT 0,-1;LEG_LEFT 0,1

        ANIM_IDLE=DELAY 2;ROW 1
        ANIM_IDLE_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_REST_LEFT;ARM_REST_RIGHT;LEG_LEFT;LEG_RIGHT
        ANIM_IDLE_1=
        ! ANIM_IDLE_1 will not make any transformation
        ANIM_IDLE_2=UPPER_TORSO 0,-1

        ANIM_CROUCH=DELAY 1;ROW 3
        ANIM_CROUCH_BODYPARTS=HEAD;UPPER_TORSO;LOWER_TORSO;ARM_FWD_LEFT;ARM_FWD_RIGHT;LEG_CROUCH_LEFT;LEG_CROUCH_RIGHT
        ANIM_CROUCH_1=
    """.trimIndent()

    operator fun invoke() {
        val prop = ADProperties(StringReader(TEST_STR))

        prop.forEach { s, list ->
            println(s)

            list.forEach {
                println("\t$it")
            }
        }
    }

}

fun main(args: Array<String>) {
    ADLParsingTest().invoke()
}