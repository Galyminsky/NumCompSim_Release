package me.proton.jobforandroid.compositionofnumbers.domain.entity

data class ComparisonSessionSettings(
    val durationSeconds: Int,
    val minimumAnswers: Int,
    val maximumAnswers: Int,
    val requiredAccuracyPercent: Int
) {
    companion object {
        fun forLevel(level: ComparisonLevel): ComparisonSessionSettings = when (level) {
            ComparisonLevel.TEST -> ComparisonSessionSettings(15, 5, 24, 60)
            ComparisonLevel.EASY -> ComparisonSessionSettings(60, 15, 45, 70)
            ComparisonLevel.NORMAL -> ComparisonSessionSettings(90, 12, 36, 75)
            ComparisonLevel.HARD -> ComparisonSessionSettings(120, 10, 30, 80)
        }
    }
}

/** Immutable counters; the caller decides when time has expired. */
class ComparisonSession private constructor(
    val level: ComparisonLevel,
    val answeredCount: Int,
    val correctCount: Int
) {
    val settings: ComparisonSessionSettings get() = ComparisonSessionSettings.forLevel(level)
    val isAnswerLimitReached: Boolean get() = answeredCount == settings.maximumAnswers

    fun answer(isCorrect: Boolean): ComparisonSession {
        check(!isAnswerLimitReached) { "Comparison answer limit reached" }
        return ComparisonSession(level, answeredCount + 1, correctCount + if (isCorrect) 1 else 0)
    }

    fun toResult(): ComparisonResult = ComparisonResult(level, answeredCount, correctCount)

    companion object {
        fun start(level: ComparisonLevel): ComparisonSession = ComparisonSession(level, 0, 0)
    }
}

data class ComparisonResult(
    val level: ComparisonLevel,
    val answeredCount: Int,
    val correctCount: Int
) {
    init {
        require(answeredCount in 0..ComparisonSessionSettings.forLevel(level).maximumAnswers)
        require(correctCount in 0..answeredCount)
    }

    val accuracyPercent: Int get() = if (answeredCount == 0) 0 else correctCount * 100 / answeredCount
    val isSuccess: Boolean get() {
        val settings = ComparisonSessionSettings.forLevel(level)
        return answeredCount >= settings.minimumAnswers &&
            correctCount * 100 >= settings.requiredAccuracyPercent * answeredCount
    }
}
