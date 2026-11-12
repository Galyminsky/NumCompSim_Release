package me.proton.jobforandroid.compositionofnumbers.data

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonBasicSessionGenerator
import kotlin.random.Random

/** Provides a fresh, complete comparison session for each request. */
class ComparisonSessionDataSource {
    fun generateQuestions(
        level: ComparisonLevel,
        random: Random = Random.Default
    ): List<ComparisonQuestion> = ComparisonBasicSessionGenerator.generate(level, random)
}
