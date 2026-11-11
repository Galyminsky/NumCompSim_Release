package me.proton.jobforandroid.compositionofnumbers

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDifficulty
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.ComparisonDifficultyClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ComparisonDifficultyClassifierTest {
    private fun check(
        level: ComparisonLevel,
        first: Int,
        operation: ComparisonOperation,
        second: Int,
        expected: ComparisonDifficulty
    ) {
        assertEquals(expected, ComparisonDifficultyClassifier.classify(
            level, ComparisonSide.Expression(first, operation, second)
        ))
    }

    @Test fun normalAdditionUsesUnitsCarryOnly() {
        check(ComparisonLevel.NORMAL, 14, ComparisonOperation.ADDITION, 10, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.NORMAL, 15, ComparisonOperation.ADDITION, 5, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.NORMAL, 29, ComparisonOperation.ADDITION, 1, ComparisonDifficulty.COMPLEX)
    }

    @Test fun normalSubtractionUsesTensBorrow() {
        check(ComparisonLevel.NORMAL, 42, ComparisonOperation.SUBTRACTION, 2, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.NORMAL, 40, ComparisonOperation.SUBTRACTION, 9, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.NORMAL, 30, ComparisonOperation.SUBTRACTION, 1, ComparisonDifficulty.COMPLEX)
    }

    @Test fun hardAdditionDetectsCarryAtAnyPlace() {
        check(ComparisonLevel.HARD, 120, ComparisonOperation.ADDITION, 30, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.HARD, 109, ComparisonOperation.ADDITION, 1, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 190, ComparisonOperation.ADDITION, 20, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 900, ComparisonOperation.ADDITION, 100, ComparisonDifficulty.COMPLEX)
    }

    @Test fun hardSubtractionDetectsBorrowAtAnyPlace() {
        check(ComparisonLevel.HARD, 321, ComparisonOperation.SUBTRACTION, 110, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.HARD, 321, ComparisonOperation.SUBTRACTION, 112, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 310, ComparisonOperation.SUBTRACTION, 20, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 300, ComparisonOperation.SUBTRACTION, 1, ComparisonDifficulty.COMPLEX)
    }

    @Test fun multiplicationUsesBothFactorsAtLevelBounds() {
        check(ComparisonLevel.NORMAL, 6, ComparisonOperation.MULTIPLICATION, 9, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.NORMAL, 5, ComparisonOperation.MULTIPLICATION, 9, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.NORMAL, 9, ComparisonOperation.MULTIPLICATION, 5, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.HARD, 6, ComparisonOperation.MULTIPLICATION, 10, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 10, ComparisonOperation.MULTIPLICATION, 10, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 5, ComparisonOperation.MULTIPLICATION, 10, ComparisonDifficulty.ORDINARY)
    }

    @Test fun divisionUsesDivisorAndQuotientAtLevelBounds() {
        check(ComparisonLevel.NORMAL, 36, ComparisonOperation.DIVISION, 6, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.NORMAL, 81, ComparisonOperation.DIVISION, 9, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.NORMAL, 30, ComparisonOperation.DIVISION, 6, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.NORMAL, 60, ComparisonOperation.DIVISION, 10, ComparisonDifficulty.ORDINARY)
        check(ComparisonLevel.HARD, 60, ComparisonOperation.DIVISION, 10, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 100, ComparisonOperation.DIVISION, 10, ComparisonDifficulty.COMPLEX)
        check(ComparisonLevel.HARD, 50, ComparisonOperation.DIVISION, 10, ComparisonDifficulty.ORDINARY)
    }

    @Test fun testAndEasyHaveNoCalculationDifficultyRule() {
        val expression = ComparisonSide.Expression(2, ComparisonOperation.ADDITION, 3)
        for (level in listOf(ComparisonLevel.TEST, ComparisonLevel.EASY)) {
            assertThrows(IllegalArgumentException::class.java) {
                ComparisonDifficultyClassifier.classify(level, expression)
            }
        }
    }
}
