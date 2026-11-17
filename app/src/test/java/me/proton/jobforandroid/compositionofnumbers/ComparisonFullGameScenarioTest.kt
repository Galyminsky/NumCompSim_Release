package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.ComparisonSessionDataSource
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonGameSession
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSessionSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ComparisonFullGameScenarioTest {
    private val source = ComparisonSessionDataSource()
    private val sets by lazy {
        ComparisonLevel.entries.associateWith { source.generateQuestions(it, Random(3404)) }
    }

    private fun game(level: ComparisonLevel) = ComparisonGameSession(sets.getValue(level))

    private fun wrongSign(correct: ComparisonSign): ComparisonSign = when (correct) {
        ComparisonSign.LESS -> ComparisonSign.GREATER
        ComparisonSign.EQUAL, ComparisonSign.GREATER -> ComparisonSign.LESS
    }

    private fun answer(session: ComparisonGameSession, correct: Boolean) {
        val question = requireNotNull(session.currentQuestion)
        val sign = if (correct) question.correctSign else wrongSign(question.correctSign)
        session.answer(sign)
    }

    @Test
    fun generatedFullSessionsEndAtMaximumForEveryLevel() {
        for (level in ComparisonLevel.entries) {
            val settings = ComparisonSessionSettings.forLevel(level)
            val generated = sets.getValue(level)
            assertEquals(settings.maximumAnswers, generated.size)
            assertTrue(generated.all { it.level == level && ComparisonQuestionValidator.isValid(it) })
            val session = game(level)
            generated.forEachIndexed { index, question ->
                assertEquals(ComparisonGameSession.State.QUESTION, session.state)
                assertEquals(question, session.currentQuestion)
                answer(session, true)
                assertEquals(index + 1, session.answeredCount)
                assertEquals(ComparisonGameSession.State.FEEDBACK, session.state)
                session.finishFeedback()
            }
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(settings.maximumAnswers, session.result?.answeredCount)
            assertEquals(settings.maximumAnswers, session.result?.correctCount)
            assertTrue(requireNotNull(session.result).isSuccess)
            assertNull(session.currentQuestion)
        }
    }

    @Test
    fun allWrongAnswersReachMaximumButFail() {
        for (level in ComparisonLevel.entries) {
            val session = game(level)
            repeat(ComparisonSessionSettings.forLevel(level).maximumAnswers) {
                answer(session, false)
                assertEquals(false, session.lastAnswerCorrect)
                session.finishFeedback()
            }
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(0, session.result?.correctCount)
            assertFalse(requireNotNull(session.result).isSuccess)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
        }
    }

    @Test
    fun mixedAnswersRespectExactPassingBoundaryAndMinimum() {
        val cases = listOf(
            Triple(ComparisonLevel.TEST, 5, 3),
            Triple(ComparisonLevel.EASY, 20, 14),
            Triple(ComparisonLevel.NORMAL, 12, 9),
            Triple(ComparisonLevel.HARD, 10, 8)
        )
        for ((level, answered, passingCorrect) in cases) {
            for (correctCount in listOf(passingCorrect - 1, passingCorrect)) {
                val session = game(level)
                repeat(answered) { index ->
                    answer(session, index < correctCount)
                    session.finishFeedback()
                }
                assertEquals(ComparisonGameSession.State.QUESTION, session.state)
                assertNull(session.result)
                session.expireTime()
                val result = requireNotNull(session.result)
                assertEquals(answered, result.answeredCount)
                assertEquals(correctCount, result.correctCount)
                assertEquals(correctCount == passingCorrect, result.isSuccess)
            }
            val minimum = ComparisonSessionSettings.forLevel(level).minimumAnswers
            val belowMinimum = game(level)
            repeat(minimum - 1) {
                answer(belowMinimum, true)
                belowMinimum.finishFeedback()
            }
            belowMinimum.expireTime()
            assertFalse(requireNotNull(belowMinimum.result).isSuccess)
        }
    }

    @Test
    fun expiryBeforeAnswerExcludesDisplayedQuestion() {
        for (level in ComparisonLevel.entries) {
            val session = game(level)
            assertEquals(sets.getValue(level).first(), session.currentQuestion)
            session.expireTime()
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(0, session.result?.answeredCount)
            assertEquals(0, session.result?.accuracyPercent)
            assertFalse(requireNotNull(session.result).isSuccess)
            assertNull(session.currentQuestion)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
        }
    }

    @Test
    fun expiryDuringFeedbackKeepsAnswerAndWaitsForFeedback() {
        for (level in ComparisonLevel.entries) {
            val session = game(level)
            val first = session.currentQuestion
            answer(session, true)
            session.expireTime()
            assertEquals(ComparisonGameSession.State.FEEDBACK, session.state)
            assertEquals(first, session.currentQuestion)
            assertEquals(1, session.answeredCount)
            assertEquals(1, session.correctCount)
            assertNull(session.result)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.EQUAL) }
            session.finishFeedback()
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(1, session.result?.answeredCount)
            assertEquals(1, session.result?.correctCount)
            assertNull(session.currentQuestion)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.EQUAL) }
        }
    }

    @Test
    fun duplicateAnswerCannotAdvanceBeforeFeedback() {
        for (level in ComparisonLevel.entries) {
            val session = game(level)
            val first = session.currentQuestion
            answer(session, false)
            assertThrows(IllegalStateException::class.java) { answer(session, true) }
            assertEquals(1, session.answeredCount)
            assertEquals(0, session.correctCount)
            assertEquals(first, session.currentQuestion)
            assertNull(session.result)
            session.finishFeedback()
            assertEquals(sets.getValue(level)[1], session.currentQuestion)
        }
    }

    @Test
    fun sequentialDataRequestsCreateIndependentSessions() {
        for (level in ComparisonLevel.entries) {
            val firstSet = source.generateQuestions(level, Random(77))
            val secondSet = source.generateQuestions(level, Random(77))
            assertNotSame(firstSet, secondSet)
            assertEquals(firstSet, secondSet)
            val first = ComparisonGameSession(firstSet)
            val second = ComparisonGameSession(secondSet)
            answer(first, true)
            first.finishFeedback()
            first.expireTime()
            assertEquals(ComparisonGameSession.State.FINISHED, first.state)
            assertEquals(ComparisonGameSession.State.QUESTION, second.state)
            assertEquals(0, second.answeredCount)
            assertNull(second.result)
            assertEquals(secondSet.first(), second.currentQuestion)
        }
    }
}
