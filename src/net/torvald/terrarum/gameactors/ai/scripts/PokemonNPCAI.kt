package net.torvald.terrarum.gameactors.ai.scripts

/**
 * Randomly roams around.
 *
 * Created by SKYHi14 on 2016-12-23.
 */
object PokemonNPCAI {
    operator fun invoke(): String = """

counter = 1

function update(delta)
    ai.moveRight()
    print("delta", delta)
    counter = counter + delta
    print("testcounter", counter)
end
"""
}