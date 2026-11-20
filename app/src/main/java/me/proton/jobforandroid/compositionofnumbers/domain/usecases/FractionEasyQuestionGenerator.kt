package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import kotlin.random.Random

/** Generates one requested EASY exercise, without session quotas or history. */
object FractionEasyQuestionGenerator {
    enum class ExerciseType { NUMERATOR, DENOMINATOR }

    fun generate(type: ExerciseType, random: Random): FractionQuestion<FractionAnswer.Integer> {
        val denominator = random.nextInt(2, 11)
        val numerator = random.nextInt(1, denominator)
        val source = Fraction(numerator, denominator)
        return withOptions(exercise(type, source), random)
    }

    internal fun generate(
        type: ExerciseType, random: Random, accepts: (FractionExercise<FractionAnswer.Integer>) -> Boolean
    ): FractionQuestion<FractionAnswer.Integer> {
        val sources = buildList {
            for (denominator in 2..10) {
                for (numerator in 1 until denominator) add(Fraction(numerator, denominator))
            }
        }
        val start = random.nextInt(sources.size)
        for (offset in sources.indices) {
            val candidate = exercise(type, sources[(start + offset) % sources.size])
            if (accepts(candidate)) return withOptions(candidate, random)
        }
        error("No unused EASY source satisfies session constraints")
    }

    private fun exercise(type: ExerciseType, source: Fraction): FractionExercise<FractionAnswer.Integer> = when (type) {
        ExerciseType.NUMERATOR -> FractionExercise.IdentifyNumerator(source)
        ExerciseType.DENOMINATOR -> FractionExercise.IdentifyDenominator(source)
    }

    private fun withOptions(
        exercise: FractionExercise<FractionAnswer.Integer>, random: Random
    ): FractionQuestion<FractionAnswer.Integer> {
        val correct = exercise.correctAnswer
        val otherComponent = when (exercise) {
            is FractionExercise.IdentifyNumerator -> exercise.source.denominator
            is FractionExercise.IdentifyDenominator -> exercise.source.numerator
            else -> error("Expected an EASY component exercise")
        }

        // Candidate policy, not additional answer bounds: confuse the two components,
        // or misread a nearby count. Three positive upward neighbours always exist here.
        val candidates = buildList {
            add(otherComponent)
            for (offset in 1..3) {
                add(correct.value - offset)
                add(correct.value + offset)
            }
        }
        val wrong = candidates.filter { it > 0 && it != correct.value }
            .distinct()
            .shuffled(random)
            .sortedBy { if (it == otherComponent) 0 else 1 }
            .take(3)
        check(wrong.size == 3) { "Unable to form three distinct EASY distractors" }

        val options = (wrong.map { FractionAnswer.Integer(it) } + correct).shuffled(random)
        return FractionQuestion(exercise, options)
    }
}
