package me.proton.jobforandroid.compositionofnumbers.domain.entity

data class Question(
    val sum: Int,
    val visibleNumber: Int,
    val options: List<Int>,
    val operation: Operation = Operation.ADDITION,
    val unknownPosition: UnknownPosition = UnknownPosition.SECOND_OPERAND,
    val multiplicationSecondOperand: Int? = null
) {
    enum class Operation { ADDITION, SUBTRACTION, MULTIPLICATION }
    enum class UnknownPosition { SECOND_OPERAND, RESULT }

    init {
        require(operation != Operation.ADDITION || unknownPosition == UnknownPosition.SECOND_OPERAND)
        if (operation == Operation.MULTIPLICATION) {
            val second = requireNotNull(multiplicationSecondOperand)
            require(visibleNumber > 0 && second > 0)
            val product = visibleNumber.toLong() * second.toLong()
            require(product <= Int.MAX_VALUE && sum.toLong() == product)
        }
    }

    // For addition, sum is the result. For subtraction, sum is the minuend.
    val firstOperand: Int get() = if (operation == Operation.ADDITION || operation == Operation.MULTIPLICATION) visibleNumber else sum
    val secondOperand: Int get() = when {
        operation == Operation.ADDITION -> sum - visibleNumber
        operation == Operation.MULTIPLICATION -> requireNotNull(multiplicationSecondOperand)
        unknownPosition == UnknownPosition.RESULT -> visibleNumber
        else -> sum - visibleNumber
    }
    val result: Int get() = when {
        operation == Operation.ADDITION -> sum
        operation == Operation.MULTIPLICATION -> sum
        unknownPosition == UnknownPosition.RESULT -> sum - visibleNumber
        else -> visibleNumber
    }
    val rightAnswer: Int get() = if (operation == Operation.MULTIPLICATION) {
        if (unknownPosition == UnknownPosition.RESULT) result else secondOperand
    } else sum - visibleNumber
}
