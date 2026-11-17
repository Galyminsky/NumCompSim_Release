package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import kotlin.math.abs

enum class ComparisonDistance { CLOSE, MEDIUM, FAR }

object ComparisonDistanceClassifier {
    /** Returns null for TEST, equality, or a distance outside the level's categories. */
    fun classify(question: ComparisonQuestion): ComparisonDistance? {
        if (question.level == ComparisonLevel.TEST) return null
        val distance = abs(question.leftValue.toLong() - question.rightValue.toLong())
        if (distance == 0L) return null

        return when (question.level) {
            ComparisonLevel.TEST -> null
            ComparisonLevel.EASY -> category(distance, 5, 30, 99)
            ComparisonLevel.NORMAL -> {
                val expression = (question.left as? ComparisonSide.Expression)
                    ?: question.right as ComparisonSide.Expression
                when (expression.operation) {
                    ComparisonOperation.ADDITION, ComparisonOperation.SUBTRACTION ->
                        category(distance, 5, 15, 35)
                    ComparisonOperation.MULTIPLICATION -> category(distance, 4, 12, 25)
                    ComparisonOperation.DIVISION -> category(distance, 2, 5, 9)
                }
            }
            ComparisonLevel.HARD -> {
                val left = question.left as ComparisonSide.Expression
                val right = question.right as ComparisonSide.Expression
                val leftArithmetic = left.operation.isArithmetic()
                val rightArithmetic = right.operation.isArithmetic()
                when {
                    leftArithmetic && rightArithmetic -> category(distance, 10, 50, 200)
                    !leftArithmetic && !rightArithmetic -> category(distance, 5, 20, 80)
                    left.operation == ComparisonOperation.MULTIPLICATION ||
                        right.operation == ComparisonOperation.MULTIPLICATION ->
                        category(distance, 5, 15, 40)
                    else -> category(distance, 2, 6, 18)
                }
            }
        }
    }

    private fun category(distance: Long, closeMax: Int, mediumMax: Int, farMax: Int): ComparisonDistance? =
        when {
            distance <= closeMax -> ComparisonDistance.CLOSE
            distance <= mediumMax -> ComparisonDistance.MEDIUM
            distance <= farMax -> ComparisonDistance.FAR
            else -> null
        }

    private fun ComparisonOperation.isArithmetic(): Boolean =
        this == ComparisonOperation.ADDITION || this == ComparisonOperation.SUBTRACTION
}
