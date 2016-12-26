package net.torvald.terrarum.gameactors.ai.scripts

/**
 * Randomly roams around.
 *
 * Created by SKYHi14 on 2016-12-23.
 */
object PokemonNPCAI {
    operator fun invoke(): String = """
ai.jump()
ai.moveRight()

thisActorInfo = ai.getSelfActorInfo()
print(thisActorInfo.strength)
"""
}