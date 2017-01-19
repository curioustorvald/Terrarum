--
-- Created by minjaesong on 2017-01-06.
--
timeCounter = 0
countMax = 0
moveMode = math.random() >= 0.5 and "left" or "right"
currentMode = "move"
jumpheight = 6 -- lol

function generateCountMax()
    local function generateTurn()
        return 4600 + 1250 * math.random()
    end

    local function generateWalk()
        return 1645 + 402 * math.random()
    end

    return (currentMode == "move") and generateWalk() or generateTurn()
end

function moveToDirection(delta)
    local tiles = ai.getNearbyTiles(1)
    local ledges = ai.getLedgesHeight(1)

    if moveMode == "left" then
        if bit32.band(bit32.bor(tiles[0][-1], tiles[-1][-1]), 1) == 1 then
            ai.moveLeft(0.8)
            if ledges[-1] <= jumpheight then -- no futile jumps
                ai.jump()
            end
        else
            ai.moveLeft(0.5)
        end
    elseif moveMode == "right" then
        if bit32.band(bit32.bor(tiles[0][1], tiles[-1][1]), 1) == 1 then
            ai.moveRight(0.8)
            if ledges[1] <= jumpheight then -- no futile jumps
                ai.jump()
            end
        else
            ai.moveRight(0.5)
        end
    end

    timeCounter = timeCounter + delta
end

function toggleCurrentMode()
    currentMode = (currentMode == "move") and "turn" or "move"
end

function toggleMoveMode()
    moveMode = (moveMode == "left") and "right" or "left"
end

-------------------------------------------------------------------------------

countMax = generateCountMax()

function toggleCondition()
    local floorsheight = ai.getFloorsHeight(1)
    return timeCounter >= countMax or
            -- avoid great falls
            (timeCounter > 150 and (floorsheight[-1] > jumpheight or floorsheight[1] > jumpheight))
end

function update(delta)
    if currentMode == "move" then
        moveToDirection(delta)
    else
        timeCounter = timeCounter + delta -- no countup when jumping
    end

    if toggleCondition() then
        timeCounter = 0
        toggleCurrentMode()
        countMax = generateCountMax()
        if currentMode == "turn" then
            toggleMoveMode()
        end
    end
end
