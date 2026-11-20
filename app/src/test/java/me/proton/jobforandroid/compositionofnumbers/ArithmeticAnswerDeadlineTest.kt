package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.presentation.canAcceptArithmeticAnswer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArithmeticAnswerDeadlineTest {
    private val question = Question(8, 5, listOf(0, 1, 2, 3, 4, 5))
    private val deadline = 5_000_000_000L

    @Test
    fun answerImmediatelyBeforeDeadlineIsAccepted() {
        assertTrue(canAcceptArithmeticAnswer(question, question, false, deadline - 1, deadline))
    }

    @Test
    fun answerAtOrAfterDeadlineIsRejectedEvenBeforeTimerCallback() {
        // gameFinished is still false: onFinish has not been delivered yet.
        assertFalse(canAcceptArithmeticAnswer(question, question, false, deadline, deadline))
        assertFalse(canAcceptArithmeticAnswer(question, question, false, deadline + 1, deadline))
        assertFalse(canAcceptArithmeticAnswer(question, question, false, deadline + 60_000, deadline))
    }

    @Test
    fun finishedGameRejectsFurtherAnswers() {
        assertFalse(canAcceptArithmeticAnswer(question, question, true, deadline - 1, deadline))
    }

    @Test
    fun repeatedAnswerForPreviousQuestionIsRejectedEvenWhenValuesMatch() {
        val nextQuestion = question.copy()
        assertTrue(canAcceptArithmeticAnswer(question, question, false, deadline - 2, deadline))
        assertFalse(canAcceptArithmeticAnswer(question, nextQuestion, false, deadline - 1, deadline))
    }

    @Test
    fun absentCurrentQuestionRejectsAnswer() {
        assertFalse(canAcceptArithmeticAnswer(question, null, false, deadline - 1, deadline))
    }
}
