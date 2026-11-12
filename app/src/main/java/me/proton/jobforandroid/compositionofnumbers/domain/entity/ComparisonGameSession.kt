package me.proton.jobforandroid.compositionofnumbers.domain.entity

/** Drives questions and feedback; elapsed time and the 500 ms delay are signalled by the caller. */
class ComparisonGameSession(questions: List<ComparisonQuestion>) {
    enum class State { QUESTION, FEEDBACK, FINISHED }

    private val questions = questions.toList()
    val level: ComparisonLevel
    private var counters: ComparisonSession
    private var timeExpired = false

    var state: State = State.QUESTION
        private set

    var lastAnswerCorrect: Boolean? = null
        private set

    val answeredCount: Int get() = counters.answeredCount
    val correctCount: Int get() = counters.correctCount
    val currentQuestion: ComparisonQuestion? get() = when (state) {
        State.QUESTION -> questions[answeredCount]
        State.FEEDBACK -> questions[answeredCount - 1]
        State.FINISHED -> null
    }
    val result: ComparisonResult? get() = if (state == State.FINISHED) counters.toResult() else null

    init {
        require(questions.isNotEmpty()) { "Comparison session requires questions" }
        level = questions.first().level
        require(questions.size == ComparisonSessionSettings.forLevel(level).maximumAnswers)
        require(questions.all { it.level == level })
        counters = ComparisonSession.start(level)
    }

    fun answer(sign: ComparisonSign) {
        check(state == State.QUESTION) { "No comparison question accepts an answer" }
        val correct = sign == requireNotNull(currentQuestion).correctSign
        counters = counters.answer(correct)
        lastAnswerCorrect = correct
        state = State.FEEDBACK
    }

    fun finishFeedback() {
        check(state == State.FEEDBACK) { "No comparison feedback is pending" }
        if (timeExpired || counters.isAnswerLimitReached) {
            state = State.FINISHED
        } else {
            lastAnswerCorrect = null
            state = State.QUESTION
        }
    }

    fun expireTime() {
        if (state == State.FINISHED) return
        timeExpired = true
        if (state == State.QUESTION) state = State.FINISHED
    }
}
