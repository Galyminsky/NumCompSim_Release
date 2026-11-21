package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Fraction
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionMathValidator as Validator
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FractionMathValidatorTest {
    @Test fun testFractionChecksOriginalBoundsAndProperness() {
        assertTrue(Validator.isValidTestFraction(Fraction(1, 2)))
        assertTrue(Validator.isValidTestFraction(Fraction(6, 7)))
        listOf(Fraction(0, 2), Fraction(-1, 2), Fraction(2, 2), Fraction(3, 2),
            Fraction(1, 1), Fraction(1, 8), Fraction(2, 8), Fraction(-1, -2)).forEach {
            assertFalse("TEST source: $it", Validator.isValidTestFraction(it))
        }
    }

    @Test fun testPartsAndFractionAnswerUseMathematicalValue() {
        assertTrue(Validator.isValidTestParts(1, 2))
        assertTrue(Validator.isValidTestParts(6, 7))
        listOf(0 to 7, -1 to 7, 7 to 7, 8 to 7, 1 to 1, 1 to 8, 1 to 0).forEach { (shaded, total) ->
            assertFalse(Validator.isValidTestParts(shaded, total))
        }
        assertTrue(Validator.isCorrectTestAnswer(2, 4, Fraction(1, 2)))
        assertTrue(Validator.isCorrectTestAnswer(2, 4, Fraction(2, 4)))
        assertFalse(Validator.isCorrectTestAnswer(2, 4, Fraction(1, 4)))
        assertFalse(Validator.isCorrectTestAnswer(1, 0, Fraction(1, 2)))
    }

    @Test fun easyChecksSourceBoundsAndAnswersFromOriginalRecord() {
        assertTrue(Validator.isValidEasyFraction(Fraction(1, 2)))
        assertTrue(Validator.isValidEasyFraction(Fraction(9, 10)))
        listOf(Fraction(0, 2), Fraction(-1, 2), Fraction(2, 2), Fraction(3, 2),
            Fraction(1, 1), Fraction(1, 11), Fraction(2, 12), Fraction(1, -2)).forEach {
            assertFalse("EASY source: $it", Validator.isValidEasyFraction(it))
        }
        val source = Fraction(6, 8)
        assertTrue(Validator.isCorrectEasyNumerator(source, 6))
        assertTrue(Validator.isCorrectEasyDenominator(source, 8))
        assertFalse(Validator.isCorrectEasyNumerator(source, 3))
        assertFalse(Validator.isCorrectEasyDenominator(source, 4))
        assertFalse(Validator.isCorrectEasyNumerator(Fraction(0, 8), 0))
        assertFalse(Validator.isCorrectEasyDenominator(Fraction(1, 11), 11))
    }

    @Test fun normalComparisonAllowsEquivalentFractionsAndAllThreeSigns() {
        assertTrue(Validator.isCorrectNormalComparison(Fraction(2, 4), Fraction(1, 2), 0))
        assertTrue(Validator.isCorrectNormalComparison(Fraction(1, 3), Fraction(1, 2), -1))
        assertTrue(Validator.isCorrectNormalComparison(Fraction(3, 4), Fraction(2, 3), 1))
        assertFalse(Validator.isCorrectNormalComparison(Fraction(2, 4), Fraction(1, 2), 1))
        assertFalse(Validator.isCorrectNormalComparison(Fraction(1, 3), Fraction(1, 2), -2))
        // The LCM cap applies to arithmetic, not comparison.
        assertTrue(Validator.isValidNormalComparison(Fraction(1, 11), Fraction(11, 12)))
        assertTrue(Validator.isValidNormalComparison(Fraction(1, 2), Fraction(11, 12)))
        listOf(Fraction(0, 12), Fraction(-1, 12), Fraction(12, 12), Fraction(13, 12),
            Fraction(1, 1), Fraction(1, 13), Fraction(2, 14), Fraction(-1, -2)).forEach {
            assertFalse(Validator.isValidNormalComparison(it, Fraction(1, 2)))
            assertFalse(Validator.isValidNormalComparison(Fraction(1, 2), it))
        }
    }

    @Test fun normalReductionRequiresReducibleSourceAndFullyReducedAnswer() {
        assertTrue(Validator.isValidNormalReduction(Fraction(6, 8), Fraction(3, 4)))
        assertTrue(Validator.isValidNormalReduction(Fraction(10, 12), Fraction(5, 6)))
        assertFalse(Validator.isValidNormalReduction(Fraction(6, 12), Fraction(2, 4)))
        assertFalse(Validator.isValidNormalReduction(Fraction(6, 8), Fraction(6, 8)))
        assertFalse(Validator.isValidNormalReduction(Fraction(6, 8), Fraction(1, 2)))
        assertFalse(Validator.isValidNormalReduction(Fraction(1, 2), Fraction(1, 2)))
        assertFalse(Validator.isValidNormalReduction(Fraction(2, 14), Fraction(1, 7)))
        assertFalse(Validator.isValidNormalReduction(Fraction(0, 12), Fraction(0, 1)))
        assertFalse(Validator.isValidNormalReduction(Fraction(-6, 8), Fraction(-3, 4)))
        assertFalse(Validator.isValidNormalReduction(Fraction(12, 12), Fraction(1, 1)))
    }

    @Test fun normalArithmeticAllowsSameDenominatorsAndLcmExactly24() {
        assertTrue(Validator.isValidNormalAddition(Fraction(1, 4), Fraction(1, 4), Fraction(1, 2)))
        assertTrue(Validator.isValidNormalSubtraction(Fraction(3, 4), Fraction(1, 4), Fraction(1, 2)))
        assertTrue(Validator.isValidNormalAddition(Fraction(1, 3), Fraction(1, 8), Fraction(11, 24)))
        assertTrue(Validator.isValidNormalSubtraction(Fraction(1, 3), Fraction(1, 8), Fraction(5, 24)))
        assertTrue(Validator.isValidNormalAddition(Fraction(1, 12), Fraction(1, 12), Fraction(1, 6)))
        assertTrue(Validator.isValidNormalAddition(Fraction(1, 2), Fraction(1, 12), Fraction(7, 12)))
        assertFalse(Validator.isValidNormalAddition(Fraction(1, 5), Fraction(1, 6), Fraction(11, 30)))
        assertFalse(Validator.isValidNormalSubtraction(Fraction(1, 5), Fraction(1, 6), Fraction(1, 30)))
    }

    @Test fun normalArithmeticRejectsInvalidOperandsAndNoncanonicalOrWrongAnswers() {
        val half = Fraction(1, 2)
        listOf(Fraction(0, 4), Fraction(-1, 4), Fraction(4, 4), Fraction(5, 4),
            Fraction(1, 1), Fraction(1, 13), Fraction(1, -4), Fraction(1, Int.MIN_VALUE)).forEach {
            assertFalse(Validator.isValidNormalAddition(it, half, Fraction(3, 4)))
            assertFalse(Validator.isValidNormalAddition(half, it, Fraction(3, 4)))
            assertFalse(Validator.isValidNormalSubtraction(it, half, Fraction(1, 4)))
            assertFalse(Validator.isValidNormalSubtraction(half, it, Fraction(1, 4)))
        }
        assertFalse(Validator.isValidNormalAddition(Fraction(1, 4), Fraction(1, 4), Fraction(2, 4)))
        assertFalse(Validator.isValidNormalSubtraction(Fraction(3, 4), Fraction(1, 4), Fraction(2, 4)))
        assertFalse(Validator.isValidNormalAddition(half, Fraction(1, 4), Fraction(1, 2)))
        assertFalse(Validator.isValidNormalSubtraction(half, Fraction(1, 4), Fraction(1, 3)))
    }

    @Test fun normalArithmeticRejectsZeroNegativeIntegerAndImproperResults() {
        assertFalse(Validator.isValidNormalAddition(Fraction(1, 2), Fraction(1, 2), Fraction(1, 1)))
        assertFalse(Validator.isValidNormalAddition(Fraction(3, 4), Fraction(1, 2), Fraction(5, 4)))
        assertFalse(Validator.isValidNormalSubtraction(Fraction(1, 2), Fraction(2, 4), Fraction(0, 1)))
        assertFalse(Validator.isValidNormalSubtraction(Fraction(1, 3), Fraction(1, 2), Fraction(-1, 6)))
    }

    @Test fun hardArithmeticAllowsLcmExactly60AndImproperNonintegerSum() {
        assertTrue(Validator.isValidHardAddition(Fraction(14, 15), Fraction(1, 12), Fraction(61, 60)))
        assertTrue(Validator.isValidHardSubtraction(Fraction(14, 15), Fraction(1, 12), Fraction(17, 20)))
        assertTrue(Validator.isValidHardAddition(Fraction(1, 2), Fraction(1, 3), Fraction(5, 6)))
        assertTrue(Validator.isValidHardSubtraction(Fraction(3, 4), Fraction(1, 2), Fraction(1, 4)))
        assertTrue(Validator.isValidHardAddition(Fraction(3, 4), Fraction(2, 3), Fraction(17, 12)))
        assertFalse(Validator.isValidHardAddition(Fraction(1, 7), Fraction(1, 9), Fraction(16, 63)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(1, 7), Fraction(1, 9), Fraction(2, 63)))
    }

    @Test fun hardArithmeticRequiresProperBoundedOperandsAndDifferentDenominators() {
        assertFalse(Validator.isValidHardAddition(Fraction(1, 4), Fraction(1, 4), Fraction(1, 2)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(3, 4), Fraction(1, 4), Fraction(1, 2)))
        val half = Fraction(1, 2)
        listOf(Fraction(0, 3), Fraction(-1, 3), Fraction(3, 3), Fraction(4, 3),
            Fraction(1, 1), Fraction(1, 16), Fraction(2, 16), Fraction(1, -3),
            Fraction(Int.MAX_VALUE, 3)).forEach {
            assertFalse(Validator.isValidHardAddition(it, half, Fraction(5, 6)))
            assertFalse(Validator.isValidHardAddition(half, it, Fraction(5, 6)))
            assertFalse(Validator.isValidHardSubtraction(it, half, Fraction(1, 6)))
            assertFalse(Validator.isValidHardSubtraction(half, it, Fraction(1, 6)))
        }
    }

    @Test fun hardArithmeticRejectsIntegerZeroNegativeUnreducedAndWrongResults() {
        assertFalse(Validator.isValidHardAddition(Fraction(1, 2), Fraction(2, 4), Fraction(1, 1)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(1, 2), Fraction(2, 4), Fraction(0, 1)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(1, 3), Fraction(1, 2), Fraction(-1, 6)))
        assertFalse(Validator.isValidHardAddition(Fraction(1, 2), Fraction(1, 3), Fraction(10, 12)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(3, 4), Fraction(1, 2), Fraction(2, 8)))
        assertFalse(Validator.isValidHardAddition(Fraction(1, 2), Fraction(1, 3), Fraction(1, 6)))
        assertFalse(Validator.isValidHardSubtraction(Fraction(3, 4), Fraction(1, 2), Fraction(1, 3)))
    }

    @Test fun hardConversionAllowsWholePartsOneThroughThreeAndReducibleSources() {
        assertTrue(Validator.isValidHardConversion(Fraction(3, 2), 1, Fraction(1, 2)))
        assertTrue(Validator.isValidHardConversion(Fraction(7, 3), 2, Fraction(1, 3)))
        assertTrue(Validator.isValidHardConversion(Fraction(59, 15), 3, Fraction(14, 15)))
        assertTrue(Validator.isValidHardConversion(Fraction(14, 4), 3, Fraction(1, 2)))
    }

    @Test fun hardConversionRejectsInvalidSourceWholePartAndFractionalPart() {
        assertFalse(Validator.isValidHardConversion(Fraction(1, 2), 0, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(9, 2), 4, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 2), 2, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(4, 2), 2, Fraction(0, 1)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 1), 2, Fraction(1, 1)))
        assertFalse(Validator.isValidHardConversion(Fraction(17, 16), 1, Fraction(1, 16)))
        assertFalse(Validator.isValidHardConversion(Fraction(0, 2), 1, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(-3, 2), 1, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(-3, -2), 1, Fraction(1, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(14, 4), 3, Fraction(2, 4)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 2), 1, Fraction(1, 3)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 2), 1, Fraction(0, 1)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 2), 1, Fraction(3, 2)))
        assertFalse(Validator.isValidHardConversion(Fraction(3, 2), 1, Fraction(-1, -2)))
        assertFalse(Validator.isValidHardConversion(Fraction(Int.MAX_VALUE, 2), 3, Fraction(1, 2)))
    }
}
