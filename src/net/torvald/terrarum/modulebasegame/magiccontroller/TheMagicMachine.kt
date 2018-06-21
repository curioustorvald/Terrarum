package net.torvald.terrarum.modulebasegame.magiccontroller

/**
 * Created by minjaesong on 2018-06-03.
 */
class TheMagicMachine(skill: MagicianSkillDefinition) {

    val akkuPack = Array(skill.numberOfAccumulator, { TheMagicLanguage.MagicAccumulator() })



}

data class MagicianSkillDefinition(
        var numberOfAccumulator: Int,
        var useableMagicPower: Double,
        var runeComprehensionLevel: Int
)