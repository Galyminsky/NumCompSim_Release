package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionAnswer
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionExercise
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionGameSession
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionGameSession.Snapshot
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionGameSession.State
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FractionGameSessionTest {
    @Test fun simultaneousDeadlinesAndLargeTimeJumpFinishExactlyOnce() {
        for (jump in listOf(0L, 1_000_000L)) {
            val session = start()
            assertTrue(answer(session, session.deadlineMs - 500))
            assertEquals(session.deadlineMs, session.feedbackEndMs)
            session.advance(session.deadlineMs - 1)
            assertEquals(State.FEEDBACK, session.state)
            val restored = FractionGameSession.restore(session.snapshot(), session.deadlineMs + jump)
            assertEquals(State.FINISHED, restored.state)
            assertEquals(FractionGameSession.Result(1, 1, false), restored.result)
            val finished = restored.snapshot()
            restored.advance(session.deadlineMs + jump + 1)
            assertFalse(restored.answer(finished.questions.first(), 0, session.deadlineMs + jump + 1))
            assertEquals(finished, restored.snapshot())
        }
    }

    private fun questions(level: Level = Level.TEST) = FractionSessionGenerator.generate(level, Random(31))
    private fun start(level: Level = Level.TEST) = FractionGameSession.start(level, questions(level), 1_000)
    private fun correctIndex(question: FractionQuestion<out FractionAnswer>): Int = question.options.indexOfFirst {
        val correct = question.correctAnswer
        if (it is FractionAnswer.FractionValue && correct is FractionAnswer.FractionValue)
            it.value.isEquivalentTo(correct.value) else it == correct
    }
    private fun answer(session: FractionGameSession, now: Long, correct: Boolean = true): Boolean {
        val question = requireNotNull(session.currentQuestion)
        val right = correctIndex(question)
        return session.answer(question, if (correct) right else (right + 1) % question.options.size, now)
    }

    @Test fun answersMustBeStrictlyBeforeDeadline() {
        for (offset in listOf(-1L, 0L, 1L)) {
            val session = start()
            assertEquals(offset < 0, answer(session, session.deadlineMs + offset))
            assertEquals(if (offset < 0) 1 else 0, session.answeredCount)
            assertEquals(if (offset < 0) State.FEEDBACK else State.FINISHED, session.state)
        }
    }

    @Test fun acceptanceCountsImmediatelyAndFeedbackEndsAtExactlyFiveHundredMs() {
        val session = start()
        val first = requireNotNull(session.currentQuestion)
        val option = correctIndex(first)
        assertTrue(session.answer(first, option, 1_100))
        assertEquals(1, session.answeredCount)
        assertEquals(1, session.correctCount)
        assertEquals(true, session.lastAnswerCorrect)
        assertSame(first.options[option], session.selectedAnswer)
        assertSame(first, session.currentQuestion)
        assertEquals(1_600L, session.feedbackEndMs)
        assertNull(session.result)
        session.advance(1_599)
        assertEquals(State.FEEDBACK, session.state)
        session.advance(1_600)
        assertEquals(State.QUESTION, session.state)
        assertEquals(1, session.currentPosition)
        assertNull(session.selectedAnswer)
        assertNull(session.feedbackEndMs)
        assertNull(session.lastAnswerCorrect)
    }

    @Test fun repeatedStaleForeignAndInvalidOptionsDoNotChangeStatistics() {
        val session = start()
        val first = requireNotNull(session.currentQuestion)
        val foreign = requireNotNull(start().currentQuestion)
        assertFalse(session.answer(foreign, 0, 1_000))
        assertFalse(session.answer(first, -1, 1_000))
        assertFalse(session.answer(first, first.options.size, 1_000))
        assertTrue(answer(session, 1_000, false))
        val feedback = session.snapshot()
        assertFalse(session.answer(first, 0, 1_001))
        assertEquals(feedback, session.snapshot())
        // An old click arriving on the next question is rejected even at the feedback boundary.
        assertFalse(session.answer(first, 0, 1_500))
        assertEquals(State.QUESTION, session.state)
        assertEquals(1, session.answeredCount)
        assertEquals(0, session.correctCount)
        assertTrue(answer(session, 1_500))
        assertEquals(2, session.answeredCount)
    }

    @Test fun deadlineDuringFeedbackWaitsForItsEndWithoutAcceptingAnotherAnswer() {
        val session = start()
        val first = requireNotNull(session.currentQuestion)
        assertTrue(answer(session, session.deadlineMs - 1))
        session.advance(session.deadlineMs)
        assertEquals(State.FEEDBACK, session.state)
        assertFalse(session.answer(first, 0, session.deadlineMs + 1))
        session.advance(session.deadlineMs + 498)
        assertEquals(State.FEEDBACK, session.state)
        session.advance(session.deadlineMs + 499)
        assertEquals(State.FINISHED, session.state)
        assertEquals(1, requireNotNull(session.result).answeredCount)
    }

    @Test fun lastAnswerFinishesOnlyAfterFeedbackAndFinishedEventsAreIdempotent() {
        for (level in Level.values()) {
            val session = start(level)
            var now = 1_000L
            repeat(FractionGameSession.settingsFor(level).questionCount) {
                assertTrue(answer(session, now))
                session.advance(now + 499)
                assertEquals(State.FEEDBACK, session.state)
                session.advance(now + 500)
                now += 500
            }
            assertEquals(State.FINISHED, session.state)
            val result = requireNotNull(session.result)
            assertEquals(FractionGameSession.settingsFor(level).questionCount, result.correctCount)
            assertTrue(result.isSuccess)
            val saved = session.snapshot()
            repeat(3) {
                session.advance(session.deadlineMs + it)
                assertFalse(session.answer(saved.questions.first(), 0, session.deadlineMs + it))
            }
            assertEquals(saved, session.snapshot())
            assertEquals(result, session.result)
            assertNull(session.currentQuestion)
        }
    }

    @Test fun unansweredQuestionAndZeroAnswersAreNotCounted() {
        val empty = start()
        empty.advance(empty.deadlineMs)
        assertEquals(FractionGameSession.Result(0, 0, false), empty.result)
        val partial = start()
        assertTrue(answer(partial, 1_000))
        partial.advance(1_500)
        partial.advance(partial.deadlineMs)
        assertEquals(FractionGameSession.Result(1, 1, false), partial.result)
    }

    @Test fun exactSuccessThresholdsForEveryLevel() {
        // Below minimum despite 100%, minimum with insufficient accuracy, and sufficient accuracy.
        for ((level, minimum, correctAtMinimum) in listOf(
            Triple(Level.TEST, 5, 3), Triple(Level.EASY, 10, 7),
            Triple(Level.NORMAL, 8, 6), Triple(Level.HARD, 6, 5)
        )) {
            assertFalse(result(level, minimum - 1, minimum - 1).isSuccess)
            assertFalse(result(level, minimum, correctAtMinimum - 1).isSuccess)
            assertTrue(result(level, minimum, correctAtMinimum).isSuccess)
        }
        // Fractional percentages must not be rounded up to the required threshold.
        assertFalse(result(Level.TEST, 7, 4).isSuccess)
        assertFalse(result(Level.EASY, 13, 9).isSuccess)
        assertFalse(result(Level.NORMAL, 11, 8).isSuccess)
        assertFalse(result(Level.HARD, 9, 7).isSuccess)
        assertTrue(result(Level.HARD, 10, 8).isSuccess) // exactly 80%
    }

    private fun result(level: Level, answered: Int, correct: Int): FractionGameSession.Result {
        val questions = questions(level)
        val accepted = (0 until answered).map { index ->
            val right = correctIndex(questions[index])
            if (index < correct) right else (right + 1) % questions[index].options.size
        }
        return requireNotNull(FractionGameSession.restore(
            Snapshot(level, questions, accepted, State.FINISHED, 10_000, null), 10_000).result)
    }

    @Test fun restoringQuestionPreservesQuestionsOptionsAnswersPositionAndDeadline() {
        val original = start()
        answer(original, 1_000, false)
        original.advance(1_500)
        val snapshot = original.snapshot()
        val restored = FractionGameSession.restore(snapshot, 2_000)
        assertEquals(snapshot, restored.snapshot())
        assertSame(snapshot.questions[1], restored.currentQuestion)
        assertEquals(snapshot.questions.map { it.options }, restored.snapshot().questions.map { it.options })
        assertEquals(1, restored.answeredCount)
        assertEquals(0, restored.correctCount)
        assertEquals(original.deadlineMs, restored.deadlineMs)
        answer(restored, 2_000)
        assertEquals(2, restored.answeredCount)
    }

    @Test fun restoringFeedbackPreservesSelectionAndDoesNotRecountOrRestartDelay() {
        val original = start()
        answer(original, 1_000)
        val saved = original.snapshot()
        val restored = FractionGameSession.restore(saved, 1_499)
        assertEquals(saved, restored.snapshot())
        assertEquals(original.selectedAnswer, restored.selectedAnswer)
        assertEquals(1, restored.correctCount)
        assertFalse(answer(restored, 1_499))
        restored.advance(1_500)
        assertEquals(State.QUESTION, restored.state)
        assertEquals(1, restored.answeredCount)
        assertEquals(1, restored.correctCount)
    }

    private fun <A : FractionAnswer> rebuild(question: FractionQuestion<A>): FractionQuestion<A> =
        FractionQuestion(question.exercise, question.options)

    @Test fun reconstructedQuestionsRejectOldObjectEventsAndCopiedListsCannotMutateSession() {
        val original = start()
        answer(original, 1_000)
        val saved = original.snapshot()
        val reconstructed = saved.questions.map { rebuild(it) }.toMutableList()
        val answers = saved.acceptedOptions.toMutableList()
        val restored = FractionGameSession.restore(saved.copy(questions = reconstructed, acceptedOptions = answers), 1_500)
        assertFalse(restored.answer(saved.questions[1], 0, 1_500))
        reconstructed.clear()
        answers.clear()
        assertEquals(8, restored.snapshot().questions.size)
        assertEquals(1, restored.answeredCount)
        val exported = restored.snapshot().questions.toMutableList()
        exported.clear()
        assertTrue(answer(restored, 1_500))
        assertEquals(2, restored.answeredCount)
    }

    @Test fun restoringSynchronizesExpiredQuestionAndFeedbackIncludingLastAnswer() {
        val question = start()
        assertEquals(State.FINISHED, FractionGameSession.restore(question.snapshot(), question.deadlineMs).state)
        val feedback = start()
        answer(feedback, feedback.deadlineMs - 1)
        val saved = feedback.snapshot()
        assertEquals(State.FEEDBACK, FractionGameSession.restore(saved, feedback.deadlineMs).state)
        val finished = FractionGameSession.restore(saved, feedback.deadlineMs + 499)
        assertEquals(State.FINISHED, finished.state)
        assertEquals(1, finished.answeredCount)
        val restoredFinished = FractionGameSession.restore(finished.snapshot(), feedback.deadlineMs + 900)
        assertEquals(finished.snapshot(), restoredFinished.snapshot())
        assertEquals(finished.result, restoredFinished.result)
        val next = start()
        answer(next, 1_000)
        assertEquals(State.QUESTION, FractionGameSession.restore(next.snapshot(), 1_500).state)
        val all = questions()
        val last = Snapshot(Level.TEST, all, all.map(::correctIndex), State.FEEDBACK, 31_000, 5_000)
        assertEquals(State.FEEDBACK, FractionGameSession.restore(last, 4_999).state)
        assertEquals(State.FINISHED, FractionGameSession.restore(last, 5_000).state)
    }

    @Test fun testEquivalentCorrectDisplayedFractionIsAccepted() {
        val first = FractionQuestion(FractionExercise.IdentifyFraction(4, 2), listOf(
            FractionAnswer.FractionValue(Fraction(1, 2)), FractionAnswer.FractionValue(Fraction(1, 3)),
            FractionAnswer.FractionValue(Fraction(2, 3)), FractionAnswer.FractionValue(Fraction(3, 4))))
        val session = FractionGameSession.start(Level.TEST, listOf(first) + questions().drop(1), 0)
        assertTrue(session.answer(first, 0, 0))
        assertEquals(1, session.correctCount)
    }

    @Test fun malformedSnapshotsAndOverflowingOrNegativeTimesAreRejected() {
        val session = start()
        val saved = session.snapshot()
        for (bad in listOf(
            saved.copy(questions = saved.questions.drop(1)),
            saved.copy(acceptedOptions = listOf(-1)),
            saved.copy(acceptedOptions = listOf(4)),
            saved.copy(state = State.FEEDBACK),
            saved.copy(feedbackEndMs = 1_500),
            saved.copy(state = State.FEEDBACK, acceptedOptions = listOf(0), feedbackEndMs = saved.deadlineMs + 500),
            saved.copy(acceptedOptions = List(9) { 0 }),
            saved.copy(acceptedOptions = List(8) { 0 }),
            saved.copy(state = State.FINISHED),
            saved.copy(deadlineMs = Long.MAX_VALUE)
        )) assertThrows(IllegalArgumentException::class.java) { FractionGameSession.restore(bad, 1_000) }
        assertThrows(IllegalArgumentException::class.java) { session.advance(-1) }
        assertThrows(IllegalArgumentException::class.java) { FractionGameSession.start(Level.TEST, questions(), -1) }
        assertThrows(IllegalArgumentException::class.java) { FractionGameSession.start(Level.TEST, questions(), Long.MAX_VALUE) }
        // The largest supported start still allows a complete 500 ms feedback interval.
        val maximumStart = Long.MAX_VALUE - 30_000 - 500
        val nearLimit = FractionGameSession.start(Level.TEST, questions(), maximumStart)
        answer(nearLimit, nearLimit.deadlineMs - 1)
        nearLimit.advance(requireNotNull(nearLimit.feedbackEndMs))
        assertEquals(State.FINISHED, nearLimit.state)
    }
}
