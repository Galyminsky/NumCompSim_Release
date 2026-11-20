package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import kotlin.random.Random

/** One NORMAL exercise at a time; no session quotas, sign balance or question history. */
object FractionNormalQuestionGenerator {
    enum class ExerciseType { COMPARE, REDUCE, ADD, SUBTRACT }

    private val sources: List<Fraction> = buildList {
        for (denominator in 2..12) {
            for (numerator in 1 until denominator) add(Fraction(numerator, denominator))
        }
    }

    fun generate(type: ExerciseType, random: Random): FractionQuestion<out FractionAnswer> = when (type) {
        ExerciseType.COMPARE -> {
            val exercise = FractionExercise.Compare(sources.random(random), sources.random(random))
            comparisonQuestion(exercise, random)
        }
        ExerciseType.REDUCE -> withFractionOptions(reductionExercise(random), random)
        ExerciseType.ADD -> withFractionOptions(arithmeticExercise(addition = true, random = random), random)
        ExerciseType.SUBTRACT -> withFractionOptions(arithmeticExercise(addition = false, random = random), random)
    }

    internal fun generate(
        type: ExerciseType, random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean
    ): FractionQuestion<out FractionAnswer> = when (type) {
        ExerciseType.COMPARE -> comparisonQuestion(comparisonExercise(random, accepts), random)
        ExerciseType.REDUCE -> withFractionOptions(reductionExercise(random, accepts), random)
        ExerciseType.ADD -> withFractionOptions(arithmeticExercise(true, random, accepts), random)
        ExerciseType.SUBTRACT -> withFractionOptions(arithmeticExercise(false, random, accepts), random)
    }

    private fun comparisonQuestion(
        exercise: FractionExercise.Compare, random: Random
    ): FractionQuestion<FractionAnswer.Sign> = FractionQuestion(exercise,
        FractionAnswer.ComparisonValue.values().map { FractionAnswer.Sign(it) }.shuffled(random))

    private fun comparisonExercise(
        random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean
    ): FractionExercise.Compare {
        val pairCount = sources.size * sources.size
        val start = random.nextInt(pairCount)
        for (offset in 0 until pairCount) {
            val index = (start + offset) % pairCount
            val exercise = FractionExercise.Compare(sources[index / sources.size], sources[index % sources.size])
            if (accepts(exercise)) return exercise
        }
        error("No NORMAL comparison satisfies session constraints")
    }

    private fun reductionExercise(
        random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean = { true }
    ): FractionExercise.Reduce {
        val start = random.nextInt(sources.size)
        for (offset in sources.indices) {
            val source = sources[(start + offset) % sources.size]
            if (FractionMathValidator.isValidNormalReduction(source, source.reduced())) {
                val exercise = FractionExercise.Reduce(source)
                if (accepts(exercise)) return exercise
            }
        }
        error("No valid NORMAL reduction source")
    }

    private fun arithmeticExercise(
        addition: Boolean, random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean = { true }
    ): FractionExercise<FractionAnswer.FractionValue> {
        val pairCount = sources.size * sources.size
        val start = random.nextInt(pairCount)
        // One finite cyclic traversal, including all same- and different-denominator pairs.
        for (offset in 0 until pairCount) {
            val index = (start + offset) % pairCount
            val left = sources[index / sources.size]
            val right = sources[index % sources.size]
            val answer = if (addition) left + right else left - right
            val valid = if (addition) FractionMathValidator.isValidNormalAddition(left, right, answer)
                else FractionMathValidator.isValidNormalSubtraction(left, right, answer)
            if (valid) {
                val exercise = if (addition) FractionExercise.Add(FractionArithmeticLevel.NORMAL, left, right)
                    else FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, left, right)
                if (accepts(exercise)) return exercise
            }
        }
        error("No valid NORMAL arithmetic pair")
    }

    private fun withFractionOptions(
        exercise: FractionExercise<FractionAnswer.FractionValue>, random: Random
    ): FractionQuestion<FractionAnswer.FractionValue> {
        val correct = exercise.correctAnswer
        val value = correct.value
        // Local distractor policy: plausible nearby component mistakes, not new validation bounds.
        // The three denominator neighbours alone provide three distinct wrong values.
        val candidates = buildList {
            for (offset in 1..3) {
                add(Fraction(value.numerator + offset, value.denominator))
                if (value.numerator > offset) add(Fraction(value.numerator - offset, value.denominator))
                add(Fraction(value.numerator, value.denominator + offset))
            }
        }.filter { it.numerator < it.denominator }.shuffled(random)
        val options = mutableListOf(correct)
        for (candidate in candidates) {
            if (options.none { it.value.isEquivalentTo(candidate) }) {
                options.add(FractionAnswer.FractionValue(candidate))
                if (options.size == 4) break
            }
        }
        check(options.size == 4) { "Unable to form four distinct NORMAL fraction options" }
        options.shuffle(random)
        return FractionQuestion(exercise, options)
    }
}
