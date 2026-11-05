package me.proton.jobforandroid.compositionofnumbers.domain.entity

data class Question(
    val sum: Int,
    val visibleNumber: Int,
    val options: List<Int>,
    val operation: Operation = Operation.ADDITION,
    val unknownPosition: UnknownPosition = UnknownPosition.SECOND_OPERAND,
    val multiplicationSecondOperand: Int? = null,
    val divisionQuotient: Int? = null
) {
    enum class Operation { ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION }
    enum class UnknownPosition { SECOND_OPERAND, RESULT }

    init {
        require(operation != Operation.ADDITION || unknownPosition == UnknownPosition.SECOND_OPERAND)
        if (operation == Operation.MULTIPLICATION) {
            val second = requireNotNull(multiplicationSecondOperand)
            require(visibleNumber > 0 && second > 0)
            val product = visibleNumber.toLong() * second.toLong()
            require(product <= Int.MAX_VALUE && sum.toLong() == product)
        }
        if (operation == Operation.DIVISION) {
            val quotient = requireNotNull(divisionQuotient)
            require(sum > 0 && visibleNumber > 0 && quotient > 0)
            require(visibleNumber.toLong() * quotient.toLong() == sum.toLong())
        }
    }

    // For addition, sum is the result. For subtraction, sum is the minuend.
    val firstOperand: Int get() = if (operation == Operation.ADDITION || operation == Operation.MULTIPLICATION) visibleNumber else sum
    val secondOperand: Int get() = when {
        operation == Operation.ADDITION -> sum - visibleNumber
        operation == Operation.MULTIPLICATION -> requireNotNull(multiplicationSecondOperand)
        operation == Operation.DIVISION -> visibleNumber
        unknownPosition == UnknownPosition.RESULT -> visibleNumber
        else -> sum - visibleNumber
    }
    val result: Int get() = when {
        operation == Operation.ADDITION -> sum
        operation == Operation.MULTIPLICATION -> sum
        operation == Operation.DIVISION -> requireNotNull(divisionQuotient)
        unknownPosition == UnknownPosition.RESULT -> sum - visibleNumber
        else -> visibleNumber
    }
    val rightAnswer: Int get() = when (operation) {
        Operation.MULTIPLICATION, Operation.DIVISION ->
            if (unknownPosition == UnknownPosition.RESULT) result else secondOperand
        else -> sum - visibleNumber
    }
}
