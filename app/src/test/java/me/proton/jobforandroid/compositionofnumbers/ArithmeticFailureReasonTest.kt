package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.presentation.ArithmeticFailureReason
import me.proton.jobforandroid.compositionofnumbers.presentation.arithmeticFailureReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ArithmeticFailureReasonTest {
    private fun result(level: Level, correct: Int, answered: Int) = GameResult(
        false, correct, answered, GameRepositoryImpl.getGameSettings(level)
    )

    @Test fun onlyCorrectAnswerMinimumIsMissing() {
        for ((level, correct) in listOf(
            Level.TEST to 2, Level.EASY to 9, Level.NORMAL to 19, Level.HARD to 29
        )) {
            assertEquals(
                level.name,
                ArithmeticFailureReason.COUNT,
                arithmeticFailureReason(result(level, correct, correct))
            )
        }
    }

    @Test fun onlyAccuracyIsMissing() {
        for ((level, counts) in listOf(
            Level.TEST to (3 to 7), Level.EASY to (10 to 15),
            Level.NORMAL to (20 to 26), Level.HARD to (30 to 34)
        )) {
            assertEquals(
                level.name,
                ArithmeticFailureReason.ACCURACY,
                arithmeticFailureReason(result(level, counts.first, counts.second))
            )
        }
    }

    @Test fun bothGoalsAreMissingIncludingZeroAnswers() {
        for (level in Level.entries) {
            assertEquals(
                level.name,
                ArithmeticFailureReason.BOTH,
                arithmeticFailureReason(result(level, 0, 0))
            )
        }
        assertEquals(
            ArithmeticFailureReason.BOTH,
            arithmeticFailureReason(result(Level.EASY, 5, 10))
        )
    }

    @Test fun inconsistentFailedResultIsRejected() {
        assertThrows(IllegalStateException::class.java) {
            arithmeticFailureReason(result(Level.EASY, 10, 10))
        }
    }
}
