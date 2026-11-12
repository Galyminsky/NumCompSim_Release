package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonGameSession
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSessionSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonGameSessionTest {
    private fun questions(level: ComparisonLevel): List<ComparisonQuestion> {
        val expression = ComparisonSide.Expression(2, ComparisonOperation.ADDITION, 3)
        val question = when (level) {
            ComparisonLevel.TEST, ComparisonLevel.EASY ->
                ComparisonQuestion(level, ComparisonSide.Number(2), ComparisonSide.Number(3))
            ComparisonLevel.NORMAL ->
                ComparisonQuestion(level, expression, ComparisonSide.Number(6))
            ComparisonLevel.HARD ->
                ComparisonQuestion(level, expression, ComparisonSide.Expression(3, ComparisonOperation.ADDITION, 4))
        }
        return List(ComparisonSessionSettings.forLevel(level).maximumAnswers) { question }
    }

    @Test
    fun answerIsCountedImmediatelyAndNextQuestionWaitsForFeedback() {
        for (level in ComparisonLevel.entries) {
            val session = ComparisonGameSession(questions(level))
            val first = session.currentQuestion
            assertEquals(ComparisonGameSession.State.QUESTION, session.state)
            assertNull(session.result)
            session.answer(ComparisonSign.GREATER)
            assertEquals(ComparisonGameSession.State.FEEDBACK, session.state)
            assertEquals(first, session.currentQuestion)
            assertEquals(1, session.answeredCount)
            assertEquals(0, session.correctCount)
            assertEquals(false, session.lastAnswerCorrect)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
            session.finishFeedback()
            assertEquals(ComparisonGameSession.State.QUESTION, session.state)
            assertEquals(1, session.answeredCount)
            assertNull(session.lastAnswerCorrect)
            session.answer(ComparisonSign.LESS)
            assertEquals(2, session.answeredCount)
            assertEquals(1, session.correctCount)
            assertEquals(true, session.lastAnswerCorrect)
        }
    }

    @Test
    fun questionsArePresentedInProvidedOrder() {
        val ordered = questions(ComparisonLevel.TEST).toMutableList()
        ordered[1] = ComparisonQuestion(
            ComparisonLevel.TEST, ComparisonSide.Number(7), ComparisonSide.Number(7)
        )
        val session = ComparisonGameSession(ordered)
        assertEquals(ordered[0], session.currentQuestion)
        session.answer(ComparisonSign.LESS)
        assertEquals(ordered[0], session.currentQuestion)
        session.finishFeedback()
        assertEquals(ordered[1], session.currentQuestion)
        session.answer(ComparisonSign.EQUAL)
        assertEquals(true, session.lastAnswerCorrect)
    }

    @Test
    fun maximumEndsOnlyAfterFeedbackAndRejectsFurtherAnswers() {
        for (level in ComparisonLevel.entries) {
            val session = ComparisonGameSession(questions(level))
            val maximum = ComparisonSessionSettings.forLevel(level).maximumAnswers
            repeat(maximum) { index ->
                session.answer(ComparisonSign.LESS)
                assertEquals(index + 1, session.answeredCount)
                assertEquals(ComparisonGameSession.State.FEEDBACK, session.state)
                assertNull(session.result)
                assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
                session.finishFeedback()
            }
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertNull(session.currentQuestion)
            assertEquals(maximum, session.result?.answeredCount)
            assertTrue(requireNotNull(session.result).isSuccess)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
            assertThrows(IllegalStateException::class.java) { session.finishFeedback() }
        }
    }

    @Test
    fun timeExpiryExcludesUnansweredQuestion() {
        for (level in ComparisonLevel.entries) {
            val session = ComparisonGameSession(questions(level))
            session.expireTime()
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(0, session.result?.answeredCount)
            assertFalse(requireNotNull(session.result).isSuccess)
            assertNull(session.currentQuestion)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
            session.expireTime()
        }
    }

    @Test
    fun timeExpiryDuringFeedbackKeepsAnswerUntilFeedbackEnds() {
        for (level in ComparisonLevel.entries) {
            val session = ComparisonGameSession(questions(level))
            session.answer(ComparisonSign.LESS)
            session.expireTime()
            assertEquals(ComparisonGameSession.State.FEEDBACK, session.state)
            assertEquals(1, session.answeredCount)
            assertNull(session.result)
            assertThrows(IllegalStateException::class.java) { session.answer(ComparisonSign.LESS) }
            session.finishFeedback()
            assertEquals(ComparisonGameSession.State.FINISHED, session.state)
            assertEquals(1, session.result?.correctCount)
            assertNull(session.currentQuestion)
        }
    }

    @Test
    fun minimumDoesNotFinishAndInvalidTransitionsAreRejected() {
        for (level in ComparisonLevel.entries) {
            val session = ComparisonGameSession(questions(level))
            assertThrows(IllegalStateException::class.java) { session.finishFeedback() }
            repeat(ComparisonSessionSettings.forLevel(level).minimumAnswers) {
                session.answer(ComparisonSign.LESS)
                session.finishFeedback()
            }
            assertEquals(ComparisonGameSession.State.QUESTION, session.state)
            assertNull(session.result)
            session.expireTime()
            assertTrue(requireNotNull(session.result).isSuccess)
        }
    }

    @Test
    fun requiresCompleteSingleLevelSet() {
        assertThrows(IllegalArgumentException::class.java) { ComparisonGameSession(emptyList()) }
        val testQuestions = questions(ComparisonLevel.TEST)
        assertThrows(IllegalArgumentException::class.java) { ComparisonGameSession(testQuestions.dropLast(1)) }
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonGameSession(testQuestions.dropLast(1) + questions(ComparisonLevel.EASY).first())
        }
    }
}
