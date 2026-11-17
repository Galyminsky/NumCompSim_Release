package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSession
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSessionSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonSessionTest {
    @Test
    fun settingsMatchEveryLevel() {
        assertEquals(ComparisonSessionSettings(15, 5, 24, 60), ComparisonSessionSettings.forLevel(ComparisonLevel.TEST))
        assertEquals(ComparisonSessionSettings(60, 15, 45, 70), ComparisonSessionSettings.forLevel(ComparisonLevel.EASY))
        assertEquals(ComparisonSessionSettings(90, 12, 36, 75), ComparisonSessionSettings.forLevel(ComparisonLevel.NORMAL))
        assertEquals(ComparisonSessionSettings(120, 10, 30, 80), ComparisonSessionSettings.forLevel(ComparisonLevel.HARD))
    }

    @Test
    fun exactAccuracyBoundaryPassesAndOneFewerCorrectAnswerFails() {
        val cases = listOf(
            Triple(ComparisonLevel.TEST, 5, 3),
            Triple(ComparisonLevel.EASY, 20, 14),
            Triple(ComparisonLevel.NORMAL, 12, 9),
            Triple(ComparisonLevel.HARD, 10, 8)
        )
        for ((level, answered, correct) in cases) {
            assertTrue(ComparisonResult(level, answered, correct).isSuccess)
            assertFalse(ComparisonResult(level, answered, correct - 1).isSuccess)
        }
    }

    @Test
    fun fractionalAccuracyBoundariesUseActualAnswerCountsAndTruncatedPercentages() {
        data class Case(
            val level: ComparisonLevel,
            val answered: Int,
            val passingCorrect: Int,
            val passingPercent: Int,
            val failingPercent: Int,
            val minimumAnswers: Int,
            val requiredPercent: Int
        )

        val cases = listOf(
            Case(ComparisonLevel.TEST, 7, 5, 71, 57, 5, 60),
            Case(ComparisonLevel.EASY, 17, 12, 70, 64, 15, 70),
            Case(ComparisonLevel.NORMAL, 13, 10, 76, 69, 12, 75),
            Case(ComparisonLevel.HARD, 11, 9, 81, 72, 10, 80)
        )
        for (case in cases) {
            val settings = ComparisonSessionSettings.forLevel(case.level)
            assertEquals(case.level.toString(), case.minimumAnswers, settings.minimumAnswers)
            assertEquals(case.level.toString(), case.requiredPercent, settings.requiredAccuracyPercent)
            assertTrue(case.level.toString(), case.answered >= settings.minimumAnswers)

            for (correct in listOf(case.passingCorrect, case.passingCorrect - 1)) {
                var session = ComparisonSession.start(case.level)
                repeat(correct) { session = session.answer(true) }
                repeat(case.answered - correct) { session = session.answer(false) }
                val result = session.toResult()

                assertEquals(case.level.toString(), case.answered, result.answeredCount)
                assertEquals(case.level.toString(), correct, result.correctCount)
                assertEquals(
                    case.level.toString(),
                    if (correct == case.passingCorrect) case.passingPercent else case.failingPercent,
                    result.accuracyPercent
                )
                assertEquals(case.level.toString(), correct == case.passingCorrect, result.isSuccess)
            }
        }
    }

    @Test
    fun minimumIsRequiredButDoesNotEndSession() {
        for (level in ComparisonLevel.entries) {
            val minimum = ComparisonSessionSettings.forLevel(level).minimumAnswers
            val before = ComparisonResult(level, minimum - 1, minimum - 1)
            assertFalse(before.isSuccess)
            var session = ComparisonSession.start(level)
            repeat(minimum) { session = session.answer(true) }
            assertTrue(session.toResult().isSuccess)
            assertFalse(session.isAnswerLimitReached)
            assertEquals(minimum + 1, session.answer(false).answeredCount)
        }
    }

    @Test
    fun maximumAnsweredEndsSessionAndRejectsFurtherAnswers() {
        for (level in ComparisonLevel.entries) {
            val maximum = ComparisonSessionSettings.forLevel(level).maximumAnswers
            var session = ComparisonSession.start(level)
            repeat(maximum - 1) { session = session.answer(true) }
            assertFalse(session.isAnswerLimitReached)
            session = session.answer(false)
            assertTrue(session.isAnswerLimitReached)
            assertEquals(maximum, session.toResult().answeredCount)
            assertThrows(IllegalStateException::class.java) { session.answer(true) }
        }
    }

    @Test
    fun wrongAnswersCountAndZeroIsSafe() {
        for (level in ComparisonLevel.entries) {
            val initial = ComparisonSession.start(level)
            assertEquals(0, initial.toResult().accuracyPercent)
            assertFalse(initial.toResult().isSuccess)
            val afterWrong = initial.answer(false)
            assertEquals(0, initial.answeredCount)
            assertEquals(1, afterWrong.answeredCount)
            assertEquals(0, afterWrong.correctCount)
            val afterCorrect = afterWrong.answer(true).toResult()
            assertEquals(2, afterCorrect.answeredCount)
            assertEquals(1, afterCorrect.correctCount)
            assertEquals(50, afterCorrect.accuracyPercent)
            assertFalse(afterCorrect.isSuccess)
        }
    }
}
