package me.proton.jobforandroid.compositionofnumbers.data

import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository
import kotlin.random.Random

object GameRepositoryImpl: GameRepository  {

    override fun generateQuestion(maxSumValue: Int, countOfOptions: Int): Question {
        validateRange(maxSumValue, countOfOptions)

        val sum = Random.nextLong(maxSumValue.toLong() + 1).toInt()
        val visibleNumber = Random.nextLong(sum.toLong() + 1).toInt()
        val rightAnswer = sum - visibleNumber
        return Question(sum, visibleNumber, generateOptions(maxSumValue, countOfOptions, rightAnswer))
    }

    override fun generateSubtractionQuestion(maxValue: Int, countOfOptions: Int): Question {
        validateRange(maxValue, countOfOptions)
        val minuend = Random.nextLong(maxValue.toLong() + 1).toInt()
        val subtrahend = Random.nextLong(minuend.toLong() + 1).toInt()
        val difference = minuend - subtrahend
        val unknownPosition = if (Random.nextBoolean()) {
            Question.UnknownPosition.RESULT
        } else {
            Question.UnknownPosition.SECOND_OPERAND
        }
        val visibleNumber = if (unknownPosition == Question.UnknownPosition.RESULT) {
            subtrahend
        } else {
            difference
        }
        val rightAnswer = minuend - visibleNumber
        return Question(
            minuend, visibleNumber, generateOptions(maxValue, countOfOptions, rightAnswer),
            Question.Operation.SUBTRACTION, unknownPosition
        )
    }

    override fun generateMultiplicationQuestion(level: Level, countOfOptions: Int): Question {
        val maxFactor = when (level) {
            Level.TEST -> 3
            Level.EASY -> 5
            Level.NORMAL -> 10
            Level.HARD -> 12
        }
        val maxProduct = (maxFactor.toLong() * maxFactor.toLong()).also {
            require(it <= Int.MAX_VALUE)
        }.toInt()
        validateRange(maxProduct, countOfOptions)
        val first = Random.nextInt(1, maxFactor + 1)
        val second = Random.nextInt(1, maxFactor + 1)
        val product = (first.toLong() * second.toLong()).also {
            require(it <= Int.MAX_VALUE)
        }.toInt()
        val unknownPosition = if (Random.nextBoolean()) {
            Question.UnknownPosition.RESULT
        } else {
            Question.UnknownPosition.SECOND_OPERAND
        }
        val rightAnswer = if (unknownPosition == Question.UnknownPosition.RESULT) product else second
        return Question(
            product, first, generateOptions(maxProduct, countOfOptions, rightAnswer),
            Question.Operation.MULTIPLICATION, unknownPosition, second
        )
    }

    private fun validateRange(maxSumValue: Int, countOfOptions: Int) {
        require(maxSumValue >= 5) {
            "maxSumValue must be at least 5 to provide six unique options"
        }
        require(maxSumValue < Int.MAX_VALUE) {
            "maxSumValue must be less than Int.MAX_VALUE to avoid upper-bound overflow"
        }
        require(countOfOptions in 1..(maxSumValue + 1)) {
            "countOfOptions must be between 1 and maxSumValue + 1"
        }
    }

    private fun generateOptions(maxSumValue: Int, countOfOptions: Int, rightAnswer: Int): List<Int> {
        val selectedWrongIndices = HashSet<Long>()
        val wrongCandidateCount = maxSumValue.toLong()
        for (index in wrongCandidateCount - (countOfOptions - 1) until wrongCandidateCount) {
            val candidate = Random.nextLong(index + 1)
            selectedWrongIndices.add(if (candidate in selectedWrongIndices) index else candidate)
        }
        val options = selectedWrongIndices.map { index ->
            (if (index >= rightAnswer.toLong()) index + 1 else index).toInt()
        }.toMutableList()
        options.add(rightAnswer)
        options.shuffle()
        return options
    }

    override fun getGameSettings(level: Level): GameSettings {
        return when (level) {
            Level.TEST -> {
               GameSettings (
                   10,
                   3,
                   50,
                   8
                       )
            }
            Level.EASY -> {
                GameSettings (
                    10,
                    10,
                    70,
                    60
                )
            }
            Level.NORMAL -> {
                GameSettings (
                    20,
                    20,
                    80,
                    60
                )
            }
            Level.HARD -> {
                GameSettings (
                    30,
                    30,
                    90,
                    90
                )
            }
        }
    }

}
