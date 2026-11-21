package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import kotlin.random.Random

/** Generates a single HARD exercise; no session quotas or question history. */
object FractionHardQuestionGenerator {
    enum class ExerciseType { ADD, SUBTRACT, TO_MIXED_NUMBER }

    private val properSources: List<Fraction> = buildList {
        for (denominator in 2..15) {
            for (numerator in 1 until denominator) add(Fraction(numerator, denominator))
        }
    }
    private val conversionSources: List<Fraction> = buildList {
        for (denominator in 2..15) {
            for (numerator in denominator + 1 until 4 * denominator) add(Fraction(numerator, denominator))
        }
    }

    fun generate(type: ExerciseType, random: Random): FractionQuestion<out FractionAnswer> = when (type) {
        ExerciseType.ADD -> withFractionOptions(arithmeticExercise(addition = true, random = random), random)
        ExerciseType.SUBTRACT -> withFractionOptions(arithmeticExercise(addition = false, random = random), random)
        ExerciseType.TO_MIXED_NUMBER -> withMixedOptions(conversionExercise(random), random)
    }

    internal fun generate(
        type: ExerciseType, random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean
    ): FractionQuestion<out FractionAnswer> = when (type) {
        ExerciseType.ADD -> withFractionOptions(arithmeticExercise(true, random, accepts), random)
        ExerciseType.SUBTRACT -> withFractionOptions(arithmeticExercise(false, random, accepts), random)
        ExerciseType.TO_MIXED_NUMBER -> withMixedOptions(conversionExercise(random, accepts), random)
    }

    private fun arithmeticExercise(
        addition: Boolean, random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean = { true }
    ): FractionExercise<FractionAnswer.FractionValue> {
        val pairCount = properSources.size * properSources.size
        val start = random.nextInt(pairCount)
        for (offset in 0 until pairCount) {
            val index = (start + offset) % pairCount
            val left = properSources[index / properSources.size]
            val right = properSources[index % properSources.size]
            val answer = if (addition) left + right else left - right
            val valid = if (addition) FractionMathValidator.isValidHardAddition(left, right, answer)
                else FractionMathValidator.isValidHardSubtraction(left, right, answer)
            if (valid) {
                val exercise = if (addition) FractionExercise.Add(FractionArithmeticLevel.HARD, left, right)
                    else FractionExercise.Subtract(FractionArithmeticLevel.HARD, left, right)
                if (accepts(exercise)) return exercise
            }
        }
        error("No valid HARD arithmetic pair")
    }

    private fun conversionExercise(
        random: Random, accepts: (FractionExercise<out FractionAnswer>) -> Boolean = { true }
    ): FractionExercise.ToMixedNumber {
        val start = random.nextInt(conversionSources.size)
        for (offset in conversionSources.indices) {
            val source = conversionSources[(start + offset) % conversionSources.size]
            val whole = source.numerator / source.denominator
            val part = Fraction(source.numerator % source.denominator, source.denominator).reduced()
            if (FractionMathValidator.isValidHardConversion(source, whole, part)) {
                val exercise = FractionExercise.ToMixedNumber(source)
                if (accepts(exercise)) return exercise
            }
        }
        error("No valid HARD conversion source")
    }

    private fun withFractionOptions(
        exercise: FractionExercise<FractionAnswer.FractionValue>, random: Random
    ): FractionQuestion<FractionAnswer.FractionValue> {
        val correct = exercise.correctAnswer
        val value = correct.value
        // Local component-error candidates; improper distractors remain possible.
        val candidates = buildList {
            for (offset in 1..3) {
                add(Fraction(value.numerator + offset, value.denominator))
                if (value.numerator > offset) add(Fraction(value.numerator - offset, value.denominator))
                add(Fraction(value.numerator, value.denominator + offset))
            }
        }.shuffled(random)
        val options = mutableListOf(correct)
        for (candidate in candidates) {
            if (options.none { it.value.isEquivalentTo(candidate) }) {
                options.add(FractionAnswer.FractionValue(candidate))
                if (options.size == 4) break
            }
        }
        check(options.size == 4) { "Unable to form four distinct HARD fraction options" }
        options.shuffle(random)
        return FractionQuestion(exercise, options)
    }

    private fun withMixedOptions(
        exercise: FractionExercise.ToMixedNumber, random: Random
    ): FractionQuestion<FractionAnswer.MixedNumber> {
        val correct = exercise.correctAnswer
        val part = correct.fractionalPart
        // Extraction/counting mistakes, not new bounds on incorrect whole/fractional parts.
        // Whole-part neighbours differ by an integer; denominator neighbours stay in (0, 1).
        val candidates = buildList {
            for (offset in 1..3) {
                add(FractionAnswer.MixedNumber(correct.wholePart + offset, part))
                if (correct.wholePart > offset) add(FractionAnswer.MixedNumber(correct.wholePart - offset, part))
                add(FractionAnswer.MixedNumber(correct.wholePart, Fraction(part.numerator, part.denominator + offset)))
            }
        }
        val wrong = candidates.shuffled(random).take(3)
        check(wrong.size == 3) { "Unable to form three HARD mixed-number distractors" }
        return FractionQuestion(exercise, (wrong + correct).shuffled(random))
    }
}
