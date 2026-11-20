package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Looper
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.domain.entity.*
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import kotlin.random.Random

/** Fragment-scoped owner. Every event, including clock events, is processed on the main thread. */
class FractionsGameViewModel(private val savedState: SavedStateHandle) : ViewModel() {
    data class UiState(
        val level: Level,
        val phase: FractionGameSession.State,
        val question: FractionQuestion<out FractionAnswer>?,
        val selectedIndex: Int?,
        val lastAnswerCorrect: Boolean?,
        val answered: Int,
        val correct: Int,
        val settings: FractionGameSession.Settings,
        val remainingMs: Long,
        val nextUpdateElapsedMs: Long,
        val result: FractionGameSession.Result?,
        val minimumReached: Boolean,
        val accuracyReached: Boolean,
        val resultNavigated: Boolean
    )

    private var session: FractionGameSession? = null
    private var resultNavigated = false
    private var cancelled = false
    var errorMessage: Int? = null
        private set

    init {
        mainThread()
        val restoring = savedState.contains(SNAPSHOT)
        try {
            val level = Level.valueOf(requireNotNull(savedState.get<String>("level")))
            if (restoring) {
                val record = JSONObject(requireNotNull(savedState.get<String>(SNAPSHOT)))
                val payload = record.getString("payload")
                require(record.getString("checksum") == FractionsSavedState.checksum(payload))
                val json = JSONObject(payload)
                require(json.getInt("version") == 1 && !json.getBoolean("cancelled"))
                resultNavigated = json.getBoolean("resultNavigated")
                val snapshot = FractionsSavedState.decode(json)
                require(snapshot.level == level)
                require(!resultNavigated || snapshot.state == FractionGameSession.State.FINISHED)
                session = FractionGameSession.restore(snapshot, SystemClock.elapsedRealtime())
            } else {
                val questions = FractionSessionGenerator.generate(level, Random.Default)
                // Generation completes before reading the starting clock.
                session = FractionGameSession.start(level, questions, SystemClock.elapsedRealtime())
            }
            persist()
        } catch (_: Exception) {
            errorMessage = if (restoring) R.string.fractions_restore_error else R.string.fractions_generation_error
            // Keep an explicit failure marker: recreation must never silently generate a replacement.
            savedState[SNAPSHOT] = "invalid"
        }
    }

    fun refresh(): UiState? {
        mainThread()
        if (cancelled || errorMessage != null) return null
        val game = session ?: return null
        val now = SystemClock.elapsedRealtime()
        val previousPhase = game.state
        game.advance(now)
        if (previousPhase != game.state) persist()
        val settings = FractionGameSession.settingsFor(game.level)
        val remaining = (game.deadlineMs - now).coerceAtLeast(0)
        // Schedule the exact domain boundary as well as the next visible timer second.
        val boundary = if (game.state == FractionGameSession.State.FEEDBACK)
            requireNotNull(game.feedbackEndMs) - now else remaining
        val timerTick = if (remaining > 0) (remaining - 1) % 1000 + 1 else Long.MAX_VALUE
        return UiState(game.level, game.state, game.currentQuestion,
            if (game.state == FractionGameSession.State.FEEDBACK) game.snapshot().acceptedOptions.last() else null,
            game.lastAnswerCorrect, game.answeredCount, game.correctCount, settings, remaining,
            now + minOf(boundary, timerTick).coerceAtLeast(1), game.result,
            game.answeredCount >= settings.minimumAnswers,
            game.answeredCount > 0 && game.correctCount.toLong() * 100 >=
                game.answeredCount.toLong() * settings.accuracyPercent, resultNavigated)
    }

    fun answer(renderedQuestion: FractionQuestion<out FractionAnswer>, optionIndex: Int) {
        mainThread()
        if (cancelled || errorMessage != null) return
        // Domain uses the rendered object's identity as its stale-question token.
        session?.answer(renderedQuestion, optionIndex, SystemClock.elapsedRealtime())
        persist() // Also saves expiry when the answer arrived at/after the deadline.
    }

    fun markResultNavigated() {
        mainThread()
        check(session?.state == FractionGameSession.State.FINISHED)
        resultNavigated = true
        persist()
    }

    fun cancel() {
        mainThread()
        cancelled = true
        persist()
        session = null
    }

    private fun persist() {
        val snapshot = session?.snapshot() ?: return
        val payload = FractionsSavedState.encode(snapshot)
            .put("version", 1).put("cancelled", cancelled)
            .put("resultNavigated", resultNavigated).toString()
        savedState[SNAPSHOT] = JSONObject().put("payload", payload)
            .put("checksum", FractionsSavedState.checksum(payload)).toString()
    }

    private fun mainThread() = check(Looper.myLooper() == Looper.getMainLooper())

    private companion object { const val SNAPSHOT = "fractionsSession" }
}

