package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FractionTest {
    @Test fun constructionPreservesOriginalRepresentation() {
        val fraction = Fraction(6, 8)
        assertEquals(6, fraction.numerator)
        assertEquals(8, fraction.denominator)
        assertEquals(Fraction(6, 8), fraction)
        assertNotEquals(Fraction(3, 4), fraction)
        assertEquals(-6, Fraction(-6, -8).numerator)
        assertEquals(-8, Fraction(-6, -8).denominator)
    }

    @Test fun reductionIsExactAndDoesNotMutateOriginal() {
        val original = Fraction(6, 8)
        val reduced = original.reduced()
        assertEquals(Fraction(3, 4), reduced)
        assertEquals(reduced, reduced.reduced())
        assertEquals(Fraction(6, 8), original)
        assertEquals(Fraction(-3, 4), Fraction(6, -8).reduced())
        assertEquals(Fraction(3, 4), Fraction(-6, -8).reduced())
        assertEquals(Fraction(0, 1), Fraction(0, -8).reduced())
    }

    @Test fun mathematicalEqualityIsIndependentOfRepresentation() {
        val first = Fraction(2, 4)
        val second = Fraction(1, 2)
        assertTrue(first.isEquivalentTo(second))
        assertTrue(second.isEquivalentTo(first))
        assertEquals(0, first.compareTo(second))
        assertNotEquals(first, second)
        assertTrue(Fraction(-2, -4).isEquivalentTo(second))
        assertFalse(Fraction(-2, 4).isEquivalentTo(second))
        assertTrue(Fraction(0, 7).isEquivalentTo(Fraction(0, -3)))
    }

    @Test fun comparisonOrdersDifferentValuesExactly() {
        assertTrue(Fraction(1, 3) < Fraction(1, 2))
        assertTrue(Fraction(3, 4) > Fraction(2, 3))
        assertTrue(Fraction(-3, 4) < Fraction(-2, 3))
        assertTrue(Fraction(1, -2) < Fraction(1, 3))
        assertTrue(Fraction(-1, -2) > Fraction(-1, 3))
        assertTrue(Fraction(1, -3) > Fraction(1, -2))
    }

    @Test fun additionHandlesEqualAndDifferentDenominators() {
        assertEquals(Fraction(1, 2), Fraction(1, 4) + Fraction(1, 4))
        assertEquals(Fraction(5, 6), Fraction(1, 2) + Fraction(1, 3))
        assertEquals(Fraction(3, 4), Fraction(1, 2) + Fraction(1, 4))
        assertEquals(Fraction(0, 1), Fraction(1, -2) + Fraction(1, 2))
        assertEquals(Fraction(-1, 6), Fraction(1, -2) + Fraction(1, 3))
    }

    @Test fun subtractionHandlesEqualAndDifferentDenominators() {
        assertEquals(Fraction(1, 2), Fraction(3, 4) - Fraction(1, 4))
        assertEquals(Fraction(1, 6), Fraction(1, 2) - Fraction(1, 3))
        assertEquals(Fraction(1, 2), Fraction(5, 6) - Fraction(1, 3))
        assertEquals(Fraction(0, 1), Fraction(2, 4) - Fraction(1, 2))
        assertEquals(Fraction(-1, 6), Fraction(1, 3) - Fraction(1, 2))
        assertEquals(Fraction(5, 6), Fraction(1, 3) - Fraction(1, -2))
    }

    @Test fun improperFractionsRemainFractions() {
        assertEquals(Fraction(5, 4), Fraction(1, 2) + Fraction(3, 4))
        assertEquals(Fraction(5, 4), Fraction(10, 8).reduced())
        assertEquals(Fraction(7, 4), Fraction(9, 4) - Fraction(1, 2))
        assertTrue(Fraction(5, 4) > Fraction(1, 1))
        assertEquals(Fraction(2, 1), Fraction(7, 4) + Fraction(1, 4))
    }

    @Test fun zeroDenominatorIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { Fraction(1, 0) }
        assertThrows(IllegalArgumentException::class.java) { Fraction(0, 0) }
    }

    @Test fun comparisonHandlesIntBoundariesWithoutOverflow() {
        val max = Int.MAX_VALUE
        val min = Int.MIN_VALUE
        assertTrue(Fraction(max, max - 1) > Fraction(max - 1, max))
        assertTrue(Fraction(min, 1) < Fraction(max, 1))
        assertTrue(Fraction(1, min) > Fraction(-1, max))
        assertTrue(Fraction(min, min).isEquivalentTo(Fraction(1, 1)))
        assertTrue(Fraction(min, -1) > Fraction(max, 1))
    }

    @Test fun reductionHandlesIntMinValueWithoutAbsOverflow() {
        assertEquals(Fraction(1, 1), Fraction(Int.MIN_VALUE, Int.MIN_VALUE).reduced())
        assertEquals(Fraction(0, 1), Fraction(0, Int.MIN_VALUE).reduced())
        assertEquals(Fraction(-1, 1073741824), Fraction(2, Int.MIN_VALUE).reduced())
        assertEquals(Fraction(Int.MIN_VALUE, 1), Fraction(Int.MIN_VALUE, 1).reduced())
        assertThrows(ArithmeticException::class.java) { Fraction(Int.MIN_VALUE, -1).reduced() }
        assertThrows(ArithmeticException::class.java) { Fraction(1, Int.MIN_VALUE).reduced() }
    }

    @Test fun arithmeticReducesWideIntermediateValuesBeforeCheckingIntBounds() {
        val max = Int.MAX_VALUE
        assertEquals(Fraction(max, 1), Fraction(max, 2) + Fraction(max, 2))
        assertEquals(Fraction(1, 1), Fraction(1, max) + Fraction(max - 1, max))
        assertEquals(Fraction(0, 1), Fraction(1, Int.MIN_VALUE) - Fraction(1, Int.MIN_VALUE))
        assertEquals(Fraction(Int.MIN_VALUE, 1), Fraction(Int.MIN_VALUE, 2) + Fraction(Int.MIN_VALUE, 2))
        assertEquals(Fraction(2, 1), Fraction(Int.MIN_VALUE, Int.MIN_VALUE) + Fraction(Int.MIN_VALUE, Int.MIN_VALUE))
        assertEquals(Fraction(0, 1), Fraction(Int.MIN_VALUE, -1) + Fraction(Int.MIN_VALUE, 1))
        assertEquals(Fraction(-1, 1073741824), Fraction(1, Int.MIN_VALUE) + Fraction(1, Int.MIN_VALUE))
    }

    @Test fun unrepresentableArithmeticResultsThrowInsteadOfWrapping() {
        assertThrows(ArithmeticException::class.java) { Fraction(Int.MAX_VALUE, 1) + Fraction(1, 1) }
        assertThrows(ArithmeticException::class.java) { Fraction(Int.MIN_VALUE, 1) - Fraction(1, 1) }
        assertThrows(ArithmeticException::class.java) { Fraction(Int.MIN_VALUE, 1) + Fraction(-1, 1) }
        assertThrows(ArithmeticException::class.java) { Fraction(Int.MAX_VALUE, 1) - Fraction(-1, 1) }
        assertThrows(ArithmeticException::class.java) {
            Fraction(1, Int.MAX_VALUE) + Fraction(1, Int.MAX_VALUE - 1)
        }
        assertThrows(ArithmeticException::class.java) {
            Fraction(1, Int.MAX_VALUE) - Fraction(1, Int.MAX_VALUE - 1)
        }
    }
}
