package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import kotlin.random.Random

/** Generates one TEST question, without session history or graphics. */
object FractionTestQuestionGenerator {
    fun generate(random: Random): FractionQuestion<FractionAnswer.FractionValue> {
        val totalParts = random.nextInt(2, 8)
        val shadedParts = random.nextInt(1, totalParts)
        val exercise = FractionExercise.IdentifyFraction(totalParts, shadedParts)
        return withOptions(exercise, random)
    }

    /** Session selection must exhaust candidates rather than retry an unrestricted random draw. */
    internal fun generate(
        random: Random, accepts: (FractionExercise.IdentifyFraction) -> Boolean
    ): FractionQuestion<FractionAnswer.FractionValue> {
        val sources = buildList {
            for (total in 2..7) {
                for (shaded in 1 until total) add(FractionExercise.IdentifyFraction(total, shaded))
            }
        }
        val start = random.nextInt(sources.size)
        for (offset in sources.indices) {
            val exercise = sources[(start + offset) % sources.size]
            if (accepts(exercise)) return withOptions(exercise, random)
        }
        error("No unused TEST source satisfies session constraints")
    }

    private fun withOptions(
        exercise: FractionExercise.IdentifyFraction, random: Random
    ): FractionQuestion<FractionAnswer.FractionValue> {
        val totalParts = exercise.totalParts
        val shadedParts = exercise.shadedParts
        val correct = exercise.correctAnswer

        // Candidate policy, not an additional validity range for all TEST answers:
        // use ratios of possible TEST pictures, prioritising mistakes in either count.
        val candidates = buildList {
            for (denominator in 2..7) {
                for (numerator in 1 until denominator) {
                    add(Fraction(numerator, denominator))
                }
            }
        }.shuffled(random).sortedBy {
            when {
                it.denominator == totalParts -> 0
                it.numerator == shadedParts -> 1
                else -> 2
            }
        }

        val options = mutableListOf(correct)
        // At most 21 candidates; never retry random draws until a value happens to fit.
        for (candidate in candidates) {
            if (options.none { it.value.isEquivalentTo(candidate) }) {
                options.add(FractionAnswer.FractionValue(candidate))
                if (options.size == 4) break
            }
        }
        check(options.size == 4) { "Unable to form four distinct TEST fraction options" }
        options.shuffle(random)
        return FractionQuestion(exercise, options)
    }
}
