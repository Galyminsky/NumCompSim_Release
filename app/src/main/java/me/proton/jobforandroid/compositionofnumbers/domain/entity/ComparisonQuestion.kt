package me.proton.jobforandroid.compositionofnumbers.domain.entity

enum class ComparisonLevel { TEST, EASY, NORMAL, HARD }

enum class ComparisonOperation { ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION }

enum class ComparisonSign { LESS, EQUAL, GREATER }

sealed class ComparisonSide {
    abstract val value: Int

    data class Number(override val value: Int) : ComparisonSide()

    data class Expression(
        val firstOperand: Int,
        val operation: ComparisonOperation,
        val secondOperand: Int
    ) : ComparisonSide() {
        override val value: Int = run {
            val first = firstOperand.toLong()
            val second = secondOperand.toLong()
            val result = when (operation) {
                ComparisonOperation.ADDITION -> first + second
                ComparisonOperation.SUBTRACTION -> first - second
                ComparisonOperation.MULTIPLICATION -> first * second
                ComparisonOperation.DIVISION -> {
                    require(second != 0L && first % second == 0L) {
                        "Division must have a nonzero divisor and an integer result"
                    }
                    first / second
                }
            }
            require(result in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                "Expression result must fit in Int"
            }
            result.toInt()
        }
    }
}

data class ComparisonQuestion(
    val level: ComparisonLevel,
    val left: ComparisonSide,
    val right: ComparisonSide
) {
    init {
        require(when (level) {
            ComparisonLevel.TEST, ComparisonLevel.EASY ->
                left is ComparisonSide.Number && right is ComparisonSide.Number
            ComparisonLevel.NORMAL ->
                (left is ComparisonSide.Expression) != (right is ComparisonSide.Expression)
            ComparisonLevel.HARD ->
                left is ComparisonSide.Expression && right is ComparisonSide.Expression
        }) { "Comparison sides do not match the level format" }
    }

    val leftValue: Int get() = left.value
    val rightValue: Int get() = right.value
    val correctSign: ComparisonSign get() = when {
        leftValue < rightValue -> ComparisonSign.LESS
        leftValue > rightValue -> ComparisonSign.GREATER
        else -> ComparisonSign.EQUAL
    }
}
