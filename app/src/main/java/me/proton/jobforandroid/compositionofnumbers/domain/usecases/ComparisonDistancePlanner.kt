package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import kotlin.random.Random

object ComparisonDistancePlanner {
    /** One assignment per sign position; null denotes EQUAL or the TEST level. */
    fun plan(
        level: ComparisonLevel,
        signs: List<ComparisonSign>,
        random: Random
    ): List<ComparisonDistance?> {
        val perSign = when (level) {
            ComparisonLevel.TEST -> 8
            ComparisonLevel.EASY -> 15
            ComparisonLevel.NORMAL -> 12
            ComparisonLevel.HARD -> 10
        }
        require(signs.size == perSign * 3 &&
            ComparisonSign.entries.all { sign -> signs.count { it == sign } == perSign }) {
            "Signs must form a complete balanced session for the level"
        }
        if (level == ComparisonLevel.TEST) return List(signs.size) { null }

        val closeCount: Int
        val mediumCount: Int
        val farCount: Int
        when (level) {
            ComparisonLevel.EASY -> {
                closeCount = 9
                mediumCount = if (random.nextBoolean()) 10 else 11
                farCount = 21 - mediumCount
            }
            ComparisonLevel.NORMAL -> {
                closeCount = 7
                mediumCount = if (random.nextBoolean()) 8 else 9
                farCount = 17 - mediumCount
            }
            ComparisonLevel.HARD -> {
                closeCount = 6
                mediumCount = 7
                farCount = 7
            }
            ComparisonLevel.TEST -> error("TEST has no distance categories")
        }

        val categories = mutableListOf<ComparisonDistance>().apply {
            repeat(closeCount) { add(ComparisonDistance.CLOSE) }
            repeat(mediumCount) { add(ComparisonDistance.MEDIUM) }
            repeat(farCount) { add(ComparisonDistance.FAR) }
            shuffle(random)
        }.iterator()
        return signs.map { sign ->
            if (sign == ComparisonSign.EQUAL) null else categories.next()
        }
    }
}
