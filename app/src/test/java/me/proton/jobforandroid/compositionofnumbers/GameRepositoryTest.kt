package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GenerateQuestionUseCase
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GetGameSettingsUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameRepositoryTest {
    private val getSettings = GetGameSettingsUseCase(GameRepositoryImpl)
    private val generateQuestion = GenerateQuestionUseCase(GameRepositoryImpl)

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