/** Android-only transport. SavedStateHandle holds one String, never domain objects/Serializable. */
private object FractionsSavedState {
    // Detect damaged but still parseable records without reimplementing level rules here.
    fun checksum(payload: String): String = MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it.toInt() and 255) }

    fun encode(snapshot: FractionGameSession.Snapshot): JSONObject = JSONObject().apply {
        put("level", snapshot.level.name)
        put("phase", snapshot.state.name)
        put("deadline", snapshot.deadlineMs)
        put("feedbackEnd", snapshot.feedbackEndMs ?: JSONObject.NULL)
        put("accepted", JSONArray(snapshot.acceptedOptions))
        put("questions", JSONArray().apply { snapshot.questions.forEach { put(question(it)) } })
    }

    fun decode(json: JSONObject): FractionGameSession.Snapshot = FractionGameSession.Snapshot(
        Level.valueOf(json.getString("level")),
        json.getJSONArray("questions").let { array -> List(array.length()) { question(array.getJSONObject(it)) } },
        json.getJSONArray("accepted").let { array -> List(array.length()) { array.getInt(it) } },
        FractionGameSession.State.valueOf(json.getString("phase")), json.getLong("deadline"),
        if (json.has("feedbackEnd") && json.isNull("feedbackEnd")) null else json.getLong("feedbackEnd")
    )

    private fun fraction(value: Fraction) = JSONArray(listOf(value.numerator, value.denominator))
    private fun fraction(value: JSONArray): Fraction {
        require(value.length() == 2)
        return Fraction(value.getInt(0), value.getInt(1))
    }

    private fun question(question: FractionQuestion<out FractionAnswer>) = JSONObject().apply {
        when (val exercise = question.exercise) {
            is FractionExercise.IdentifyFraction -> {
                put("type", "picture"); put("total", exercise.totalParts); put("shaded", exercise.shadedParts)
            }
            is FractionExercise.IdentifyNumerator -> { put("type", "numerator"); put("left", fraction(exercise.source)) }
            is FractionExercise.IdentifyDenominator -> { put("type", "denominator"); put("left", fraction(exercise.source)) }
            is FractionExercise.Reduce -> { put("type", "reduce"); put("left", fraction(exercise.source)) }
            is FractionExercise.ToMixedNumber -> { put("type", "mixed"); put("left", fraction(exercise.source)) }
            is FractionExercise.Compare -> {
                put("type", "compare"); put("left", fraction(exercise.left)); put("right", fraction(exercise.right))
            }
            is FractionExercise.Add -> {
                put("type", "add"); put("arithmeticLevel", exercise.level.name)
                put("left", fraction(exercise.left)); put("right", fraction(exercise.right))
            }
            is FractionExercise.Subtract -> {
                put("type", "subtract"); put("arithmeticLevel", exercise.level.name)
                put("left", fraction(exercise.left)); put("right", fraction(exercise.right))
            }
        }
        put("options", JSONArray().apply { question.options.forEach { put(answer(it)) } })
    }

    @Suppress("UNCHECKED_CAST")
    private fun question(json: JSONObject): FractionQuestion<out FractionAnswer> {
        fun left() = fraction(json.getJSONArray("left"))
        fun right() = fraction(json.getJSONArray("right"))
        fun level() = FractionArithmeticLevel.valueOf(json.getString("arithmeticLevel"))
        val exercise: FractionExercise<out FractionAnswer> = when (json.getString("type")) {
            "picture" -> FractionExercise.IdentifyFraction(json.getInt("total"), json.getInt("shaded"))
            "numerator" -> FractionExercise.IdentifyNumerator(left())
            "denominator" -> FractionExercise.IdentifyDenominator(left())
            "reduce" -> FractionExercise.Reduce(left())
            "mixed" -> FractionExercise.ToMixedNumber(left())
            "compare" -> FractionExercise.Compare(left(), right())
            "add" -> FractionExercise.Add(level(), left(), right())
            "subtract" -> FractionExercise.Subtract(level(), left(), right())
            else -> error("Unknown saved exercise")
        }
        val options = json.getJSONArray("options")
        // Construction delegates format, uniqueness and correctness validation to Domain.
        return FractionQuestion(exercise as FractionExercise<FractionAnswer>,
            List(options.length()) { answer(options.getJSONObject(it)) })
    }

    private fun answer(answer: FractionAnswer) = JSONObject().apply {
        when (answer) {
            is FractionAnswer.Integer -> { put("type", "integer"); put("value", answer.value) }
            is FractionAnswer.FractionValue -> { put("type", "fraction"); put("value", fraction(answer.value)) }
            is FractionAnswer.MixedNumber -> {
                put("type", "mixed"); put("whole", answer.wholePart); put("value", fraction(answer.fractionalPart))
            }
            is FractionAnswer.Sign -> { put("type", "sign"); put("value", answer.value.name) }
        }
    }

    private fun answer(json: JSONObject): FractionAnswer = when (json.getString("type")) {
        "integer" -> FractionAnswer.Integer(json.getInt("value"))
        "fraction" -> FractionAnswer.FractionValue(fraction(json.getJSONArray("value")))
        "mixed" -> FractionAnswer.MixedNumber(json.getInt("whole"), fraction(json.getJSONArray("value")))
        "sign" -> FractionAnswer.Sign(FractionAnswer.ComparisonValue.valueOf(json.getString("value")))
        else -> error("Unknown saved answer")
    }
}
