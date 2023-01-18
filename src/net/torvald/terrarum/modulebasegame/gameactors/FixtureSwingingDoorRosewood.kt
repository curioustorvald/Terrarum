package net.torvald.terrarum.modulebasegame.gameactors

import net.torvald.terrarum.BlockCodex
import net.torvald.terrarum.blockproperties.Block

/**
 * Created by minjaesong on 2022-07-28.
 */
class FixtureSwingingDoorOak : FixtureSwingingDoorBase {
    constructor() : super() {
        _construct(
                2,
                1,
                3,
                BlockCodex[Block.STONE].opacity,
                false,
                "basegame",
                "sprites/fixtures/door_basegame-48.tga",
                "fixtures-door_basegame-48.tga",
                "DOOR_OAK",
                true
        )
    }
}

class FixtureSwingingDoorEbony : FixtureSwingingDoorBase {
    constructor() : super() {
        _construct(
                2,
                1,
                3,
                BlockCodex[Block.STONE].opacity,
                false,
                "basegame",
                "sprites/fixtures/door_basegame-49.tga",
                "fixtures-door_basegame-49.tga",
                "DOOR_EBONY",
                true
        )
    }
}

class FixtureSwingingDoorBirch : FixtureSwingingDoorBase {
    constructor() : super() {
        _construct(
                2,
                1,
                3,
                BlockCodex[Block.STONE].opacity,
                false,
                "basegame",
                "sprites/fixtures/door_basegame-50.tga",
                "fixtures-door_basegame-50.tga",
                "DOOR_BIRCH",
                true
        )
    }
}

class FixtureSwingingDoorRosewood : FixtureSwingingDoorBase {
    constructor() : super() {
        _construct(
                2,
                1,
                3,
                BlockCodex[Block.STONE].opacity,
                false,
                "basegame",
                "sprites/fixtures/door_basegame-51.tga",
                "fixtures-door_basegame-51.tga",
                "DOOR_ROSEWOOD",
                true
        )
    }
}