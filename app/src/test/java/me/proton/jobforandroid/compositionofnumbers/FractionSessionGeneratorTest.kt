package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionArithmeticLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionEasyQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionHardQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionNormalQuestionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionTestQuestionGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionSessionGeneratorTest {
    @Test(timeout = 30000) fun oneHundredSeedsForEveryLevelProduceCompleteValidUniqueSessions() {
        for (level in Level.values()) {
            for (seed in 0 until 100) assertSession(level, FractionSessionGenerator.generate(level, Random(seed)))
        }
    }

    @Test(timeout = 30000) fun sameSeedsReproduceConditionsAndEveryOptionOrder() {
        for (level in Level.values()) {
            for (seed in 0 until 100) {
                val first = FractionSessionGenerator.generate(level, Random(seed))
                val second = FractionSessionGenerator.generate(level, Random(seed))
                assertEquals(first.map { it.exercise }, second.map { it.exercise })
                assertEquals(first.map { it.options }, second.map { it.options })
            }
        }
    }

    @Test(timeout = 30000) fun constantRandomStillBuildsFullSessionsWithoutRelaxingRules() {
        for (level in Level.values()) {
            repeat(3) { assertSession(level, FractionSessionGenerator.generate(level, zeroRandom())) }
        }
    }

    @Test fun noHistoryLeaksBetweenSessionsOrLevels() {
        for (level in Level.values()) {
            val first = FractionSessionGenerator.generate(level, Random(17))
            FractionSessionGenerator.generate(Level.HARD, Random(99))
            val second = FractionSessionGenerator.generate(level, Random(17))
            assertEquals(first.map { it.exercise }, second.map { it.exercise })
            assertEquals(first.map { it.options }, second.map { it.options })
        }
    }

    @Test fun mixedTypeSessionsDoNotRetainOneFixedBlockOrder() {
        for (level in listOf(Level.EASY, Level.NORMAL, Level.HARD)) {
            val orders = (0 until 20).map { seed ->
                FractionSessionGenerator.generate(level, Random(seed)).map { it.exercise::class }
            }.toSet()
            assertTrue("Exercise order must vary for $level", orders.size > 1)
        }
    }

    @Test fun canonicalKeysRespectRawEasyRecordsButMathematicalEquivalenceElsewhere() {
        assertEquals(key(FractionExercise.IdentifyFraction(4, 2)), key(FractionExercise.IdentifyFraction(6, 3)))
        assertEquals(key(FractionExercise.IdentifyNumerator(Fraction(6, 8))),
            key(FractionExercise.IdentifyDenominator(Fraction(6, 8))))
        assertNotEquals(key(FractionExercise.IdentifyNumerator(Fraction(6, 8))),
            key(FractionExercise.IdentifyDenominator(Fraction(3, 4))))
        assertEquals(key(FractionExercise.Reduce(Fraction(2, 4))), key(FractionExercise.Reduce(Fraction(3, 6))))
        assertEquals(key(FractionExercise.ToMixedNumber(Fraction(14, 4))),
            key(FractionExercise.ToMixedNumber(Fraction(7, 2))))
    }

    @Test fun keysIgnoreAdditionAndComparisonSwapsButPreserveTypeAndOrderedSubtraction() {
        assertEquals(key(FractionExercise.Compare(Fraction(1, 3), Fraction(1, 2))),
            key(FractionExercise.Compare(Fraction(2, 4), Fraction(2, 6))))
        assertEquals(key(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 4), Fraction(1, 3))),
            key(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(2, 6), Fraction(2, 8))))
        assertNotEquals(key(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 3), Fraction(1, 6))),
            key(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 4), Fraction(1, 4))))
        assertEquals(key(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(3, 4), Fraction(1, 2))),
            key(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(6, 8), Fraction(2, 4))))
        assertNotEquals(key(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(3, 4), Fraction(1, 2))),
            key(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(1, 2), Fraction(1, 4))))
        assertNotEquals(key(FractionExercise.Add(FractionArithmeticLevel.NORMAL, Fraction(1, 4), Fraction(1, 4))),
            key(FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(3, 4), Fraction(1, 4))))
        // A reversed positive subtraction is not a valid exercise under the approved constraints.
        assertThrows(IllegalArgumentException::class.java) {
            FractionExercise.Subtract(FractionArithmeticLevel.NORMAL, Fraction(1, 2), Fraction(3, 4))
        }
    }

    @Test(timeout = 30000) fun exhaustedCandidateFiltersFailExplicitlyForEverySingleGeneratorType() {
        assertThrows(IllegalStateException::class.java) { FractionTestQuestionGenerator.generate(zeroRandom()) { false } }
        for (type in FractionEasyQuestionGenerator.ExerciseType.values()) {
            assertThrows(IllegalStateException::class.java) {
                FractionEasyQuestionGenerator.generate(type, zeroRandom()) { false }
            }
        }
        for (type in FractionNormalQuestionGenerator.ExerciseType.values()) {
            assertThrows(IllegalStateException::class.java) {
                FractionNormalQuestionGenerator.generate(type, zeroRandom()) { false }
            }
        }
        for (type in FractionHardQuestionGenerator.ExerciseType.values()) {
            assertThrows(IllegalStateException::class.java) {
                FractionHardQuestionGenerator.generate(type, zeroRandom()) { false }
            }
        }
    }

    private fun assertSession(level: Level, questions: List<FractionQuestion<out FractionAnswer>>) {
        val exercises = questions.map { it.exercise }
        when (level) {
            Level.TEST -> {
                assertEquals(8, questions.size)
                assertTrue(exercises.all { it is FractionExercise.IdentifyFraction })
            }
            Level.EASY -> {
                assertEquals(15, questions.size)
                assertEquals(8, exercises.count { it is FractionExercise.IdentifyNumerator })
                assertEquals(7, exercises.count { it is FractionExercise.IdentifyDenominator })
            }
            Level.NORMAL -> {
                assertEquals(12, questions.size)
                assertEquals(3, exercises.count { it is FractionExercise.Compare })
                assertEquals(3, exercises.count { it is FractionExercise.Reduce })
                assertEquals(3, exercises.count { it is FractionExercise.Add })
                assertEquals(3, exercises.count { it is FractionExercise.Subtract })
                for (sign in FractionAnswer.ComparisonValue.values()) {
                    assertEquals(1, exercises.filterIsInstance<FractionExercise.Compare>()
                        .count { it.correctAnswer.value == sign })
                }
            }
            Level.HARD -> {
                assertEquals(10, questions.size)
                assertEquals(3, exercises.count { it is FractionExercise.Add })
                assertEquals(3, exercises.count { it is FractionExercise.Subtract })
                assertEquals(4, exercises.count { it is FractionExercise.ToMixedNumber })
            }
        }
        questions.forEach { assertQuestion(level, it) }
        exercises.forEachIndexed { index, exercise ->
            exercises.take(index).forEach { other ->
                // Deliberately independent of the production canonical-key implementation.
                assertFalse("Repeated conditions: $exercise and $other", sameTask(exercise, other))
            }
        }
    }

    private fun assertQuestion(level: Level, question: FractionQuestion<out FractionAnswer>) {
        val correct = question.correctAnswer
        assertEquals(if (question.exercise is FractionExercise.Compare) 3 else 4, question.options.size)
        assertEquals(1, question.options.count { isCorrect(question.exercise, it) })
        question.options.forEachIndexed { index, option ->
            assertEquals(correct::class, option::class)
            question.options.take(index).forEach { other -> assertFalse(sameAnswer(option, other)) }
        }
        when (val exercise = question.exercise) {
            is FractionExercise.Add -> assertEquals(
                if (level == Level.NORMAL) FractionArithmeticLevel.NORMAL else FractionArithmeticLevel.HARD, exercise.level)
            is FractionExercise.Subtract -> assertEquals(
                if (level == Level.NORMAL) FractionArithmeticLevel.NORMAL else FractionArithmeticLevel.HARD, exercise.level)
            else -> Unit
        }
    }

    private fun sameTask(first: FractionExercise<out FractionAnswer>, second: FractionExercise<out FractionAnswer>): Boolean {
        if (isEasy(first) && isEasy(second)) return easySource(first) == easySource(second)
        return when (first) {
            is FractionExercise.IdentifyFraction -> second is FractionExercise.IdentifyFraction &&
                Fraction(first.shadedParts, first.totalParts).isEquivalentTo(Fraction(second.shadedParts, second.totalParts))
            is FractionExercise.Compare -> second is FractionExercise.Compare &&
                sameUnorderedOperands(first.left, first.right, second.left, second.right)
            is FractionExercise.Reduce -> second is FractionExercise.Reduce && first.source.isEquivalentTo(second.source)
            is FractionExercise.Add -> second is FractionExercise.Add && first.level == second.level &&
                sameUnorderedOperands(first.left, first.right, second.left, second.right)
            is FractionExercise.Subtract -> second is FractionExercise.Subtract && first.level == second.level &&
                first.left.isEquivalentTo(second.left) && first.right.isEquivalentTo(second.right)
            is FractionExercise.ToMixedNumber -> second is FractionExercise.ToMixedNumber &&
                first.source.isEquivalentTo(second.source)
            else -> false
        }
    }

    private fun sameUnorderedOperands(a: Fraction, b: Fraction, c: Fraction, d: Fraction): Boolean =
        (a.isEquivalentTo(c) && b.isEquivalentTo(d)) || (a.isEquivalentTo(d) && b.isEquivalentTo(c))

    private fun isEasy(exercise: FractionExercise<out FractionAnswer>): Boolean =
        exercise is FractionExercise.IdentifyNumerator || exercise is FractionExercise.IdentifyDenominator

    private fun easySource(exercise: FractionExercise<out FractionAnswer>): Fraction = when (exercise) {
        is FractionExercise.IdentifyNumerator -> exercise.source
        is FractionExercise.IdentifyDenominator -> exercise.source
        else -> error("Not an EASY exercise")
    }

    private fun sameAnswer(first: FractionAnswer, second: FractionAnswer): Boolean = when (first) {
        is FractionAnswer.Integer -> second is FractionAnswer.Integer && first.value == second.value
        is FractionAnswer.FractionValue -> second is FractionAnswer.FractionValue && first.value.isEquivalentTo(second.value)
        is FractionAnswer.MixedNumber -> second is FractionAnswer.MixedNumber && first.wholePart == second.wholePart &&
            first.fractionalPart.isEquivalentTo(second.fractionalPart)
        is FractionAnswer.Sign -> second is FractionAnswer.Sign && first.value == second.value
    }

    private fun isCorrect(exercise: FractionExercise<out FractionAnswer>, answer: FractionAnswer): Boolean = when (exercise) {
        is FractionExercise.IdentifyFraction -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isCorrectTestAnswer(exercise.shadedParts, exercise.totalParts, answer.value)
        is FractionExercise.IdentifyNumerator -> answer is FractionAnswer.Integer &&
            FractionMathValidator.isCorrectEasyNumerator(exercise.source, answer.value)
        is FractionExercise.IdentifyDenominator -> answer is FractionAnswer.Integer &&
            FractionMathValidator.isCorrectEasyDenominator(exercise.source, answer.value)
        is FractionExercise.Compare -> answer is FractionAnswer.Sign &&
            FractionMathValidator.isCorrectNormalComparison(exercise.left, exercise.right, answer.value.comparison)
        is FractionExercise.Reduce -> answer is FractionAnswer.FractionValue &&
            FractionMathValidator.isValidNormalReduction(exercise.source, answer.value)
        is FractionExercise.Add -> answer is FractionAnswer.FractionValue && when (exercise.level) {
            FractionArithmeticLevel.NORMAL -> FractionMathValidator.isValidNormalAddition(exercise.left, exercise.right, answer.value)
            FractionArithmeticLevel.HARD -> FractionMathValidator.isValidHardAddition(exercise.left, exercise.right, answer.value)
        }
        is FractionExercise.Subtract -> answer is FractionAnswer.FractionValue && when (exercise.level) {
            FractionArithmeticLevel.NORMAL -> FractionMathValidator.isValidNormalSubtraction(exercise.left, exercise.right, answer.value)
            FractionArithmeticLevel.HARD -> FractionMathValidator.isValidHardSubtraction(exercise.left, exercise.right, answer.value)
        }
        is FractionExercise.ToMixedNumber -> answer is FractionAnswer.MixedNumber &&
            FractionMathValidator.isValidHardConversion(exercise.source, answer.wholePart, answer.fractionalPart)
    }

    private fun key(exercise: FractionExercise<out FractionAnswer>): String = FractionCanonicalKey.of(exercise)

    private fun zeroRandom(): Random = object : Random() {
        override fun nextBits(bitCount: Int): Int = 0
    }
}
