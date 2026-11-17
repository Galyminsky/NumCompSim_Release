package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestionValidator
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ComparisonQuestionValidatorTest {
    private fun number(value: Int) = ComparisonSide.Number(value)
    private fun expression(first: Int, operation: ComparisonOperation, second: Int) =
        ComparisonSide.Expression(first, operation, second)
    private fun valid(level: ComparisonLevel, left: ComparisonSide, right: ComparisonSide) =
        ComparisonQuestionValidator.isValid(ComparisonQuestion(level, left, right))

    @Test fun numberLevelsCheckBothBoundsAndFormat() {
        assertTrue(valid(ComparisonLevel.TEST, number(0), number(10)))
        assertFalse(valid(ComparisonLevel.TEST, number(-1), number(2)))
        assertFalse(valid(ComparisonLevel.TEST, number(2), number(11)))
        assertTrue(valid(ComparisonLevel.EASY, number(0), number(100)))
        assertFalse(valid(ComparisonLevel.EASY, number(101), number(1)))
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonQuestion(ComparisonLevel.TEST,
                expression(2, ComparisonOperation.ADDITION, 3), number(5))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonQuestion(ComparisonLevel.NORMAL, number(2), number(3))
        }
        assertThrows(IllegalArgumentException::class.java) {
            ComparisonQuestion(ComparisonLevel.HARD,
                expression(2, ComparisonOperation.MULTIPLICATION, 3), number(6))
        }
    }

    @Test fun normalChecksAllOperationsAndNumberRangesInBothOrientations() {
        val add = expression(9, ComparisonOperation.ADDITION, 11)
        assertTrue(valid(ComparisonLevel.NORMAL, add, number(2)))
        assertTrue(valid(ComparisonLevel.NORMAL, number(100), add))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(9, ComparisonOperation.ADDITION, 10), number(20)))
        assertFalse(valid(ComparisonLevel.NORMAL, add, number(101)))
        assertTrue(valid(ComparisonLevel.NORMAL,
            expression(100, ComparisonOperation.SUBTRACTION, 2), number(98)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(101, ComparisonOperation.SUBTRACTION, 2), number(99)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(3, ComparisonOperation.SUBTRACTION, 2), number(2)))
        assertTrue(valid(ComparisonLevel.NORMAL,
            expression(9, ComparisonOperation.MULTIPLICATION, 9), number(81)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(10, ComparisonOperation.MULTIPLICATION, 2), number(20)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(1, ComparisonOperation.MULTIPLICATION, 9), number(9)))
        assertTrue(valid(ComparisonLevel.NORMAL,
            expression(18, ComparisonOperation.DIVISION, 9), number(18)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(18, ComparisonOperation.DIVISION, 9), number(19)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(10, ComparisonOperation.DIVISION, 1), number(10)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(20, ComparisonOperation.DIVISION, 10), number(2)))
    }

    @Test fun normalAndHardRejectZeroOperandsAndNonpositiveResults() {
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(0, ComparisonOperation.ADDITION, 20), number(20)))
        assertFalse(valid(ComparisonLevel.NORMAL,
            expression(2, ComparisonOperation.SUBTRACTION, 2), number(2)))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(0, ComparisonOperation.ADDITION, 100),
            expression(2, ComparisonOperation.MULTIPLICATION, 3)))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(2, ComparisonOperation.SUBTRACTION, 3),
            expression(2, ComparisonOperation.MULTIPLICATION, 3)))
        assertThrows(IllegalArgumentException::class.java) {
            expression(7, ComparisonOperation.DIVISION, 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            expression(7, ComparisonOperation.DIVISION, 0)
        }
    }

    @Test fun hardArithmeticGroupChecksNewBoundsWithoutApplyingThemToMixedPairs() {
        val add = expression(100, ComparisonOperation.ADDITION, 900)
        val subtract = expression(1000, ComparisonOperation.SUBTRACTION, 1)
        assertTrue(valid(ComparisonLevel.HARD, add, subtract))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(50, ComparisonOperation.ADDITION, 49), subtract))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(999, ComparisonOperation.ADDITION, 2), subtract))
        assertFalse(valid(ComparisonLevel.HARD, add,
            expression(99, ComparisonOperation.SUBTRACTION, 1)))
        assertFalse(valid(ComparisonLevel.HARD, add,
            expression(100, ComparisonOperation.SUBTRACTION, 1)))
        assertFalse(valid(ComparisonLevel.HARD, add,
            expression(1001, ComparisonOperation.SUBTRACTION, 2)))
        assertTrue(valid(ComparisonLevel.HARD,
            expression(2, ComparisonOperation.ADDITION, 3),
            expression(2, ComparisonOperation.MULTIPLICATION, 3)))
    }

    @Test fun hardMixedAndTabularPairsCheckTheirOwnBounds() {
        val multiply = expression(10, ComparisonOperation.MULTIPLICATION, 10)
        val divide = expression(100, ComparisonOperation.DIVISION, 10)
        assertTrue(valid(ComparisonLevel.HARD, multiply, divide))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(11, ComparisonOperation.MULTIPLICATION, 2), divide))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(10, ComparisonOperation.DIVISION, 1), multiply))
        assertTrue(valid(ComparisonLevel.HARD,
            expression(50, ComparisonOperation.ADDITION, 50), multiply))
        assertFalse(valid(ComparisonLevel.HARD,
            expression(50, ComparisonOperation.ADDITION, 51), multiply))
        assertTrue(valid(ComparisonLevel.HARD, divide,
            expression(15, ComparisonOperation.SUBTRACTION, 2)))
        assertFalse(valid(ComparisonLevel.HARD, divide,
            expression(22, ComparisonOperation.SUBTRACTION, 1)))
    }
}
