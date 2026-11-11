package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonBasicSessionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistance
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistanceClassifier
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDifficulty
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDifficultyClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComparisonBasicSessionGeneratorTest {
    private fun generate(level: ComparisonLevel, seed: Int) =
        ComparisonBasicSessionGenerator.generate(level, Random(seed))

    private fun assertSession(level: ComparisonLevel, questions: List<ComparisonQuestion>) {
        val perSign = when (level) {
            ComparisonLevel.TEST -> 8
            ComparisonLevel.EASY -> 15
            ComparisonLevel.NORMAL -> 12
            ComparisonLevel.HARD -> 10
        }
        assertEquals(perSign * 3, questions.size)
        assertTrue(questions.all { it.level == level && ComparisonQuestionValidator.isValid(it) })
        for (sign in ComparisonSign.entries) {
            assertEquals(perSign, questions.count { it.correctSign == sign })
        }
        val keys = questions.map(ComparisonCanonicalKey::of)
        assertEquals(questions.size, keys.toSet().size)
        val reversedKeys = questions.map { question ->
            ComparisonCanonicalKey.of(ComparisonQuestion(level, question.right, question.left))
        }
        assertEquals(keys, reversedKeys)
        for (question in questions) {
            val left = independentValue(question.left)
            val right = independentValue(question.right)
            assertEquals(left, question.leftValue.toLong())
            assertEquals(right, question.rightValue.toLong())
            val sign = when {
                left < right -> ComparisonSign.LESS
                left > right -> ComparisonSign.GREATER
                else -> ComparisonSign.EQUAL
            }
            assertEquals(sign, question.correctSign)
        }
    }

    private fun independentValue(side: ComparisonSide): Long = when (side) {
        is ComparisonSide.Number -> side.value.toLong()
        is ComparisonSide.Expression -> {
            val first = side.firstOperand.toLong()
            val second = side.secondOperand.toLong()
            when (side.operation) {
                ComparisonOperation.ADDITION -> first + second
                ComparisonOperation.SUBTRACTION -> first - second
                ComparisonOperation.MULTIPLICATION -> first * second
                ComparisonOperation.DIVISION -> {
                    assertTrue(second > 0 && first % second == 0L)
                    first / second
                }
            }
        }
    }

    private fun assertTestSession(questions: List<ComparisonQuestion>) {
        assertSession(ComparisonLevel.TEST, questions)
        assertTrue(questions.all { question ->
            question.left is ComparisonSide.Number && question.right is ComparisonSide.Number &&
                question.leftValue in 0..10 && question.rightValue in 0..10 &&
                ComparisonDistanceClassifier.classify(question) == null
        })
    }

    @Test fun testSessionIsCompleteValidBalancedAndUnique() {
        assertTestSession(generate(ComparisonLevel.TEST, 20261111))
    }

    private fun assertEasySession(questions: List<ComparisonQuestion>) {
        assertSession(ComparisonLevel.EASY, questions)
        val unequal = questions.filter { it.correctSign != ComparisonSign.EQUAL }
        assertEquals(30, unequal.size)
        assertEquals(9, unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.CLOSE })
        val medium = unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.MEDIUM }
        val far = unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.FAR }
        assertEquals(setOf(10, 11), setOf(medium, far))
        assertTrue(questions.count { it.leftValue in 10..99 && it.rightValue in 10..99 } > questions.size / 2)
        assertTrue(questions.all { it.leftValue in 0..100 && it.rightValue in 0..100 })
    }

    @Test fun easySessionHasExactDistancesAndMostlyTwoDigitNumbers() {
        assertEasySession(generate(ComparisonLevel.EASY, 20261111))
    }

    @Test fun sameSeedReproducesCompleteSessionsAndDifferentSeedsChangeThem() {
        for (level in ComparisonLevel.entries) {
            assertEquals(generate(level, 42), generate(level, 42))
            assertNotEquals(generate(level, 42), generate(level, 43))
        }
    }

    private fun assertNormalSession(questions: List<ComparisonQuestion>) {
        assertSession(ComparisonLevel.NORMAL, questions)
        assertTrue(questions.any { it.left is ComparisonSide.Expression })
        assertTrue(questions.any { it.right is ComparisonSide.Expression })
        val expressions = questions.map { question ->
            val expression = (question.left as? ComparisonSide.Expression)
                ?: question.right as ComparisonSide.Expression
            val number = (question.left as? ComparisonSide.Number)
                ?: question.right as ComparisonSide.Number
            val allowedNumber = if (expression.operation == ComparisonOperation.DIVISION) 2..18 else 2..100
            assertTrue(number.value in allowedNumber)
            assertTrue(expression.firstOperand > 0 && expression.secondOperand > 0 && expression.value > 0)
            when (expression.operation) {
                ComparisonOperation.ADDITION -> assertTrue(expression.value in 20..100)
                ComparisonOperation.SUBTRACTION ->
                    assertTrue(expression.firstOperand <= 100 && expression.value >= 2)
                ComparisonOperation.MULTIPLICATION ->
                    assertTrue(expression.firstOperand in 2..9 && expression.secondOperand in 2..9)
                ComparisonOperation.DIVISION ->
                    assertTrue(expression.secondOperand in 2..9 && expression.value in 2..9 &&
                        expression.firstOperand % expression.secondOperand == 0)
            }
            expression
        }
        for (operation in ComparisonOperation.entries) {
            assertEquals(9, expressions.count { it.operation == operation })
        }
        assertEquals(22, expressions.count {
            ComparisonDifficultyClassifier.classify(ComparisonLevel.NORMAL, it) == ComparisonDifficulty.COMPLEX
        })
        assertEquals(14, expressions.count {
            ComparisonDifficultyClassifier.classify(ComparisonLevel.NORMAL, it) == ComparisonDifficulty.ORDINARY
        })
        val unequal = questions.filter { it.correctSign != ComparisonSign.EQUAL }
        assertEquals(24, unequal.size)
        assertEquals(7, unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.CLOSE })
        assertEquals(setOf(8, 9), setOf(
            unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.MEDIUM },
            unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.FAR }
        ))
        assertTrue(questions.filter { it.correctSign == ComparisonSign.EQUAL }
            .all { ComparisonDistanceClassifier.classify(it) == null })
    }

    @Test fun normalSessionMeetsAllQuotasAndRanges() {
        assertNormalSession(generate(ComparisonLevel.NORMAL, 20261111))
    }

    private fun assertHardSession(questions: List<ComparisonQuestion>) {
        assertSession(ComparisonLevel.HARD, questions)
        val operations = questions.map { question ->
            val left = question.left as ComparisonSide.Expression
            val right = question.right as ComparisonSide.Expression
            val leftArithmetic = left.operation == ComparisonOperation.ADDITION ||
                left.operation == ComparisonOperation.SUBTRACTION
            val rightArithmetic = right.operation == ComparisonOperation.ADDITION ||
                right.operation == ComparisonOperation.SUBTRACTION
            for (expression in listOf(left, right)) {
                assertTrue(expression.firstOperand > 0 && expression.secondOperand > 0 && expression.value > 0)
                when (expression.operation) {
                    ComparisonOperation.ADDITION -> assertTrue(expression.value <= 1000)
                    ComparisonOperation.SUBTRACTION -> assertTrue(expression.firstOperand <= 1000)
                    ComparisonOperation.MULTIPLICATION -> assertTrue(
                        expression.firstOperand in 2..10 && expression.secondOperand in 2..10)
                    ComparisonOperation.DIVISION -> assertTrue(
                        expression.secondOperand in 2..10 && expression.value in 2..10 &&
                            expression.firstOperand % expression.secondOperand == 0)
                }
            }
            if (leftArithmetic && rightArithmetic) {
                for (expression in listOf(left, right)) {
                    when (expression.operation) {
                        ComparisonOperation.ADDITION -> assertTrue(
                            (expression.firstOperand in 100..999 || expression.secondOperand in 100..999) &&
                                expression.value in 100..1000)
                        ComparisonOperation.SUBTRACTION -> assertTrue(
                            expression.firstOperand in 100..1000 && expression.value in 100..999)
                        else -> error("Arithmetic group contains a tabular operation")
                    }
                }
            } else if (leftArithmetic != rightArithmetic) {
                val arithmetic = if (leftArithmetic) left else right
                val tabular = if (leftArithmetic) right else left
                val range = if (tabular.operation == ComparisonOperation.MULTIPLICATION) 2..100 else 2..20
                assertTrue(arithmetic.value in range)
            }
            when {
                leftArithmetic && rightArithmetic -> "ARITHMETIC"
                !leftArithmetic && !rightArithmetic -> "TABULAR"
                else -> "MIXED"
            }
        }
        assertEquals(10, operations.count { it == "ARITHMETIC" })
        assertEquals(10, operations.count { it == "TABULAR" })
        assertEquals(10, operations.count { it == "MIXED" })
        val expressions = questions.flatMap { listOf(
            it.left as ComparisonSide.Expression,
            it.right as ComparisonSide.Expression
        ) }
        assertEquals(60, expressions.size)
        assertEquals(42, expressions.count {
            ComparisonDifficultyClassifier.classify(ComparisonLevel.HARD, it) == ComparisonDifficulty.COMPLEX
        })
        assertEquals(18, expressions.count {
            ComparisonDifficultyClassifier.classify(ComparisonLevel.HARD, it) == ComparisonDifficulty.ORDINARY
        })
        val unequal = questions.filter { it.correctSign != ComparisonSign.EQUAL }
        assertEquals(20, unequal.size)
        assertEquals(6, unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.CLOSE })
        assertEquals(7, unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.MEDIUM })
        assertEquals(7, unequal.count { ComparisonDistanceClassifier.classify(it) == ComparisonDistance.FAR })
        assertTrue(questions.filter { it.correctSign == ComparisonSign.EQUAL }
            .all { ComparisonDistanceClassifier.classify(it) == null })
    }

    @Test fun hardSessionMeetsAllQuotasAndRanges() {
        assertHardSession(generate(ComparisonLevel.HARD, 20261111))
    }

    private fun assertHundredSeeds(level: ComparisonLevel) {
        for (seed in 0 until 100) {
            try {
                val questions = generate(level, seed)
                when (level) {
                    ComparisonLevel.TEST -> assertTestSession(questions)
                    ComparisonLevel.EASY -> assertEasySession(questions)
                    ComparisonLevel.NORMAL -> assertNormalSession(questions)
                    ComparisonLevel.HARD -> assertHardSession(questions)
                }
            } catch (failure: AssertionError) {
                throw AssertionError("level=$level seed=$seed: ${failure.message}", failure)
            } catch (failure: RuntimeException) {
                throw AssertionError("level=$level seed=$seed: ${failure.message}", failure)
            }
        }
    }

    @Test(timeout = 120000) fun testGeneratesOneHundredCompleteSessions() =
        assertHundredSeeds(ComparisonLevel.TEST)

    @Test(timeout = 120000) fun easyGeneratesOneHundredCompleteSessions() =
        assertHundredSeeds(ComparisonLevel.EASY)

    @Test(timeout = 180000) fun normalGeneratesOneHundredCompleteSessions() =
        assertHundredSeeds(ComparisonLevel.NORMAL)

    @Test(timeout = 180000) fun hardGeneratesOneHundredCompleteSessions() =
        assertHundredSeeds(ComparisonLevel.HARD)
}
