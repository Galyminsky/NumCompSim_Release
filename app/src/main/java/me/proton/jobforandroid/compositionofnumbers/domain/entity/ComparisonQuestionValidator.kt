package me.proton.jobforandroid.compositionofnumbers.domain.entity

object ComparisonQuestionValidator {
    fun isValid(question: ComparisonQuestion): Boolean = when (question.level) {
        ComparisonLevel.TEST ->
            question.left is ComparisonSide.Number && question.right is ComparisonSide.Number &&
                question.leftValue in 0..10 && question.rightValue in 0..10
        ComparisonLevel.EASY ->
            question.left is ComparisonSide.Number && question.right is ComparisonSide.Number &&
                question.leftValue in 0..100 && question.rightValue in 0..100
        ComparisonLevel.NORMAL -> validateNormal(question)
        ComparisonLevel.HARD -> validateHard(question)
    }

    private fun validateNormal(question: ComparisonQuestion): Boolean {
        val expression = (question.left as? ComparisonSide.Expression)
            ?: (question.right as? ComparisonSide.Expression) ?: return false
        val number = (question.left as? ComparisonSide.Number)
            ?: (question.right as? ComparisonSide.Number) ?: return false
        if (!validExpression(expression, ComparisonLevel.NORMAL)) return false
        val numberRange = if (expression.operation == ComparisonOperation.DIVISION) 2..18 else 2..100
        return number.value in numberRange
    }

    private fun validateHard(question: ComparisonQuestion): Boolean {
        val left = question.left as? ComparisonSide.Expression ?: return false
        val right = question.right as? ComparisonSide.Expression ?: return false
        if (!validExpression(left, ComparisonLevel.HARD) ||
            !validExpression(right, ComparisonLevel.HARD)) return false

        val leftArithmetic = left.operation.isArithmetic()
        val rightArithmetic = right.operation.isArithmetic()
        return when {
            leftArithmetic && rightArithmetic ->
                validHardArithmetic(left) && validHardArithmetic(right)
            leftArithmetic != rightArithmetic -> {
                val arithmetic = if (leftArithmetic) left else right
                val tabular = if (leftArithmetic) right else left
                val allowed = if (tabular.operation == ComparisonOperation.MULTIPLICATION) 2..100 else 2..20
                arithmetic.value in allowed
            }
            else -> true
        }
    }

    private fun validExpression(expression: ComparisonSide.Expression, level: ComparisonLevel): Boolean {
        val first = expression.firstOperand
        val second = expression.secondOperand
        val value = expression.value
        if (first <= 0 || second <= 0 || value <= 0) return false
        return when (expression.operation) {
            ComparisonOperation.ADDITION -> when (level) {
                ComparisonLevel.NORMAL -> value in 20..100
                ComparisonLevel.HARD -> value <= 1000
                else -> false
            }
            ComparisonOperation.SUBTRACTION -> when (level) {
                ComparisonLevel.NORMAL -> first <= 100 && value >= 2
                ComparisonLevel.HARD -> first <= 1000
                else -> false
            }
            ComparisonOperation.MULTIPLICATION -> {
                val factors = if (level == ComparisonLevel.NORMAL) 2..9 else 2..10
                first in factors && second in factors
            }
            ComparisonOperation.DIVISION -> {
                val factors = if (level == ComparisonLevel.NORMAL) 2..9 else 2..10
                second in factors && value in factors && first % second == 0
            }
        }
    }

    private fun validHardArithmetic(expression: ComparisonSide.Expression): Boolean = when (expression.operation) {
        ComparisonOperation.ADDITION ->
            (expression.firstOperand in 100..999 || expression.secondOperand in 100..999) &&
                expression.value in 100..1000
        ComparisonOperation.SUBTRACTION ->
            expression.firstOperand in 100..1000 && expression.value in 100..999
        else -> false
    }

    private fun ComparisonOperation.isArithmetic(): Boolean =
        this == ComparisonOperation.ADDITION || this == ComparisonOperation.SUBTRACTION
}
