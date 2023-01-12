package me.proton.jobforandroid.compositionofnumbers.domain.usecases

import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository

class GenerateQuestionUseCase(

    private val repository: GameRepository
) {

    operator fun invoke(maxSumValue: Int): Question {
        return repository.generateQuestion(maxSumValue, COUNT_OF_OPTIONS)
    }

    private companion object {

        private const val COUNT_OF_OPTIONS = 6

    }
}

