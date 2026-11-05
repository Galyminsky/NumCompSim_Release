package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GenerateQuestionUseCase
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GetGameSettingsUseCase
import me.proton.jobforandroid.compositionofnumbers.presentation.calculatePercentOfRightAnswers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryTest {
    private val getSettings = GetGameSettingsUseCase(GameRepositoryImpl)
    private val generateQuestion = GenerateQuestionUseCase(GameRepositoryImpl)

    @Test
    fun gameResultRetainsOperationAndDefaultsToAddition() {
        for (level in Level.entries) {
            val settings = getSettings(level)
            val addition = GameResult(false, 0, 0, settings)
            val subtraction = GameResult(true, 3, 3, settings, Question.Operation.SUBTRACTION)
            val multiplication = GameResult(true, 3, 3, settings, Question.Operation.MULTIPLICATION)
            assertEquals(Question.Operation.ADDITION, addition.operation)
            assertEquals(Question.Operation.SUBTRACTION, subtraction.operation)
            assertEquals(Question.Operation.MULTIPLICATION, multiplication.operation)
            assertEquals(settings, addition.gameSettings)
            assertEquals(settings, subtraction.gameSettings)
            assertEquals(settings, multiplication.gameSettings)
            assertEquals(3, subtraction.countOfRightAnswers)
            assertEquals(3, multiplication.countOfRightAnswers)
        }
    }

    @Test
    fun questionComputesCorrectAnswerAtRangeBoundaries() {
        assertEquals(0, Question(0, 0, listOf(0, 1, 2, 3, 4, 5)).rightAnswer)
        assertEquals(10, Question(10, 0, listOf(10, 0, 1, 2, 3, 4)).rightAnswer)
        assertEquals(0, Question(10, 10, listOf(0, 1, 2, 3, 4, 5)).rightAnswer)
        val addition = Question(10, 3, listOf(7))
        assertEquals(Question.Operation.ADDITION, addition.operation)
        assertEquals(Question.UnknownPosition.SECOND_OPERAND, addition.unknownPosition)
        assertEquals(3, addition.firstOperand)
        assertEquals(7, addition.secondOperand)
        assertEquals(10, addition.result)
        val missingResult = Question(10, 3, listOf(7), Question.Operation.SUBTRACTION,
            Question.UnknownPosition.RESULT)
        assertEquals(10, missingResult.firstOperand)
        assertEquals(3, missingResult.secondOperand)
        assertEquals(7, missingResult.result)
        assertEquals(7, missingResult.rightAnswer)
        val missingSubtrahend = Question(10, 3, listOf(7), Question.Operation.SUBTRACTION,
            Question.UnknownPosition.SECOND_OPERAND)
        assertEquals(10, missingSubtrahend.firstOperand)
        assertEquals(7, missingSubtrahend.secondOperand)
        assertEquals(3, missingSubtrahend.result)
        assertEquals(7, missingSubtrahend.rightAnswer)
    }

    @Test
    fun generateUseCasePassesEachLevelMaximumAndRequestsSixOptions() {
        val calls = mutableListOf<Pair<Int, Int>>()
        val repository = object : GameRepository {
            override fun getGameSettings(level: Level): GameSettings = GameRepositoryImpl.getGameSettings(level)
            override fun generateQuestion(maxSumValue: Int, countOfOptions: Int): Question {
                calls += maxSumValue to countOfOptions
                return Question(0, 0, listOf(0, 1, 2, 3, 4, 5))
            }
            override fun generateSubtractionQuestion(maxValue: Int, countOfOptions: Int): Question {
                calls += maxValue to countOfOptions
                return Question(0, 0, listOf(0, 1, 2, 3, 4, 5),
                    Question.Operation.SUBTRACTION, Question.UnknownPosition.RESULT)
            }
            override fun generateMultiplicationQuestion(level: Level, countOfOptions: Int): Question {
                calls += level.ordinal to countOfOptions
                return Question(1, 1, listOf(1, 0, 2, 3, 4, 5),
                    Question.Operation.MULTIPLICATION, Question.UnknownPosition.RESULT, 1)
            }
        }
        val useCase = GenerateQuestionUseCase(repository)
        for (level in Level.entries) {
            assertEquals(6, useCase(getSettings(level).maxSumValue).options.size)
        }
        assertEquals(listOf(10 to 6, 10 to 6, 20 to 6, 30 to 6), calls)
        calls.clear()
        for (level in Level.entries) {
            assertEquals(Question.Operation.SUBTRACTION, useCase.generateSubtraction(level).operation)
        }
        assertEquals(listOf(10 to 6, 10 to 6, 20 to 6, 30 to 6), calls)
        calls.clear()
        for (level in Level.entries) {
            assertEquals(Question.Operation.MULTIPLICATION, useCase.generateMultiplication(level).operation)
        }
        assertEquals(listOf(0 to 6, 1 to 6, 2 to 6, 3 to 6), calls)
    }

    @Test
    fun multiplicationQuestionsRespectBothFormatsAndAllLevelBounds() {
        val factorLimits = mapOf(Level.TEST to 3, Level.EASY to 5,
            Level.NORMAL to 10, Level.HARD to 12)
        for ((level, maxFactor) in factorLimits) {
            val maxProduct = maxFactor * maxFactor
            val formats = mutableSetOf<Question.UnknownPosition>()
            repeat(200) {
                val question = generateQuestion.generateMultiplication(level)
                formats += question.unknownPosition
                assertEquals(Question.Operation.MULTIPLICATION, question.operation)
                assertTrue(question.firstOperand in 1..maxFactor)
                assertTrue(question.secondOperand in 1..maxFactor)
                assertEquals(question.firstOperand * question.secondOperand, question.result)
                assertTrue(question.result in 1..maxProduct)
                assertEquals(if (question.unknownPosition == Question.UnknownPosition.RESULT)
                    question.result else question.secondOperand, question.rightAnswer)
                assertTrue(question.rightAnswer > 0)
                assertEquals(6, question.options.size)
                assertEquals(6, question.options.toSet().size)
                assertEquals(1, question.options.count { it == question.rightAnswer })
                assertTrue(question.options.all { it in 0..maxProduct })
            }
            assertEquals(Question.UnknownPosition.entries.toSet(), formats)
        }
    }

    @Test
    fun multiplicationModelCoversEveryFactorPairAndRejectsInvalidProducts() {
        for (maxFactor in listOf(3, 5, 10, 12)) {
            for (first in 1..maxFactor) for (second in 1..maxFactor) {
                val product = first * second
                for (position in Question.UnknownPosition.entries) {
                    val question = Question(product, first, emptyList(),
                        Question.Operation.MULTIPLICATION, position, second)
                    assertEquals(first, question.firstOperand)
                    assertEquals(second, question.secondOperand)
                    assertEquals(product, question.result)
                    assertEquals(if (position == Question.UnknownPosition.RESULT) product else second,
                        question.rightAnswer)
                }
            }
        }
        for ((product, first, second) in listOf(Triple(0, 0, 1), Triple(0, 1, 0),
            Triple(7, 2, 3), Triple(Int.MAX_VALUE, Int.MAX_VALUE, 2))) {
            assertThrows(IllegalArgumentException::class.java) {
                Question(product, first, emptyList(), Question.Operation.MULTIPLICATION,
                    Question.UnknownPosition.RESULT, second)
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            Question(1, 1, emptyList(), Question.Operation.MULTIPLICATION,
                Question.UnknownPosition.RESULT)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameRepositoryImpl.generateMultiplicationQuestion(Level.TEST, 11)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GameRepositoryImpl.generateMultiplicationQuestion(Level.HARD, 0)
        }
    }

    @Test
    fun multiplicationFormatsAreApproximatelyBalancedAndZeroCanBeWrong() {
        for (level in Level.entries) {
            var resultFormats = 0
            var zeroWrongSeen = false
            repeat(1000) {
                val question = generateQuestion.generateMultiplication(level)
                if (question.unknownPosition == Question.UnknownPosition.RESULT) resultFormats++
                if (0 in question.options) zeroWrongSeen = true
            }
            assertTrue("$level: result formats=$resultFormats", resultFormats in 400..600)
            assertTrue("$level: zero never appeared as a wrong option", zeroWrongSeen)
        }
    }

    @Test
    fun accuracyTruncationRespectsEachLevelThresholdBoundary() {
        val cases = listOf(
            Triple(Level.TEST, 3 to 6, 2 to 6),
            Triple(Level.EASY, 7 to 10, 6 to 10),
            Triple(Level.NORMAL, 8 to 10, 7 to 10),
            Triple(Level.HARD, 9 to 10, 8 to 10)
        )
        for ((level, atThreshold, belowThreshold) in cases) {
            val settings = getSettings(level)
            val target = settings.minPercentOfRightAnswers
            assertEquals(target, calculatePercentOfRightAnswers(atThreshold.first, atThreshold.second))
            assertTrue(calculatePercentOfRightAnswers(belowThreshold.first, belowThreshold.second) < target)
        }
        assertEquals(66, calculatePercentOfRightAnswers(2, 3))
    }

    @Test
    fun countAndAccuracyThresholdsHaveIndependentBoundaryCases() {
        val cases = listOf(
            Triple(Level.TEST, 3 to 6, 3 to 7),
            Triple(Level.EASY, 10 to 14, 10 to 15),
            Triple(Level.NORMAL, 20 to 25, 20 to 26),
            Triple(Level.HARD, 30 to 33, 30 to 34)
        )
        for ((level, bothMet, countOnly) in cases) {
            val settings = getSettings(level)
            assertEquals(settings.minCountOfRightAnswers, bothMet.first)
            assertTrue(calculatePercentOfRightAnswers(bothMet.first, bothMet.second) >= settings.minPercentOfRightAnswers)
            assertEquals(settings.minCountOfRightAnswers, countOnly.first)
            assertTrue(calculatePercentOfRightAnswers(countOnly.first, countOnly.second) < settings.minPercentOfRightAnswers)
            val accuracyOnlyCount = settings.minCountOfRightAnswers - 1
            assertEquals(100, calculatePercentOfRightAnswers(accuracyOnlyCount, accuracyOnlyCount))
        }
    }

    @Test
    fun accuracyHandlesNoAnswersAndAllCorrectOrWrong() {
        assertEquals(0, calculatePercentOfRightAnswers(0, 0))
        assertEquals(0, calculatePercentOfRightAnswers(0, 10))
        assertEquals(100, calculatePercentOfRightAnswers(10, 10))
    }

    @Test
    fun settingsMatchEveryLevel() {
        assertEquals(GameSettings(10, 3, 50, 8), getSettings(Level.TEST))
        assertEquals(GameSettings(10, 10, 70, 60), getSettings(Level.EASY))
        assertEquals(GameSettings(20, 20, 80, 60), getSettings(Level.NORMAL))
        assertEquals(GameSettings(30, 30, 90, 90), getSettings(Level.HARD))
    }

    @Test
    fun questionsRespectEveryLevelRangeAndContainOneCorrectOption() {
        for (level in Level.entries) {
            val maximum = getSettings(level).maxSumValue
            repeat(100) {
                val question = generateQuestion(maximum)
                assertEquals(Question.Operation.ADDITION, question.operation)
                assertEquals(Question.UnknownPosition.SECOND_OPERAND, question.unknownPosition)
                assertTrue("$level: sum=${question.sum}", question.sum in 0..maximum)
                assertTrue("$level: visible=${question.visibleNumber}", question.visibleNumber in 0..question.sum)
                assertTrue("$level: answer=${question.rightAnswer}", question.rightAnswer in 0..question.sum)
                assertEquals(question.sum, question.visibleNumber + question.rightAnswer)
                assertEquals(question.firstOperand + question.secondOperand, question.result)
                assertEquals(question.secondOperand, question.rightAnswer)
                assertEquals(6, question.options.size)
                assertEquals(6, question.options.toSet().size)
                assertEquals(1, question.options.count { it == question.rightAnswer })
                assertTrue(question.options.all { it in 0..maximum })
            }
        }
    }

    @Test
    fun subtractionQuestionsCoverBothFormatsAndStayWithinEveryLevelRange() {
        for (level in Level.entries) {
            val maximum = getSettings(level).maxSumValue
            val seen = mutableSetOf<Question.UnknownPosition>()
            repeat(200) {
                val question = generateQuestion.generateSubtraction(level)
                seen += question.unknownPosition
                assertEquals(Question.Operation.SUBTRACTION, question.operation)
                assertTrue(question.firstOperand in 0..maximum)
                assertTrue(question.secondOperand in 0..maximum)
                assertTrue(question.result in 0..maximum)
                assertTrue(question.firstOperand >= question.secondOperand)
                assertEquals(question.firstOperand - question.secondOperand, question.result)
                assertTrue(question.rightAnswer in 0..maximum)
                val expected = if (question.unknownPosition == Question.UnknownPosition.RESULT) {
                    question.result
                } else {
                    question.secondOperand
                }
                assertEquals(expected, question.rightAnswer)
                assertEquals(6, question.options.size)
                assertEquals(6, question.options.toSet().size)
                assertEquals(1, question.options.count { it == question.rightAnswer })
                assertTrue(question.options.all { it in 0..maximum })
            }
            assertEquals(Question.UnknownPosition.entries.toSet(), seen)
        }
    }

    @Test
    fun allValidEquationsAtEachLevelBoundaryResolveTheMissingValue() {
        for (level in Level.entries) {
            val maximum = getSettings(level).maxSumValue
            for (left in 0..maximum) {
                for (right in 0..left) {
                    val addition = Question(left, right, emptyList())
                    assertEquals(left, addition.result)
                    assertEquals(left - right, addition.rightAnswer)
                    assertEquals(left, addition.firstOperand + addition.secondOperand)

                    val missingResult = Question(left, right, emptyList(),
                        Question.Operation.SUBTRACTION, Question.UnknownPosition.RESULT)
                    assertEquals(left - right, missingResult.rightAnswer)
                    assertEquals(left - right, missingResult.result)

                    val missingSubtrahend = Question(left, left - right, emptyList(),
                        Question.Operation.SUBTRACTION, Question.UnknownPosition.SECOND_OPERAND)
                    assertEquals(right, missingSubtrahend.rightAnswer)
                    assertEquals(right, missingSubtrahend.secondOperand)
                    assertEquals(left - right, missingSubtrahend.result)
                }
            }
        }
    }

    @Test
    fun repeatedSubtractionGenerationUsesBothFormatsApproximatelyEqually() {
        for (level in Level.entries) {
            var missingResults = 0
            repeat(1000) {
                val question = generateQuestion.generateSubtraction(level)
                if (question.unknownPosition == Question.UnknownPosition.RESULT) missingResults++
            }
            assertTrue("$level: missing results=$missingResults", missingResults in 400..600)
        }
    }

    @Test
    fun subtractionAllowsZeroAndMaximumBoundaryCases() {
        val zeroResult = Question(0, 0, listOf(0), Question.Operation.SUBTRACTION,
            Question.UnknownPosition.RESULT)
        val zeroSubtrahend = Question(0, 0, listOf(0), Question.Operation.SUBTRACTION,
            Question.UnknownPosition.SECOND_OPERAND)
        assertEquals(0, zeroResult.result)
        assertEquals(0, zeroSubtrahend.secondOperand)
        val maximum = Question(30, 0, listOf(30), Question.Operation.SUBTRACTION,
            Question.UnknownPosition.RESULT)
        assertEquals(30, maximum.rightAnswer)
        repeat(100) {
            val question = GameRepositoryImpl.generateSubtractionQuestion(5, 6)
            assertEquals((0..5).toSet(), question.options.toSet())
        }
    }

    @Test
    fun subtractionRejectsInvalidRangesAndOptionCounts() {
        for (maximum in listOf(-1, 0, 4, Int.MAX_VALUE)) {
            assertThrows(IllegalArgumentException::class.java) {
                GameRepositoryImpl.generateSubtractionQuestion(maximum, 6)
            }
        }
        for (count in listOf(0, 7)) {
            assertThrows(IllegalArgumentException::class.java) {
                GameRepositoryImpl.generateSubtractionQuestion(5, count)
            }
        }
    }

    @Test
    fun smallestRangeSupportingSixOptionsTerminates() {
        val singleOption = GameRepositoryImpl.generateQuestion(5, 1)
        assertEquals(listOf(singleOption.rightAnswer), singleOption.options)

        repeat(100) {
            val question = GameRepositoryImpl.generateQuestion(5, 6)
            assertTrue(question.sum in 0..5)
            assertTrue(question.visibleNumber in 0..question.sum)
            assertEquals(question.sum, question.visibleNumber + question.rightAnswer)
            assertEquals(6, question.options.toSet().size)
            assertEquals((0..5).toSet(), question.options.toSet())
            assertTrue(question.rightAnswer in question.options)
        }
    }

    @Test
    fun largestSupportedMaximumDoesNotOverflow() {
        val maximum = Int.MAX_VALUE - 1
        val question = GameRepositoryImpl.generateQuestion(maximum, 6)
        assertTrue(question.sum in 0..maximum)
        assertTrue(question.visibleNumber in 0..question.sum)
        assertEquals(question.sum, question.visibleNumber + question.rightAnswer)
        assertEquals(6, question.options.toSet().size)
        assertTrue(question.options.all { it in 0..maximum })
    }

    @Test
    fun invalidMaximumsAndOptionCountsAreRejected() {
        for (maximum in listOf(-1, 0, 4, Int.MAX_VALUE)) {
            val error = assertThrows(IllegalArgumentException::class.java) {
                GameRepositoryImpl.generateQuestion(maximum, 6)
            }
            assertTrue(error.message?.contains("maxSumValue") == true)
        }
        for (count in listOf(0, 7)) {
            val error = assertThrows(IllegalArgumentException::class.java) {
                GameRepositoryImpl.generateQuestion(5, count)
            }
            assertTrue(error.message?.contains("countOfOptions") == true)
        }
    }
}
