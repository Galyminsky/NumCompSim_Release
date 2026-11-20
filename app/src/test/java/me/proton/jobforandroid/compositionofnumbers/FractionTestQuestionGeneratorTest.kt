package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionTestQuestionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionTestQuestionGeneratorTest {
    @Test(timeout = 10000) fun oneHundredSeedsProduceValidQuestionsWithoutFailure() {
        for (seed in 0 until 100) {
            assertValid(FractionTestQuestionGenerator.generate(Random(seed)))
        }
    }

    @Test(timeout = 10000) fun everyAllowedPictureCanProduceFourDistinctOptions() {
        for (total in 2..7) {
            for (shaded in 1 until total) {
                // Control only the two source draws; candidate and option shuffles remain seeded.
                val random = object : Random() {
                    private val delegate = Random(total * 10 + shaded)
                    private var sourceDraws = 0
                    override fun nextBits(bitCount: Int): Int = delegate.nextBits(bitCount)
                    override fun nextInt(from: Int, until: Int): Int = when (sourceDraws++) {
                        0 -> total
                        1 -> shaded
                        else -> delegate.nextInt(from, until)
                    }
                }
                val question = FractionTestQuestionGenerator.generate(random)
                val exercise = question.exercise as FractionExercise.IdentifyFraction
                assertEquals(total, exercise.totalParts)
                assertEquals(shaded, exercise.shadedParts)
                assertValid(question)
            }
        }
    }

    @Test fun sameSeedReproducesConditionsAndExactOptionOrder() {
        for (seed in 0 until 100) {
            val first = FractionTestQuestionGenerator.generate(Random(seed))
            val second = FractionTestQuestionGenerator.generate(Random(seed))
            assertEquals(first.exercise, second.exercise)
            assertEquals(first.correctAnswer, second.correctAnswer)
            assertEquals(first.options, second.options)
        }
    }

    @Test fun suppliedRandomReproducesASequenceWithoutSharedGeneratorState() {
        val firstRandom = Random(123)
        val secondRandom = Random(123)
        repeat(20) {
            val first = FractionTestQuestionGenerator.generate(firstRandom)
            val second = FractionTestQuestionGenerator.generate(secondRandom)
            assertEquals(first.exercise, second.exercise)
            assertEquals(first.options, second.options)
            assertValid(first)
        }
    }

    @Test fun correctPositionIsNotFixedAcrossSeeds() {
        val positions = (0 until 100).map { seed ->
            val question = FractionTestQuestionGenerator.generate(Random(seed))
            question.options.indexOfFirst { it.value.isEquivalentTo(question.correctAnswer.value) }
        }.toSet()
        assertTrue(positions.all { it in 0..3 })
        assertTrue("Correct position must vary", positions.size > 1)
    }

    @Test(timeout = 10000) fun repeatedRandomBitsDoNotCauseCandidateRetryLoops() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        repeat(10) {
            val question = FractionTestQuestionGenerator.generate(random)
            assertEquals(FractionExercise.IdentifyFraction(2, 1), question.exercise)
            assertValid(question)
        }
    }

    private fun assertValid(question: FractionQuestion<FractionAnswer.FractionValue>) {
        val exercise = question.exercise as FractionExercise.IdentifyFraction
        assertTrue(exercise.totalParts in 2..7)
        assertTrue(exercise.shadedParts in 1 until exercise.totalParts)
        assertTrue(FractionMathValidator.isValidTestParts(exercise.shadedParts, exercise.totalParts))
        assertEquals(Fraction(exercise.shadedParts, exercise.totalParts), question.correctAnswer.value)
        assertEquals(4, question.options.size)
        assertEquals(4, question.options.distinct().size)
        assertEquals(1, question.options.count {
            FractionMathValidator.isCorrectTestAnswer(exercise.shadedParts, exercise.totalParts, it.value)
        })
        question.options.forEachIndexed { index, answer ->
            for (other in question.options.take(index)) {
                assertFalse(answer.value.isEquivalentTo(other.value))
            }
        }
    }
}
