package me.proton.jobforandroid.compositionofnumbers.domain.entity

data class Question(
    val sum: Int,
    val visibleNumber: Int,
    val options: List<Int>,
    val operation: Operation = Operation.ADDITION,
    val unknownPosition: UnknownPosition = UnknownPosition.SECOND_OPERAND
) {
    enum class Operation { ADDITION, SUBTRACTION }
    enum class UnknownPosition { SECOND_OPERAND, RESULT }

    init {
        require(operation == Operation.SUBTRACTION || unknownPosition == UnknownPosition.SECOND_OPERAND)
    }

    // For addition, sum is the result. For subtraction, sum is the minuend.
    val firstOperand: Int get() = if (operation == Operation.ADDITION) visibleNumber else sum
    val secondOperand: Int get() = when {
        operation == Operation.ADDITION -> sum - visibleNumber
        unknownPosition == UnknownPosition.RESULT -> visibleNumber
        else -> sum - visibleNumber
    }
    val result: Int get() = when {
        operation == Operation.ADDITION -> sum
        unknownPosition == UnknownPosition.RESULT -> sum - visibleNumber
        else -> visibleNumber
    }
    val rightAnswer: Int get() = sum - visibleNumber
}
