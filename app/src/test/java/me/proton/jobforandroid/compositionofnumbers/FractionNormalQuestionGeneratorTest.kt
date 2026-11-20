package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionNormalQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionNormalQuestionGenerator.ExerciseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionNormalQuestionGeneratorTest {
    @Test(timeout = 10000) fun oneHundredSeedsForEachTypeProduceValidQuestions() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) assertValid(type, FractionNormalQuestionGenerator.generate(type, Random(seed)))
        }
    }

    @Test fun sameSeedReproducesConditionsAndExactOptionOrderForEveryType() {
        for (type in ExerciseType.values()) {
            for (seed in 0 until 100) {
                val first = FractionNormalQuestionGenerator.generate(type, Random(seed))
                val second = FractionNormalQuestionGenerator.generate(type, Random(seed))
                assertEquals(first.exercise, second.exercise)
                assertEquals(first.correctAnswer, second.correctAnswer)
                assertEquals(first.options, second.options)
            }
        }
    }

    @Test fun comparisonSupportsAllSignsIncludingDifferentEquivalentRecords() {
        val cases = listOf(
            Triple(Fraction(1, 3), Fraction(1, 2), FractionAnswer.ComparisonValue.LESS),
            Triple(Fraction(2, 4), Fraction(1, 2), FractionAnswer.ComparisonValue.EQUAL),
            Triple(Fraction(1, 2), Fraction(1, 3), FractionAnswer.ComparisonValue.GREATER),
            Triple(Fraction(11, 12), Fraction(1, 2), FractionAnswer.ComparisonValue.GREATER)
        )
        for ((left, right, sign) in cases) {
            val question = FractionNormalQuestionGenerator.generate(ExerciseType.COMPARE,
                firstChoices(sourceIndex(left), sourceIndex(right)))
            assertEquals(FractionExercise.Compare(left, right), question.exercise)
            assertEquals(FractionAnswer.Sign(sign), question.correctAnswer)
            assertEquals(FractionAnswer.ComparisonValue.values().toSet(),
                question.options.map { (it as FractionAnswer.Sign).value }.toSet())
            assertValid(ExerciseType.COMPARE, question)
        }
    }

    @Test fun reductionPreservesSourceAndProducesFullyReducedAnswer() {
        for (source in listOf(Fraction(2, 4), Fraction(6, 8), Fraction(10, 12))) {
            val question = FractionNormalQuestionGenerator.generate(ExerciseType.REDUCE, firstChoices(sourceIndex(source)))
            assertEquals(FractionExercise.Reduce(source), question.exercise)
            assertEquals(FractionAnswer.FractionValue(source.reduced()), question.correctAnswer)
            assertValid(ExerciseType.REDUCE, question)
        }
    }

    @Test fun arithmeticReachesSameDifferentAndBoundaryDenominatorsIncludingLcm24() {
        assertArithmetic(ExerciseType.ADD, Fraction(1, 4), Fraction(1, 4), Fraction(1, 2))
        assertArithmetic(ExerciseType.SUBTRACT, Fraction(3, 4), Fraction(1, 4), Fraction(1, 2))
        assertArithmetic(ExerciseType.ADD, Fraction(1, 3), Fraction(1, 8), Fraction(11, 24))
        assertArithmetic(ExerciseType.SUBTRACT, Fraction(1, 3), Fraction(1, 8), Fraction(5, 24))
        assertArithmetic(ExerciseType.ADD, Fraction(1, 2), Fraction(1, 12), Fraction(7, 12))
        assertArithmetic(ExerciseType.SUBTRACT, Fraction(11, 12), Fraction(1, 12), Fraction(5, 6))
        assertArithmetic(ExerciseType.ADD, Fraction(1, 12), Fraction(1, 12), Fraction(1, 6))
    }

    @Test fun boundedSearchSkipsIntegerZeroNegativeAndExcessiveLcmCandidates() {
        val cases = listOf(
            Triple(ExerciseType.ADD, Fraction(1, 2), Fraction(1, 2)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 2), Fraction(1, 2)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 3), Fraction(1, 2)),
            Triple(ExerciseType.ADD, Fraction(1, 5), Fraction(1, 6)),
            Triple(ExerciseType.SUBTRACT, Fraction(1, 5), Fraction(1, 6))
        )
        for ((type, left, right) in cases) {
            val question = FractionNormalQuestionGenerator.generate(type,
                firstChoices(sourceIndex(left) * 66 + sourceIndex(right)))
            assertFalse(conditionFractions(question.exercise) == listOf(left, right))
            assertValid(type, question)
        }
    }

    @Test fun sequenceOfRequestedTypesIsReproducibleWithoutSharedState() {
        val firstRandom = Random(123)
        val secondRandom = Random(123)
        repeat(20) { index ->
            val type = ExerciseType.values()[index % 4]
            val first = FractionNormalQuestionGenerator.generate(type, firstRandom)
            val second = FractionNormalQuestionGenerator.generate(type, secondRandom)
            assertEquals(first.exercise, second.exercise)
            assertEquals(first.options, second.options)
            assertValid(type, first)
        }
    }

    @Test fun correctPositionVariesForAllFourTypes() {
        for (type in ExerciseType.values()) {
            val positions = (0 until 100).map { seed ->
                val question = FractionNormalQuestionGenerator.generate(type, Random(seed))
                question.options.indexOf(question.correctAnswer)
            }.toSet()
            assertTrue(positions.all { it in 0 until (if (type == ExerciseType.COMPARE) 3 else 4) })
            assertTrue("Correct position must vary for $type", positions.size > 1)
        }
    }

    @Test(timeout = 10000) fun repeatedRandomBitsStillTerminateForEveryType() {
        val random = object : Random() {
            override fun nextBits(bitCount: Int): Int = 0
        }
        for (type in ExerciseType.values()) {
            repeat(10) { assertValid(type, FractionNormalQuestionGenerator.generate(type, random)) }
        }
    }

    private fun assertArithmetic(type: ExerciseType, left: Fraction, right: Fraction, result: Fraction) {
        val question = FractionNormalQuestionGenerator.generate(type,
            firstChoices(sourceIndex(left) * 66 + sourceIndex(right)))
        assertEquals(listOf(left, right), conditionFractions(question.exercise))
        assertEquals(FractionAnswer.FractionValue(result), question.correctAnswer)
        assertValid(type, question)
    }

    private fun assertValid(type: ExerciseType, question: FractionQuestion<out FractionAnswer>) {
        assertTrue(when (type) {
            ExerciseType.COMPARE -> question.exercise is FractionExercise.Compare
            ExerciseType.REDUCE -> question.exercise is FractionExercise.Reduce
            ExerciseType.ADD -> question.exercise is FractionExercise.Add
            ExerciseType.SUBTRACT -> question.exercise is FractionExercise.Subtract
        })
        conditionFractions(question.exercise).forEach { source ->
            assertTrue(source.denominator in 2..12)
            assertTrue(source.numerator in 1 until source.denominator)
        }
        assertEquals(if (type == ExerciseType.COMPARE) 3 else 4, question.options.size)
        assertEquals(question.options.size, question.options.distinct().size)
        assertEquals(1, question.options.count { isCorrect(question.exercise, it) })
        question.options.forEachIndexed { index, answer ->
            question.options.take(index).forEach { other ->
                when (answer) {
                    is FractionAnswer.Sign -> assertFalse(answer == other)
                    is FractionAnswer.FractionValue -> {
                        assertTrue(other is FractionAnswer.FractionValue)
                        assertFalse(answer.value.isEquivalentTo((other as FractionAnswer.FractionValue).value))
                    }
                    else -> error("Unexpected NORMAL answer: $answer")
                }
            }
        }
        val correct = question.correctAnswer
        if (correct is FractionAnswer.FractionValue) {
            assertTrue(correct.value > Fraction(0, 1))
            assertTrue(correct.value < Fraction(1, 1))
            assertEquals(correct.value.reduced(), correct.value)
        }
    }

    private fun conditionFractions(exercise: FractionExercise<out FractionAnswer>): List<Fraction> = when (exercise) {
        is FractionExercise.Compare -> listOf(exercise.left, exercise.right)
        is FractionExercise.Reduce -> listOf(exercise.source)
        is FractionExercise.Add -> {
            assertEquals(FractionArithmeticLevel.NORMAL, exercise.level)
            listOf(exercise.left, exercise.right)
        }
        is FractionExercise.Subtract -> {
            assertEquals(FractionArithmeticLevel.NORMAL, exercise.level)
            listOf(exercise.left, exercise.right)
        }
        else -> error("Unexpected NORMAL exercise: $exercise")
    }

    private fun isCorrect(exercise: FractionExercise<out FractionAnswer>, answer: FractionAnswer): Boolean = when (exercise) {
        is FractionExercise.Compare -> answer is FractionAnswer.Sign &&
            FractionMathValidator.isCorrectNormalComparison(exercise.left, exercise.right, answer.value.comparison)
        is FractionExercise.Reduce -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidNormalReduction(exercise.source, answer.value)
        is FractionExercise.Add -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidNormalAddition(exercise.left, exercise.right, answer.value)
        is FractionExercise.Subtract -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidNormalSubtraction(exercise.left, exercise.right, answer.value)
        else -> false
    }

    // Source ordering is denominator, then numerator; the test controls the search start, not its result.
    private fun sourceIndex(fraction: Fraction): Int =
        (fraction.denominator - 2) * (fraction.denominator - 1) / 2 + fraction.numerator - 1

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
