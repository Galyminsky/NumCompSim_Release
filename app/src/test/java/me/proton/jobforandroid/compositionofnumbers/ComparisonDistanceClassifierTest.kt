package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistance
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDistanceClassifier
import org.junit.Assert.assertEquals
import org.junit.Test

class ComparisonDistanceClassifierTest {
    private fun number(value: Int) = ComparisonSide.Number(value)
    private fun expression(a: Int, operation: ComparisonOperation, b: Int) =
        ComparisonSide.Expression(a, operation, b)
    private fun assertCategory(expected: ComparisonDistance?, question: ComparisonQuestion) =
        assertEquals(expected, ComparisonDistanceClassifier.classify(question))

    private fun assertBounds(
        closeMax: Int,
        mediumMax: Int,
        farMax: Int,
        questionAtDistance: (Int) -> ComparisonQuestion
    ) {
        assertCategory(ComparisonDistance.CLOSE, questionAtDistance(1))
        assertCategory(ComparisonDistance.CLOSE, questionAtDistance(closeMax))
        assertCategory(ComparisonDistance.MEDIUM, questionAtDistance(closeMax + 1))
        assertCategory(ComparisonDistance.MEDIUM, questionAtDistance(mediumMax))
        assertCategory(ComparisonDistance.FAR, questionAtDistance(mediumMax + 1))
        assertCategory(ComparisonDistance.FAR, questionAtDistance(farMax))
        assertCategory(null, questionAtDistance(farMax + 1))
    }

    @Test fun testHasNoCategoriesAndEqualityHasNoCategory() {
        assertCategory(null, ComparisonQuestion(ComparisonLevel.TEST, number(0), number(10)))
        assertCategory(null, ComparisonQuestion(ComparisonLevel.EASY, number(7), number(7)))
        assertCategory(null, ComparisonQuestion(ComparisonLevel.NORMAL,
            expression(20, ComparisonOperation.ADDITION, 10), number(30)))
        assertCategory(null, ComparisonQuestion(ComparisonLevel.HARD,
            expression(100, ComparisonOperation.ADDITION, 2),
            expression(105, ComparisonOperation.SUBTRACTION, 3)))
    }

    @Test fun easyChecksEveryBoundaryAndOutsideRange() {
        assertBounds(5, 30, 99) { distance ->
            ComparisonQuestion(ComparisonLevel.EASY, number(0), number(distance))
        }
        assertCategory(ComparisonDistance.CLOSE,
            ComparisonQuestion(ComparisonLevel.EASY, number(8), number(3)))
    }

    @Test fun normalAdditionAndSubtractionUseArithmeticBoundaries() {
        for (expression in listOf(
            expression(20, ComparisonOperation.ADDITION, 30),
            expression(80, ComparisonOperation.SUBTRACTION, 30)
        )) {
            assertBounds(5, 15, 35) { distance ->
                ComparisonQuestion(ComparisonLevel.NORMAL, expression, number(50 + distance))
            }
        }
    }

    @Test fun normalMultiplicationAndDivisionUseOwnBoundaries() {
        assertBounds(4, 12, 25) { distance ->
            ComparisonQuestion(ComparisonLevel.NORMAL,
                number(36 + distance), expression(6, ComparisonOperation.MULTIPLICATION, 6))
        }
        assertBounds(2, 5, 9) { distance ->
            ComparisonQuestion(ComparisonLevel.NORMAL,
                expression(18, ComparisonOperation.DIVISION, 9), number(2 + distance))
        }
    }

    @Test fun hardArithmeticGroupChecksAllBoundariesIncludingSubtraction() {
        assertBounds(10, 50, 200) { distance ->
            ComparisonQuestion(ComparisonLevel.HARD,
                expression(100, ComparisonOperation.ADDITION, 1),
                expression(100, ComparisonOperation.ADDITION, 1 + distance))
        }
        assertBounds(10, 50, 200) { distance ->
            ComparisonQuestion(ComparisonLevel.HARD,
                expression(500, ComparisonOperation.SUBTRACTION, 200),
                expression(100, ComparisonOperation.ADDITION, 200 + distance))
        }
    }

    @Test fun hardTabularGroupChecksAllBoundariesAndUnreachableDistance() {
        val pairs = listOf(
            Triple(1,  ComparisonDistance.CLOSE,  36 to 35),
            Triple(5,  ComparisonDistance.CLOSE,  25 to 30),
            Triple(6,  ComparisonDistance.MEDIUM, 36 to 30),
            Triple(20, ComparisonDistance.MEDIUM, 100 to 80),
            Triple(21, ComparisonDistance.FAR,    49 to 70),
            Triple(80, ComparisonDistance.FAR,   100 to 20),
            Triple(81, null,                     90 to 9)
        )
        val factors = mapOf(
            36 to (6 to 6), 35 to (5 to 7), 25 to (5 to 5),
            30 to (5 to 6), 100 to (10 to 10), 80 to (8 to 10),
            49 to (7 to 7), 70 to (7 to 10), 20 to (4 to 5),
            90 to (9 to 10), 9 to (3 to 3)
        )
        for ((distance, expected, values) in pairs) {
            val (leftA, leftB) = requireNotNull(factors[values.first])
            val (rightA, rightB) = requireNotNull(factors[values.second])
            val question = ComparisonQuestion(ComparisonLevel.HARD,
                expression(leftA, ComparisonOperation.MULTIPLICATION, leftB),
                expression(rightA, ComparisonOperation.MULTIPLICATION, rightB))
            assertEquals(distance, kotlin.math.abs(question.leftValue - question.rightValue))
            assertCategory(expected, question)
        }
        assertCategory(ComparisonDistance.CLOSE, ComparisonQuestion(ComparisonLevel.HARD,
            expression(100, ComparisonOperation.DIVISION, 10),
            expression(2, ComparisonOperation.MULTIPLICATION, 4)))
        assertCategory(ComparisonDistance.MEDIUM, ComparisonQuestion(ComparisonLevel.HARD,
            expression(4, ComparisonOperation.DIVISION, 2),
            expression(100, ComparisonOperation.DIVISION, 10)))
    }

    @Test fun hardMixedMultiplicationAndDivisionUseDifferentBoundaries() {
        assertBounds(5, 15, 40) { distance ->
            val target = 50 + distance
            ComparisonQuestion(ComparisonLevel.HARD,
                expression(target - 2, ComparisonOperation.ADDITION, 2),
                expression(5, ComparisonOperation.MULTIPLICATION, 10))
        }
        assertBounds(2, 6, 18) { distance ->
            val target = 2 + distance
            ComparisonQuestion(ComparisonLevel.HARD,
                expression(4, ComparisonOperation.DIVISION, 2),
                expression(target - 1, ComparisonOperation.ADDITION, 1))
        }
        assertCategory(ComparisonDistance.MEDIUM, ComparisonQuestion(ComparisonLevel.HARD,
            expression(16, ComparisonOperation.SUBTRACTION, 10),
            expression(4, ComparisonOperation.DIVISION, 2)))
    }
}
