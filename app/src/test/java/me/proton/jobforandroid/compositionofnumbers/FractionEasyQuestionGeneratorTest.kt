package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionEasyQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionEasyQuestionGenerator.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionEasyQuestionGeneratorTest {
    @Test(timeout = 10000) fun oneHundredSeedsForEachTypeProduceValidQuestions() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) {
                assertValid(type, FractionEasyQuestionGenerator.generate(type, Random(seed)))
            }
        }
    }

    @Test(timeout = 10000) fun allFortyFiveSourceRecordsWorkForBothTypes() {
        for (type in ExerciseType.values()) {
            for (denominator in 2..10) {
                for (numerator in 1 until denominator) {
                    val question = FractionEasyQuestionGenerator.generate(type, sourceRandom(numerator, denominator))
                    assertEquals(Fraction(numerator, denominator), sourceOf(question))
                    assertValid(type, question)
                }
            }
        }
    }

    @Test fun reducibleSourceRecordIsNotChangedBeforeReadingItsComponents() {
        val numerator = FractionEasyQuestionGenerator.generate(ExerciseType.NUMERATOR, sourceRandom(6, 8))
        val denominator = FractionEasyQuestionGenerator.generate(ExerciseType.DENOMINATOR, sourceRandom(6, 8))
        assertEquals(Fraction(6, 8), sourceOf(numerator))
        assertEquals(Fraction(6, 8), sourceOf(denominator))
        assertEquals(FractionAnswer.Integer(6), numerator.correctAnswer)
        assertEquals(FractionAnswer.Integer(8), denominator.correctAnswer)
    }

    @Test fun sameSeedReproducesEachTypeAndExactOptionOrder() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) {
                val first = FractionEasyQuestionGenerator.generate(type, Random(seed))
                val second = FractionEasyQuestionGenerator.generate(type, Random(seed))
                assertEquals(first.exercise, second.exercise)
                assertEquals(first.correctAnswer, second.correctAnswer)
                assertEquals(first.options, second.options)
            }
        }
    }

    @Test fun mixedSequenceOfRequestedTypesIsReproducibleWithoutSharedState() {
        val firstRandom = Random(123)
        val secondRandom = Random(123)
        repeat(20) { index ->
            val type = if (index % 2 == 0) ExerciseType.NUMERATOR else ExerciseType.DENOMINATOR
            val first = FractionEasyQuestionGenerator.generate(type, firstRandom)
            val second = FractionEasyQuestionGenerator.generate(type, secondRandom)
            assertEquals(first.exercise, second.exercise)
            assertEquals(first.options, second.options)
            assertValid(type, first)
        }
    }

    @Test fun correctPositionIsNotFixedForEitherType() {
        for (type in ExerciseType.values()) {
            val positions = (0 until 100).map { seed ->
                val question = FractionEasyQuestionGenerator.generate(type, Random(seed))
                question.options.indexOf(question.correctAnswer)
            }.toSet()
            assertTrue(positions.all { it in 0..3 })
            assertTrue("Position must vary for $type", positions.size > 1)
        }
    }

    @Test(timeout = 10000) fun minimumDenominatorWorksWithRepeatedRandomBitsForBothTypes() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        for (type in ExerciseType.values()) {
            repeat(10) {
                val question = FractionEasyQuestionGenerator.generate(type, random)
                assertEquals(Fraction(1, 2), sourceOf(question))
                assertValid(type, question)
            }
        }
    }

    private fun sourceOf(question: FractionQuestion<FractionAnswer.Integer>): Fraction = when (val exercise = question.exercise) {
        is FractionExercise.IdentifyNumerator -> exercise.source
        is FractionExercise.IdentifyDenominator -> exercise.source
        else -> error("Unexpected EASY exercise: $exercise")
    }

    private fun assertValid(type: ExerciseType, question: FractionQuestion<FractionAnswer.Integer>) {
        val source = sourceOf(question)
        assertTrue(source.denominator in 2..10)
        assertTrue(source.numerator in 1 until source.denominator)
        assertTrue(FractionMathValidator.isValidEasyFraction(source))
        when (type) {
            ExerciseType.NUMERATOR -> {
                assertTrue(question.exercise is FractionExercise.IdentifyNumerator)
                assertEquals(source.numerator, question.correctAnswer.value)
                assertEquals(1, question.options.count { FractionMathValidator.isCorrectEasyNumerator(source, it.value) })
            }
            ExerciseType.DENOMINATOR -> {
                assertTrue(question.exercise is FractionExercise.IdentifyDenominator)
                assertEquals(source.denominator, question.correctAnswer.value)
                assertEquals(1, question.options.count { FractionMathValidator.isCorrectEasyDenominator(source, it.value) })
            }
        }
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.map { it.value }.distinct().size)
        assertEquals(1, question.options.count { it == question.correctAnswer })
    }

    private fun sourceRandom(numerator: Int, denominator: Int): Random = object : Random() {
        private val delegate = Random(denominator * 10 + numerator)
        private var sourceDraws = 0
        override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)
        override fun nextInt(from: Int, until: Int): Int = when (sourceDraws++) {
            0 -> denominator
            1 -> numerator
            else -> delegate.nextInt(from, until)
        }
    }
}
