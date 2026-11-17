package me.proton.jobforandroid.compositionofnumbers.presentation

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository

class GameViewModelFactory(
    private val level: Level,
    private val application: Application,
    private val repository: GameRepository,
    private val operation: Question.Operation = Question.Operation.ADDITION
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
            return modelClass.cast(GameViewModel(application, level, operation, repository))
        }
        throw RuntimeException("Unknown view model class $modelClass")
    }
}
