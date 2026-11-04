package me.proton.jobforandroid.compositionofnumbers

import org.junit.Test

import org.junit.Assert.*
import me.proton.jobforandroid.compositionofnumbers.presentation.calculatePercentOfRightAnswers

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun answerPercentageUsesZeroForNoAnswersAndTruncatesFractions() {
        assertEquals(0, calculatePercentOfRightAnswers(0, 0))
        assertEquals(0, calculatePercentOfRightAnswers(0, 3))
        assertEquals(66, calculatePercentOfRightAnswers(2, 3))
        assertEquals(70, calculatePercentOfRightAnswers(7, 10))
        assertEquals(100, calculatePercentOfRightAnswers(3, 3))
    }
}
