package net.torvald.terrarum.gameactors.ai.scripts

/**
 * Randomly roams around.
 *
 * Created by SKYHi14 on 2016-12-23.
 */
object PokemonNPCAI {
    operator fun invoke(): String = """

timeCounter = 0
countMax = 0
moveMode = math.random() >= 0.5 and "left" or "right"
currentMode = "turn"

function generateTurn()
    return 4600 + 1250 * math.random()
end

function generateWalk()
    return 568 + 342 * math.random()
end



countMax = generateTurn()

function update(delta)
    timeCounter = timeCounter + delta

    if currentMode == "turn" then
        -- wait
        -- reset counter
        if timeCounter >= countMax then
            timeCounter = 0
            currentMode = "move"
            countMax = generateWalk()
            moveMode = (moveMode == "left") and "right" or "left"
        end
    elseif currentMode == "move" then
        -- move
        if moveMode == "left" then
            ai.moveLeft()
        elseif moveMode == "right" then
            ai.moveRight()
        end
        -- reset counter
        if timeCounter >= countMax then
            timeCounter = 0
            currentMode = "turn"
            countMax = generateTurn()
        end
    end
end
"""
}