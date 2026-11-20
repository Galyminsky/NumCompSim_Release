package me.proton.jobforandroid.compositionofnumbers.domain.entity

import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level

/** Single-owner state machine. All times use the caller's same monotonic millisecond clock. */
class FractionGameSession private constructor(saved: Snapshot) {
    enum class State { QUESTION, FEEDBACK, FINISHED }

    data class Settings(val questionCount: Int, val durationMs: Long, val minimumAnswers: Int, val accuracyPercent: Int)

    /** Answers are option indices for the consecutive prefix of questions, in acceptance order.
     * Position and selected feedback answer are derived, so they cannot disagree with that prefix.
     */
    data class Snapshot(
        val level: Level,
        val questions: List<FractionQuestion<out FractionAnswer>>,
        val acceptedOptions: List<Int>,
        val state: State,
        val deadlineMs: Long,
        val feedbackEndMs: Long?
    )

    data class Result(val answeredCount: Int, val correctCount: Int, val isSuccess: Boolean)

    private var data = saved.copy(questions = saved.questions.toList(), acceptedOptions = saved.acceptedOptions.toList())
    val level: Level get() = data.level
    val state: State get() = data.state
    val deadlineMs: Long get() = data.deadlineMs
    val feedbackEndMs: Long? get() = data.feedbackEndMs
    val answeredCount: Int get() = data.acceptedOptions.size
    val currentPosition: Int get() = if (state == State.FEEDBACK) answeredCount - 1 else answeredCount
    val currentQuestion: FractionQuestion<out FractionAnswer>?
        get() = if (state == State.FINISHED) null else data.questions[currentPosition]
    val selectedAnswer: FractionAnswer?
        get() = if (state == State.FEEDBACK) data.questions[currentPosition].options[data.acceptedOptions.last()] else null
    val correctCount: Int get() = data.acceptedOptions.indices.count { isCorrect(it, data.acceptedOptions[it]) }
    val lastAnswerCorrect: Boolean?
        get() = if (state == State.FEEDBACK) isCorrect(currentPosition, data.acceptedOptions.last()) else null
    val result: Result? get() {
        if (state != State.FINISHED) return null
        val settings = settingsFor(level)
        val correct = correctCount
        return Result(answeredCount, correct, answeredCount >= settings.minimumAnswers &&
            answeredCount > 0 && correct.toLong() * 100 >= answeredCount.toLong() * settings.accuracyPercent)
    }

    fun snapshot(): Snapshot = data.copy(questions = data.questions.toList(), acceptedOptions = data.acceptedOptions.toList())

    /** Pass the question object actually rendered, and an index into its displayed options.
     * Old question objects (including those from before reconstruction) must not be reused by UI.
     */
    fun answer(question: FractionQuestion<out FractionAnswer>, optionIndex: Int, nowMs: Long): Boolean {
        advance(nowMs)
        if (state != State.QUESTION || currentQuestion !== question || optionIndex !in question.options.indices) return false
        data = data.copy(acceptedOptions = data.acceptedOptions + optionIndex,
            state = State.FEEDBACK, feedbackEndMs = nowMs + FEEDBACK_MS)
        return true
    }

    /** Explicit clock event, also used before accepting an answer and immediately on restoration. */
    fun advance(nowMs: Long) {
        require(nowMs >= 0) { "Monotonic time must be nonnegative" }
        when (state) {
            State.QUESTION -> if (nowMs >= deadlineMs) data = data.copy(state = State.FINISHED)
            State.FEEDBACK -> if (nowMs >= requireNotNull(feedbackEndMs)) {
                data = data.copy(state = if (nowMs >= deadlineMs || answeredCount == data.questions.size)
                    State.FINISHED else State.QUESTION, feedbackEndMs = null)
            }
            State.FINISHED -> Unit
        }
    }

    private fun isCorrect(questionIndex: Int, optionIndex: Int): Boolean {
        val question = data.questions[questionIndex]
        val option = question.options[optionIndex]
        val correct = question.correctAnswer
        // TEST alone permits an equivalent, differently written correct displayed fraction.
        return if (question.exercise is FractionExercise.IdentifyFraction)
            (option as FractionAnswer.FractionValue).value.isEquivalentTo((correct as FractionAnswer.FractionValue).value)
        else option == correct
    }

    companion object {
        const val FEEDBACK_MS = 500L

        fun settingsFor(level: Level): Settings = when (level) {
            Level.TEST -> Settings(8, 30_000, 5, 60)
            Level.EASY -> Settings(15, 90_000, 10, 70)
            Level.NORMAL -> Settings(12, 120_000, 8, 75)
            Level.HARD -> Settings(10, 180_000, 6, 80)
        }

        /** The complete, already generated level set must be supplied before starting the clock. */
        fun start(level: Level, questions: List<FractionQuestion<out FractionAnswer>>, nowMs: Long): FractionGameSession {
            val duration = settingsFor(level).durationMs
            require(nowMs >= 0 && nowMs <= Long.MAX_VALUE - duration - FEEDBACK_MS) { "Deadline would overflow" }
            return restore(Snapshot(level, questions, emptyList(), State.QUESTION, nowMs + duration, null), nowMs)
        }

        fun restore(snapshot: Snapshot, nowMs: Long): FractionGameSession {
            require(snapshot.questions.size == settingsFor(snapshot.level).questionCount) { "Incomplete question set" }
            require(snapshot.deadlineMs in 0..Long.MAX_VALUE - FEEDBACK_MS) { "Invalid deadline" }
            require(snapshot.acceptedOptions.size <= snapshot.questions.size) { "Too many answers" }
            snapshot.acceptedOptions.forEachIndexed { index, option ->
                require(option in snapshot.questions[index].options.indices) { "Answer is not a displayed option" }
            }
            when (snapshot.state) {
                State.QUESTION -> require(snapshot.acceptedOptions.size < snapshot.questions.size && snapshot.feedbackEndMs == null)
                State.FEEDBACK -> require(snapshot.acceptedOptions.isNotEmpty() && snapshot.feedbackEndMs != null &&
                    snapshot.feedbackEndMs >= FEEDBACK_MS && snapshot.feedbackEndMs < snapshot.deadlineMs + FEEDBACK_MS)
                State.FINISHED -> require(snapshot.feedbackEndMs == null &&
                    (snapshot.acceptedOptions.size == snapshot.questions.size || nowMs >= snapshot.deadlineMs))
            }
            return FractionGameSession(snapshot).also { it.advance(nowMs) }
        }
    }
}
