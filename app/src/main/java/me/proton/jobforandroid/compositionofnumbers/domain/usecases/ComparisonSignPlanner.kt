package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import kotlin.random.Random

object ComparisonSignPlanner {
    fun plan(level: ComparisonLevel, random: Random): List<ComparisonSign> {
        val countPerSign = when (level) {
            ComparisonLevel.TEST -> 8
            ComparisonLevel.EASY -> 15
            ComparisonLevel.NORMAL -> 12
            ComparisonLevel.HARD -> 10
        }
        val signs = mutableListOf<ComparisonSign>()
        for (sign in ComparisonSign.entries) {
            repeat(countPerSign) { signs.add(sign) }
        }
        signs.shuffle(random)
        return signs
    }
}
