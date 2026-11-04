package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
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
    fun questionComputesCorrectAnswerAtRangeBoundaries() {
        assertEquals(0, Question(0, 0, listOf(0, 1, 2, 3, 4, 5)).rightAnswer)
        assertEquals(10, Question(10, 0, listOf(10, 0, 1, 2, 3, 4)).rightAnswer)
        assertEquals(0, Question(10, 10, listOf(0, 1, 2, 3, 4, 5)).rightAnswer)
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
        }
        val useCase = GenerateQuestionUseCase(repository)
        for (level in Level.entries) {
            assertEquals(6, useCase(getSettings(level).maxSumValue).options.size)
        }
        assertEquals(listOf(10 to 6, 10 to 6, 20 to 6, 30 to 6), calls)
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
            val target = getSettings(level).minPercentOfRightAnswers
            assertEquals(target, calculatePercentOfRightAnswers(atThreshold.first, atThreshold.second))
            assertTrue(calculatePercentOfRightAnswers(belowThreshold.first, belowThreshold.second) < target)
        }
        assertEquals(66, calculatePercentOfRightAnswers(2, 3))
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
                assertTrue("$level: sum=${question.sum}", question.sum in 0..maximum)
                assertTrue("$level: visible=${question.visibleNumber}", question.visibleNumber in 0..question.sum)
                assertTrue("$level: answer=${question.rightAnswer}", question.rightAnswer in 0..question.sum)
                assertEquals(question.sum, question.visibleNumber + question.rightAnswer)
                assertEquals(6, question.options.size)
                assertEquals(6, question.options.toSet().size)
                assertEquals(1, question.options.count { it == question.rightAnswer })
                assertTrue(question.options.all { it in 0..maximum })
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
