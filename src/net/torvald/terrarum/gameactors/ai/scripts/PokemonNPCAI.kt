package net.torvald.terrarum.gameactors.ai.scripts

/**
 * Randomly roams around.
 *
 * Created by SKYHi14 on 2016-12-23.
 */


object PokemonNPCAI : EncapsulatedString() {
    override fun getString() = """
timeCounter = 0
countMax = 0
moveMode = math.random() >= 0.5 and "left" or "right"
currentMode = "turn"

local function generateCountMax()
    function generateTurn()
        return 4600 + 1250 * math.random()
    end

    function generateWalk()
        return 568 + 342 * math.random()
    end

    return (currentMode == "move") and generateWalk() or generateTurn()
end

local function moveToDirection()
    if moveMode == "left" then
        ai.moveLeft(0.5)
    elseif moveMode == "right" then
        ai.moveRight(0.5)
    end
end

local function toggleCurrentMode()
    currentMode = (currentMode == "move") and "turn" or "move"
end

local function toggleMoveMode()
    moveMode = (moveMode == "left") and "right" or "left"
end

-------------------------------------------------------------------------------

countMax = generateCountMax()

function update(delta)
    timeCounter = timeCounter + delta

    if currentMode == "move" then
        moveToDirection()
    end

    if timeCounter >= countMax then
        timeCounter = 0
        toggleCurrentMode()
        countMax = generateCountMax()
        if currentMode == "turn" then
            toggleMoveMode()
        end
    end
end

"""
}
