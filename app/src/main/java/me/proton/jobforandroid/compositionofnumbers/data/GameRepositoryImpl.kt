package me.proton.jobforandroid.compositionofnumbers.data

import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository
import kotlin.random.Random

object GameRepositoryImpl: GameRepository  {

    override fun generateQuestion(maxSumValue: Int, countOfOptions: Int): Question {
        require(maxSumValue >= 5) {
            "maxSumValue must be at least 5 to provide six unique options"
        }
        require(maxSumValue < Int.MAX_VALUE) {
            "maxSumValue must be less than Int.MAX_VALUE to avoid upper-bound overflow"
        }
        require(countOfOptions in 1..(maxSumValue + 1)) {
            "countOfOptions must be between 1 and maxSumValue + 1"
        }

        val sum = Random.nextLong(maxSumValue.toLong() + 1).toInt()
        val visibleNumber = Random.nextLong(sum.toLong() + 1).toInt()
        val rightAnswer = sum - visibleNumber
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
        return Question(sum, visibleNumber, options)
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
