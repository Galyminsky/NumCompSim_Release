package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository

class GenerateQuestionUseCase(
    private val repository: GameRepository
) {
    operator fun invoke(maxSumValue: Int): Question {
        return repository.generateQuestion(maxSumValue, COUNT_OF_OPTIONS)
    }
    fun generateSubtraction(level: Level): Question {
        val maxValue = repository.getGameSettings(level).maxSumValue
        return repository.generateSubtractionQuestion(maxValue, COUNT_OF_OPTIONS)
    }
    fun generateMultiplication(level: Level): Question {
        return repository.generateMultiplicationQuestion(level, COUNT_OF_OPTIONS)
    }
    private companion object {
        private const val COUNT_OF_OPTIONS = 6
    }
}

