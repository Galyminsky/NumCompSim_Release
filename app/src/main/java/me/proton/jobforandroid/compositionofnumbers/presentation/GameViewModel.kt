package me.proton.jobforandroid.compositionofnumbers.presentation

import android.app.Application
import android.os.CountDownTimer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GenerateQuestionUseCase
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GetGameSettingsUseCase

class GameViewModel(
    private val application: Application,
    val level: Level
) : ViewModel() {

    private lateinit var gameSettings: GameSettings
    private val repository = GameRepositoryImpl
    private val generateQuestionUseCase = GenerateQuestionUseCase(repository)
    private val getGameSettingsUseCase = GetGameSettingsUseCase(repository)
    private var timer: CountDownTimer? = null
    private val _formattedTime = MutableLiveData<String>()
    val formattedTime: LiveData<String>
        get() = _formattedTime
    private val _question = MutableLiveData<Question>()
    val question: LiveData<Question>
        get() = _question
    private val _percentOfRightAnswers = MutableLiveData<Int>()
    val percentOfRightAnswers: LiveData<Int>
        get() = _percentOfRightAnswers
    private val _progressAnswers = MutableLiveData<String>()
    val progressAnswers: LiveData<String>
        get() = _progressAnswers
    private val _correctAnswers = MutableLiveData<Int>()
    val correctAnswers: LiveData<Int>
        get() = _correctAnswers
    val requiredAnswers: Int
        get() = gameSettings.minCountOfRightAnswers
    val maxSumValue: Int
        get() = gameSettings.maxSumValue
    private val _timeUrgent = MutableLiveData<Boolean>(false)
    val timeUrgent: LiveData<Boolean>
        get() = _timeUrgent
    private val _enoughCount = MutableLiveData<Boolean>()
    val enoughCount: LiveData<Boolean>
        get() = _enoughCount
    private val _enoughPercent = MutableLiveData<Boolean>()
    val enoughPercent: LiveData<Boolean>
        get() = _enoughPercent
    private val _minPercent = MutableLiveData<Int>()
    val minPercent: LiveData<Int>
        get() = _minPercent
    private val _gameResult = MutableLiveData<GameResult>()
    val gameResult: LiveData<GameResult>
        get() = _gameResult
    private var countOfRightAnswers = 0
    private var countOfQuestions = 0
    private var gameFinished = false

    init {
        startGame()
    }

    private fun startGame() {
        getGameSettings()
        startTimer()
        generateQuestion()
        updateProgress()
    }

    fun chooseAnswer(displayedQuestion: Question, number: Int) {
        if (gameFinished || _question.value !== displayedQuestion) return
        checkAnswer(number)
        updateProgress()
        generateQuestion()
    }

    private fun updateProgress() {
        val percent = calculatePercentOfRightAnswers(countOfRightAnswers, countOfQuestions)
        _percentOfRightAnswers.value = percent
        _correctAnswers.value = countOfRightAnswers
        _progressAnswers.value = String.format(
            application.resources.configuration.locales[0],
            application.resources.getString(R.string.progress_answers),
            countOfRightAnswers,
            gameSettings.minCountOfRightAnswers
        )
        _enoughCount.value = countOfRightAnswers >= gameSettings.minCountOfRightAnswers
        _enoughPercent.value = percent >= gameSettings.minPercentOfRightAnswers
    }

    private fun checkAnswer(number: Int) {
        val rightAnswer = question.value?.rightAnswer
        if (number == rightAnswer) {
            countOfRightAnswers++
        }
        countOfQuestions++
    }

    private fun getGameSettings() {
        this.gameSettings = getGameSettingsUseCase(level)
        _minPercent.value = gameSettings.minPercentOfRightAnswers
    }

    private fun startTimer() {
        timer = object : CountDownTimer(
            gameSettings.gameTimeInSeconds * MILLIS_IN_SECONDS,
            MILLIS_IN_SECONDS
        ) {
            override fun onTick(millisUntilFinished: Long) {
                _formattedTime.value = formatTime(millisUntilFinished)
                _timeUrgent.value = millisUntilFinished <= 10 * MILLIS_IN_SECONDS
            }
            override fun onFinish() {
                finishGame()
            }
        }
        timer?.start()
    }

    private fun generateQuestion() {
        _question.value = generateQuestionUseCase(gameSettings.maxSumValue)
    }

    private fun formatTime(millisUntilFinished: Long): String {
        val seconds = millisUntilFinished / MILLIS_IN_SECONDS
        val minutes = seconds / SECONDS_IN_MINUTES
        val leftSeconds = seconds - (minutes * SECONDS_IN_MINUTES)
        return String.format(application.resources.configuration.locales[0], "%02d:%02d", minutes, leftSeconds)
    }

    private fun finishGame() {
        gameFinished = true
        _gameResult.value = GameResult(
            enoughCount.value == true && enoughPercent.value == true,
            countOfRightAnswers,
            countOfQuestions,
            gameSettings
        )
    }

    override fun onCleared() {
        timer?.cancel()
    }

    companion object {
        private const val MILLIS_IN_SECONDS = 1000L
        private const val SECONDS_IN_MINUTES = 60
    }
}

internal fun calculatePercentOfRightAnswers(countOfRightAnswers: Int, countOfQuestions: Int): Int {
    if (countOfQuestions == 0) {
        return 0
    }
    return ((countOfRightAnswers / countOfQuestions.toDouble()) * 100).toInt()
}
