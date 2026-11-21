package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionHardQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionHardQuestionGenerator.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionHardQuestionGeneratorTest {
    @Test(timeout = 10000) fun oneHundredSeedsForEachTypeProduceValidQuestions() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) assertValid(type, FractionHardQuestionGenerator.generate(type, Random(seed)))
        }
    }

    @Test fun sameSeedReproducesEachTypeAndExactOptionOrder() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) {
                val first = FractionHardQuestionGenerator.generate(type, Random(seed))
                val second = FractionHardQuestionGenerator.generate(type, Random(seed))
                assertEquals(first.exercise, second.exercise)
                assertEquals(first.correctAnswer, second.correctAnswer)
                assertEquals(first.options, second.options)
            }
        }
    }

    @Test fun arithmeticAllowsProperAndImproperSumsAndLcmExactly60() {
        assertArithmetic(ExerciseType.ADD, Fraction(1, 2), Fraction(1, 3), Fraction(5, 6))
        assertArithmetic(ExerciseType.ADD, Fraction(3, 4), Fraction(2, 3), Fraction(17, 12))
        assertArithmetic(ExerciseType.ADD, Fraction(14, 15), Fraction(1, 12), Fraction(61, 60))
        assertArithmetic(ExerciseType.SUBTRACT, Fraction(14, 15), Fraction(1, 12), Fraction(17, 20))
        assertArithmetic(ExerciseType.SUBTRACT, Fraction(1, 2), Fraction(1, 15), Fraction(13, 30))
    }

    @Test fun searchSkipsSameDenominatorsIntegerZeroNegativeAndLcm63() {
        val cases = listOf(
            Triple(ExerciseType.ADD, Fraction(1, 4), Fraction(1, 4)),
            Triple(ExerciseType.ADD, Fraction(1, 2), Fraction(2, 4)),
            Triple(ExerciseType.SUBTRACT, Fraction(3, 4), Fraction(1, 4)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 2), Fraction(2, 4)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 3), Fraction(1, 2)),
            Triple(ExerciseType.ADD, Fraction(1, 7), Fraction(1, 9)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 7), Fraction(1, 9))
        )
        for ((type, left, right) in cases) {
            val question = FractionHardQuestionGenerator.generate(type,
                firstChoices(sourceIndex(left) * 105 + sourceIndex(right)))
            assertFalse(conditionFractions(question.exercise) == listOf(left, right))
            assertValid(type, question)
        }
    }

    @Test fun conversionCoversWholePartsOneThroughThreeAndPreservesReducibleSources() {
        val cases = listOf(
            Fraction(3, 2) to FractionAnswer.MixedNumber(1, Fraction(1, 2)),
            Fraction(5, 2) to FractionAnswer.MixedNumber(2, Fraction(1, 2)),
            Fraction(7, 2) to FractionAnswer.MixedNumber(3, Fraction(1, 2)),
            Fraction(14, 4) to FractionAnswer.MixedNumber(3, Fraction(1, 2)),
            Fraction(59, 15) to FractionAnswer.MixedNumber(3, Fraction(14, 15))
        )
        for ((source, expected) in cases) {
            val question = FractionHardQuestionGenerator.generate(ExerciseType.TO_MIXED_NUMBER,
                firstChoices(conversionIndex(source)))
            assertEquals(FractionExercise.ToMixedNumber(source), question.exercise)
            assertEquals(expected, question.correctAnswer)
            assertValid(ExerciseType.TO_MIXED_NUMBER, question)
        }
    }

    @Test fun conversionSearchSkipsIntegerSources() {
        for (source in listOf(Fraction(4, 2), Fraction(6, 2), Fraction(30, 15), Fraction(45, 15))) {
            val question = FractionHardQuestionGenerator.generate(ExerciseType.TO_MIXED_NUMBER,
                firstChoices(conversionIndex(source)))
            assertFalse((question.exercise as FractionExercise.ToMixedNumber).source == source)
            assertValid(ExerciseType.TO_MIXED_NUMBER, question)
        }
    }

    @Test(timeout = 10000) fun everyAllowedNonintegerConversionSourceCanBeSelected() {
        for (denominator in 2..15) {
            for (whole in 1..3) {
                for (remainder in 1 until denominator) {
                    val source = Fraction(whole * denominator + remainder, denominator)
                    val question = FractionHardQuestionGenerator.generate(ExerciseType.TO_MIXED_NUMBER,
                        firstChoices(conversionIndex(source)))
                    assertEquals(FractionExercise.ToMixedNumber(source), question.exercise)
                    assertValid(ExerciseType.TO_MIXED_NUMBER, question)
                }
            }
        }
    }

    @Test fun suppliedRandomReproducesMixedSequencesWithoutSharedState() {
        val firstRandom = Random(123)
        val secondRandom = Random(123)
        repeat(20) { index ->
            val type = ExerciseType.values()[index % 3]
            val first = FractionHardQuestionGenerator.generate(type, firstRandom)
            val second = FractionHardQuestionGenerator.generate(type, secondRandom)
            assertEquals(first.exercise, second.exercise)
            assertEquals(first.options, second.options)
            assertValid(type, first)
        }
    }

    @Test fun correctPositionVariesForAllThreeTypes() {
        for (type in ExerciseType.values()) {
            val positions = (0 until 100).map { seed ->
                val question = FractionHardQuestionGenerator.generate(type, Random(seed))
                question.options.indexOf(question.correctAnswer)
            }.toSet()
            assertTrue(positions.all { it in 0..3 })
            assertTrue("Correct position must vary for $type", positions.size > 1)
        }
    }

    @Test(timeout = 10000) fun repeatedRandomBitsDoNotCauseRetryLoops() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        for (type in ExerciseType.values()) {
            repeat(10) { assertValid(type, FractionHardQuestionGenerator.generate(type, random)) }
        }
    }

    private fun assertArithmetic(type: ExerciseType, left: Fraction, right: Fraction, result: Fraction) {
        val question = FractionHardQuestionGenerator.generate(type,
            firstChoices(sourceIndex(left) * 105 + sourceIndex(right)))
        assertEquals(listOf(left, right), conditionFractions(question.exercise))
        assertEquals(FractionAnswer.FractionValue(result), question.correctAnswer)
        assertValid(type, question)
    }

    private fun assertValid(type: ExerciseType, question: FractionQuestion<out FractionAnswer>) {
        assertTrue(when (type) {
            ExerciseType.ADD -> question.exercise is FractionExercise.Add
            ExerciseType.SUBTRACT -> question.exercise is FractionExercise.Subtract
            ExerciseType.TO_MIXED_NUMBER -> question.exercise is FractionExercise.ToMixedNumber
        })
        val sources = conditionFractions(question.exercise)
        sources.forEach { assertTrue(it.denominator in 2..15) }
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.distinct().size)
        assertEquals(1, question.options.count { isCorrect(question.exercise, it) })
        question.options.forEachIndexed { index, answer ->
            question.options.take(index).forEach { other ->
                when (answer) {
                    is FractionAnswer.FractionValue -> {
                        assertTrue(other is FractionAnswer.FractionValue)
                        assertFalse(answer.value.isEquivalentTo((other as FractionAnswer.FractionValue).value))
                    }
                    is FractionAnswer.MixedNumber -> {
                        assertTrue(other is FractionAnswer.MixedNumber)
                        other as FractionAnswer.MixedNumber
                        assertFalse(answer.wholePart == other.wholePart &&
                            answer.fractionalPart.isEquivalentTo(other.fractionalPart))
                    }
                    else -> error("Unexpected HARD answer: $answer")
                }
            }
        }
        when (val correct = question.correctAnswer) {
            is FractionAnswer.FractionValue -> {
                sources.forEach { assertTrue(it.numerator in 1 until it.denominator) }
                assertFalse(sources[0].denominator == sources[1].denominator)
                assertTrue(correct.value > Fraction(0, 1))
                assertTrue(correct.value.denominator > 1)
                assertEquals(correct.value.reduced(), correct.value)
            }
            is FractionAnswer.MixedNumber -> {
                val source = sources.single()
                assertTrue(source.numerator > source.denominator)
                assertTrue(source.numerator % source.denominator != 0)
                assertTrue(correct.wholePart in 1..3)
                assertTrue(correct.fractionalPart > Fraction(0, 1))
                assertTrue(correct.fractionalPart < Fraction(1, 1))
                assertEquals(correct.fractionalPart.reduced(), correct.fractionalPart)
                assertTrue((Fraction(correct.wholePart, 1) + correct.fractionalPart).isEquivalentTo(source))
            }
            else -> error("Unexpected HARD correct answer: $correct")
        }
    }

    private fun conditionFractions(exercise: FractionExercise<out FractionAnswer>): List<Fraction> = when (exercise) {
        is FractionExercise.Add -> {
            assertEquals(FractionArithmeticLevel.HARD, exercise.level)
            listOf(exercise.left, exercise.right)
        }
        is FractionExercise.Subtract -> {
            assertEquals(FractionArithmeticLevel.HARD, exercise.level)
            listOf(exercise.left, exercise.right)
        }
        is FractionExercise.ToMixedNumber -> listOf(exercise.source)
        else -> error("Unexpected HARD exercise: $exercise")
    }

    private fun isCorrect(exercise: FractionExercise<out FractionAnswer>, answer: FractionAnswer): Boolean = when (exercise) {
        is FractionExercise.Add -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidHardAddition(exercise.left, exercise.right, answer.value)
        is FractionExercise.Subtract -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidHardSubtraction(exercise.left, exercise.right, answer.value)
        is FractionExercise.ToMixedNumber -> answer is FractionAnswer.MixedNumber &&
            FractionMathValidator.isValidHardConversion(exercise.source, answer.wholePart, answer.fractionalPart)
        else -> false
    }

    private fun sourceIndex(fraction: Fraction): Int =
        (fraction.denominator - 2) * (fraction.denominator - 1) / 2 + fraction.numerator - 1

    private fun conversionIndex(source: Fraction): Int =
        (2 until source.denominator).sumOf { 3 * it - 1 } + source.numerator - source.denominator - 1

    private fun firstChoices(vararg choices: Int): Random = object : Random() {
        private val delegate = Random(123)
        private var index = 0
        override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)
        override fun nextInt(until: Int): Int {
            if (index >= choices.size) return delegate.nextInt(until)
            val choice = choices[index++]
            require(choice in 0 until until)
            return choice
        }
    }
}
