package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonCanonicalKey
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ComparisonCanonicalKeyTest {
    private fun number(value: Int) = ComparisonSide.Number(value)
    private fun expression(a: Int, operation: ComparisonOperation, b: Int) =
        ComparisonSide.Expression(a, operation, b)

    @Test fun swappingSidesKeepsKeyEvenWhenCorrectSignChanges() {
        val first = ComparisonQuestion(ComparisonLevel.TEST, number(2), number(8))
        val swapped = ComparisonQuestion(ComparisonLevel.TEST, number(8), number(2))
        assertEquals(ComparisonSign.LESS, first.correctSign)
        assertEquals(ComparisonSign.GREATER, swapped.correctSign)
        assertEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(swapped))
        assertEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(first))
    }

    @Test fun additionAndMultiplicationOperandSwapsKeepKey() {
        val first = ComparisonQuestion(ComparisonLevel.HARD,
            expression(100, ComparisonOperation.ADDITION, 25),
            expression(7, ComparisonOperation.MULTIPLICATION, 8))
        val swappedOperandsAndSides = ComparisonQuestion(ComparisonLevel.HARD,
            expression(8, ComparisonOperation.MULTIPLICATION, 7),
            expression(25, ComparisonOperation.ADDITION, 100))
        assertEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(swappedOperandsAndSides))
    }

    @Test fun subtractionOperandOrderRemainsSignificant() {
        val first = ComparisonQuestion(ComparisonLevel.HARD,
            expression(8, ComparisonOperation.SUBTRACTION, 3),
            expression(3, ComparisonOperation.MULTIPLICATION, 4))
        val reversedSubtraction = ComparisonQuestion(ComparisonLevel.HARD,
            expression(3, ComparisonOperation.SUBTRACTION, 8),
            expression(3, ComparisonOperation.MULTIPLICATION, 4))
        assertNotEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(reversedSubtraction))
    }

    @Test fun divisionOperandOrderRemainsSignificant() {
        // The model permits signed expressions; this test checks key structure, not level validation.
        val first = ComparisonQuestion(ComparisonLevel.HARD,
            expression(-2, ComparisonOperation.DIVISION, 2),
            expression(3, ComparisonOperation.MULTIPLICATION, 4))
        val reversedDivision = ComparisonQuestion(ComparisonLevel.HARD,
            expression(2, ComparisonOperation.DIVISION, -2),
            expression(3, ComparisonOperation.MULTIPLICATION, 4))
        assertNotEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(reversedDivision))
    }

    @Test fun differentExpressionsWithEqualValuesKeepDifferentKeys() {
        val first = ComparisonQuestion(ComparisonLevel.NORMAL,
            expression(20, ComparisonOperation.ADDITION, 30), number(50))
        val second = ComparisonQuestion(ComparisonLevel.NORMAL,
            expression(10, ComparisonOperation.ADDITION, 40), number(50))
        assertEquals(first.leftValue, second.leftValue)
        assertEquals(ComparisonSign.EQUAL, first.correctSign)
        assertEquals(ComparisonSign.EQUAL, second.correctSign)
        assertNotEquals(ComparisonCanonicalKey.of(first), ComparisonCanonicalKey.of(second))
    }

    @Test fun operationRemainsPartOfStructure() {
        val addition = ComparisonQuestion(ComparisonLevel.HARD,
            expression(2, ComparisonOperation.ADDITION, 2),
            expression(2, ComparisonOperation.MULTIPLICATION, 2))
        val multiplication = ComparisonQuestion(ComparisonLevel.HARD,
            expression(2, ComparisonOperation.MULTIPLICATION, 2),
            expression(2, ComparisonOperation.MULTIPLICATION, 2))
        assertNotEquals(ComparisonCanonicalKey.of(addition), ComparisonCanonicalKey.of(multiplication))
    }
}
