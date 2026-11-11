package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide

enum class ComparisonDifficulty { ORDINARY, COMPLEX }

object ComparisonDifficultyClassifier {
    fun classify(
        level: ComparisonLevel,
        expression: ComparisonSide.Expression
    ): ComparisonDifficulty {
        require(level == ComparisonLevel.NORMAL || level == ComparisonLevel.HARD) {
            "Calculation difficulty is defined only for NORMAL and HARD"
        }
        val first = expression.firstOperand
        val second = expression.secondOperand
        require(first >= 0 && second >= 0) { "Operands must be nonnegative" }

        val complex = when (expression.operation) {
            ComparisonOperation.ADDITION -> if (level == ComparisonLevel.NORMAL) {
                first % 10 + second % 10 >= 10
            } else {
                hasCarry(first, second)
            }
            ComparisonOperation.SUBTRACTION -> if (level == ComparisonLevel.NORMAL) {
                first % 10 < second % 10
            } else {
                hasBorrow(first, second)
            }
            ComparisonOperation.MULTIPLICATION -> {
                val factors = if (level == ComparisonLevel.NORMAL) 6..9 else 6..10
                first in factors && second in factors
            }
            ComparisonOperation.DIVISION -> {
                val factors = if (level == ComparisonLevel.NORMAL) 6..9 else 6..10
                second in factors && expression.value in factors
            }
        }
        return if (complex) ComparisonDifficulty.COMPLEX else ComparisonDifficulty.ORDINARY
    }

    private fun hasCarry(first: Int, second: Int): Boolean {
        var a = first
        var b = second
        while (a > 0 || b > 0) {
            if (a % 10 + b % 10 >= 10) return true
            a /= 10
            b /= 10
        }
        return false
    }

    private fun hasBorrow(first: Int, second: Int): Boolean {
        var a = first
        var b = second
        while (a > 0 || b > 0) {
            if (a % 10 < b % 10) return true
            a /= 10
            b /= 10
        }
        return false
    }
}
