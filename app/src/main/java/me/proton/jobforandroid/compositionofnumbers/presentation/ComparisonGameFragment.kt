package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.data.ComparisonSessionDataSource
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ComparisonGameBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonGameSession
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonOperation
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonQuestion
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSessionSettings
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSide
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSign
import java.util.Locale

class ComparisonGameFragment : Fragment() {
    private var _binding: NcsV2ComparisonGameBinding? = null
    private val binding get() = _binding!!
    private var session: ComparisonGameSession? = null
    private var questions: List<ComparisonQuestion>? = null
    private val answerHistory = ArrayList<ComparisonSign>()
    private var selectedSign: ComparisonSign? = null
    private var feedbackQuestion: ComparisonQuestion? = null
    private var displayedQuestion: ComparisonQuestion? = null
    private var feedbackEndUptimeMs = 0L
    private var expressionTextSizePx = 0f
    private var deadlineElapsedMs = 0L
    private var stoppedRemainingMs: Long? = null
    private var resultNavigationStarted = false
    private var sounds: UiSoundPool? = null
    private val callbackHandler = Handler(Looper.getMainLooper())
    private var finishFeedback: Runnable? = null
    private var updateTimer: Runnable? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2ComparisonGameBinding.inflate(inflater, container, false)
        val gameBinding = binding
        finishFeedback = Runnable {
            if (_binding === gameBinding) advanceAfterFeedback()
        }
        updateTimer = Runnable { renderTimer(gameBinding) }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press, R.raw.answer_correct, R.raw.answer_wrong)
        binding.ncsV2ComparisonGameBackButton.installButtonPressAnimation()
        val level = requireArguments().getSerializable(LEVEL_ARGUMENT) as ComparisonLevel
        val levelName = when (level) {
            ComparisonLevel.TEST -> R.string.ncs_v2_test_level
            ComparisonLevel.EASY -> R.string.ncs_v2_easy_level
            ComparisonLevel.NORMAL -> R.string.ncs_v2_medium_level
            ComparisonLevel.HARD -> R.string.ncs_v2_hard_level
        }
        binding.ncsV2ComparisonGameContext.text = getString(
            R.string.ncs_v2_game_level_context,
            getString(levelName),
            getString(R.string.ncs_v2_hub_comparison)
        )
        if (session == null && savedInstanceState != null &&
            savedInstanceState.containsKey(QUESTIONS_KEY)) {
            restoreSession(level, savedInstanceState)
        }
        val gameSession = session ?: run {
            val generated = ComparisonSessionDataSource().generateQuestions(level)
            questions = generated
            ComparisonGameSession(generated).also { session = it }
        }
        val settings = ComparisonSessionSettings.forLevel(gameSession.level)
        binding.ncsV2ComparisonGameMinimum.text = getString(
            R.string.ncs_v2_comparison_game_minimum, settings.minimumAnswers
        )
        if (deadlineElapsedMs == 0L) {
            deadlineElapsedMs = SystemClock.elapsedRealtime() +
                settings.durationSeconds * 1000L
        }
        expireIfNeeded(gameSession)
        if (gameSession.state == ComparisonGameSession.State.FINISHED) {
            navigateToResult(gameSession)
            return
        }
        expressionTextSizePx = binding.ncsV2ComparisonExpressionLeft.textSize
        when (gameSession.state) {
            ComparisonGameSession.State.QUESTION -> renderQuestion(requireNotNull(gameSession.currentQuestion))
            ComparisonGameSession.State.FEEDBACK -> {
                renderFeedback(requireNotNull(gameSession.currentQuestion), requireNotNull(selectedSign))
                scheduleFeedbackEnd()
            }
            ComparisonGameSession.State.FINISHED -> {
                if (selectedSign != null) {
                    renderFeedback(requireNotNull(feedbackQuestion), requireNotNull(selectedSign))
                } else {
                    renderQuestion(requireNotNull(displayedQuestion))
                }
                answerButtons().forEach { it.isEnabled = false }
            }
        }
        if (resultNavigationStarted) return
        renderTimer(binding)
        binding.ncsV2ComparisonLessButton.setOnClickListener { answer(ComparisonSign.LESS) }
        binding.ncsV2ComparisonEqualButton.setOnClickListener { answer(ComparisonSign.EQUAL) }
        binding.ncsV2ComparisonGreaterButton.setOnClickListener { answer(ComparisonSign.GREATER) }
        binding.ncsV2ComparisonGameBackButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.comparisonGameFragment) {
                sounds?.play(R.raw.key_press)
                navController.popBackStack()
            }
        }
    }

    private fun answer(sign: ComparisonSign) {
        val gameSession = requireNotNull(session)
        if (expireIfNeeded(gameSession)) {
            answerButtons().forEach { it.isEnabled = false }
            renderTimer(binding)
            return
        }
        if (gameSession.state != ComparisonGameSession.State.QUESTION ||
            findNavController().currentDestination?.id != R.id.comparisonGameFragment) return
        val question = requireNotNull(gameSession.currentQuestion)
        gameSession.answer(sign)
        sounds?.play(if (gameSession.lastAnswerCorrect == true) R.raw.answer_correct else R.raw.answer_wrong)
        val selectedButton = answerButtons()[sign.ordinal]
        selectedButton.animate().scaleX(0.96f).scaleY(0.96f)
            .setDuration(ANIMATION_HALF_MS)
            .withEndAction {
                if (_binding != null) {
                    selectedButton.animate().scaleX(1f).scaleY(1f)
                        .setDuration(ANIMATION_HALF_MS).start()
                }
            }.start()
        answerHistory.add(sign)
        selectedSign = sign
        feedbackQuestion = question
        feedbackEndUptimeMs = SystemClock.uptimeMillis() + FEEDBACK_MS
        renderFeedback(question, sign)
        scheduleFeedbackEnd()
    }

    private fun renderQuestion(question: ComparisonQuestion) {
        displayedQuestion = question
        renderExpression(question, getString(R.string.ncs_v2_math_question_mark))
        answerButtons().forEach { button ->
            button.isActivated = false
            button.setBackgroundResource(R.drawable.ncs_v2_answer_card)
            button.setTextColor(requireContext().getColor(R.color.ncs_v2_ink))
            button.isEnabled = true
        }
        binding.ncsV2ComparisonCorrectIndicator.visibility = View.GONE
        binding.ncsV2ComparisonFeedbackText.setText(R.string.ncs_v2_comparison_game_hint)
        binding.ncsV2ComparisonFeedbackText.setTextColor(requireContext().getColor(R.color.ncs_v2_muted))
        renderStats()
    }

    private fun renderFeedback(question: ComparisonQuestion, sign: ComparisonSign) {
        val correctSign = question.correctSign
        renderExpression(question, signSymbol(correctSign))
        answerButtons().forEachIndexed { index, button ->
            val buttonSign = ComparisonSign.entries[index]
            button.isEnabled = false
            button.isActivated = false
            if (buttonSign == sign && sign == correctSign) {
                button.setBackgroundResource(R.drawable.ncs_v2_answer_correct)
                button.setTextColor(requireContext().getColor(R.color.ncs_v2_surface))
            } else if (buttonSign == sign) {
                button.setBackgroundResource(R.drawable.ncs_v2_answer_wrong)
                button.setTextColor(requireContext().getColor(R.color.ncs_v2_surface))
            } else if (buttonSign == correctSign) {
                button.setBackgroundResource(R.drawable.ncs_v2_answer_correct)
                button.setTextColor(requireContext().getColor(R.color.ncs_v2_surface))
            }
        }
        val correct = requireNotNull(requireNotNull(session).lastAnswerCorrect)
        binding.ncsV2ComparisonCorrectIndicator.visibility = if (correct) View.VISIBLE else View.GONE
        binding.ncsV2ComparisonFeedbackText.text = explanation(question)
        binding.ncsV2ComparisonFeedbackText.setTextColor(
            requireContext().getColor(
                if (correct) R.color.ncs_v2_comparison_correct_border else R.color.ncs_v2_muted
            )
        )
        renderStats()
    }

    private fun renderExpression(question: ComparisonQuestion, sign: String) {
        val gameBinding = binding
        val left = formatSide(question.left)
        val right = formatSide(question.right)
        val leftView = gameBinding.ncsV2ComparisonExpressionLeft
        val signView = gameBinding.ncsV2ComparisonExpressionSign
        val rightView = gameBinding.ncsV2ComparisonExpressionRight
        listOf(leftView, signView, rightView).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_PX, expressionTextSizePx)
        }
        leftView.text = left
        signView.text = sign
        rightView.text = right
        gameBinding.ncsV2ComparisonExpressionCard.contentDescription = "$left $sign $right"
        gameBinding.ncsV2ComparisonExpressionRow.post {
            if (_binding !== gameBinding) return@post
            val signMargins = signView.layoutParams as ViewGroup.MarginLayoutParams
            val card = gameBinding.ncsV2ComparisonExpressionCard
            val sideWidth = (card.width - card.paddingLeft - card.paddingRight -
                signView.width -
                signMargins.marginStart - signMargins.marginEnd) / 2f
            val widestSide = maxOf(
                leftView.paint.measureText(leftView.text.toString()),
                rightView.paint.measureText(rightView.text.toString())
            )
            if (sideWidth > 0 && widestSide > sideWidth) {
                val scale = sideWidth / widestSide
                listOf(leftView, signView, rightView).forEach { view ->
                    view.setTextSize(TypedValue.COMPLEX_UNIT_PX, expressionTextSizePx * scale)
                }
            }
        }
    }

    private fun explanation(question: ComparisonQuestion): String {
        val calculations = listOf(question.left, question.right)
            .filterIsInstance<ComparisonSide.Expression>()
            .joinToString("; ") { "${formatSide(it)} = ${it.value}" }
        val comparison = "${question.leftValue} ${signSymbol(question.correctSign)} ${question.rightValue}"
        return if (calculations.isEmpty()) comparison else
            getString(R.string.ncs_v2_comparison_game_explanation_format, calculations, comparison)
    }

    private fun renderStats() {
        val gameSession = requireNotNull(session)
        val answered = resources.getQuantityString(
            R.plurals.ncs_v2_comparison_answered_count,
            gameSession.answeredCount,
            gameSession.answeredCount
        )
        val correct = resources.getQuantityString(
            R.plurals.ncs_v2_comparison_correct_count,
            gameSession.correctCount,
            gameSession.correctCount
        )
        binding.ncsV2ComparisonGameStatsValue.text = "$answered · $correct"
        renderProgress(gameSession)
    }

    private fun renderProgress(gameSession: ComparisonGameSession) {
        val gameBinding = binding
        val track = gameBinding.ncsV2ComparisonProgressTrack
        val fill = gameBinding.ncsV2ComparisonProgressFill
        val total = requireNotNull(questions).size
        val answered = gameSession.answeredCount
        fill.layoutParams = fill.layoutParams.apply {
            width = track.width * answered / total
        }
        if (track.width == 0) track.post {
            if (_binding === gameBinding) {
                fill.layoutParams = fill.layoutParams.apply {
                    width = track.width * requireNotNull(session).answeredCount / total
                }
            }
        }
    }

    private fun answerButtons(gameBinding: NcsV2ComparisonGameBinding = binding) = listOf(
        gameBinding.ncsV2ComparisonLessButton,
        gameBinding.ncsV2ComparisonEqualButton,
        gameBinding.ncsV2ComparisonGreaterButton
    )

    private fun signSymbol(sign: ComparisonSign): String = getString(when (sign) {
        ComparisonSign.LESS -> R.string.ncs_v2_comparison_less_sign
        ComparisonSign.EQUAL -> R.string.ncs_v2_math_equals
        ComparisonSign.GREATER -> R.string.ncs_v2_comparison_rules_example_sign
    })

    private fun scheduleFeedbackEnd() {
        val remaining = feedbackEndUptimeMs - SystemClock.uptimeMillis()
        if (remaining <= 0L) advanceAfterFeedback()
        else if (_binding != null) {
            finishFeedback?.let { callbackHandler.postDelayed(it, remaining) }
        }
    }

    private fun advanceAfterFeedback() {
        val gameSession = session ?: return
        if (gameSession.state != ComparisonGameSession.State.FEEDBACK) return
        expireIfNeeded(gameSession)
        gameSession.finishFeedback()
        feedbackEndUptimeMs = 0L
        if (gameSession.state == ComparisonGameSession.State.QUESTION) {
            selectedSign = null
            feedbackQuestion = null
            _binding?.let { renderQuestion(requireNotNull(gameSession.currentQuestion)) }
        }
        _binding?.let { renderTimer(it) }
        if (gameSession.state == ComparisonGameSession.State.FINISHED) {
            navigateToResult(gameSession)
        }
    }

    private fun expireIfNeeded(gameSession: ComparisonGameSession): Boolean {
        if (SystemClock.elapsedRealtime() < deadlineElapsedMs) return false
        gameSession.expireTime()
        return true
    }

    private fun renderTimer(gameBinding: NcsV2ComparisonGameBinding) {
        if (_binding !== gameBinding) return
        val gameSession = session ?: return
        val remainingMs = stoppedRemainingMs
            ?: (deadlineElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        val remainingSeconds = (remainingMs + 999L) / 1000L
        gameBinding.ncsV2ComparisonGameTimeValue.text = String.format(
            Locale.ROOT, "%02d:%02d", remainingSeconds / 60, remainingSeconds % 60
        )
        updateTimer?.let { callbackHandler.removeCallbacks(it) }
        if (remainingMs == 0L) {
            gameSession.expireTime()
            if (gameSession.state == ComparisonGameSession.State.FINISHED) {
                answerButtons(gameBinding).forEach { it.isEnabled = false }
            }
        } else if (gameSession.state != ComparisonGameSession.State.FINISHED) {
            updateTimer?.let { callbackHandler.postDelayed(it, (remainingMs - 1L) % 1000L + 1L) }
        }
        if (gameSession.state == ComparisonGameSession.State.FINISHED) {
            stoppedRemainingMs = remainingMs
            navigateToResult(gameSession)
        }
    }

    private fun navigateToResult(gameSession: ComparisonGameSession) {
        if (resultNavigationStarted || gameSession.state != ComparisonGameSession.State.FINISHED) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.comparisonGameFragment) return
        val result = requireNotNull(gameSession.result)
        resultNavigationStarted = true
        navController.navigate(
            R.id.action_comparisonGameFragment_to_comparisonResultFragment,
            Bundle().apply {
                putSerializable("level", result.level)
                putInt("answeredCount", result.answeredCount)
                putInt("correctCount", result.correctCount)
            }
        )
    }

    private fun formatSide(side: ComparisonSide): String = when (side) {
        is ComparisonSide.Number -> side.value.toString()
        is ComparisonSide.Expression -> {
            val operation = when (side.operation) {
                ComparisonOperation.ADDITION -> R.string.ncs_v2_math_plus
                ComparisonOperation.SUBTRACTION -> R.string.ncs_v2_math_minus
                ComparisonOperation.MULTIPLICATION -> R.string.ncs_v2_math_multiply
                ComparisonOperation.DIVISION -> R.string.ncs_v2_math_divide
            }
            "${side.firstOperand} ${getString(operation)} ${side.secondOperand}"
        }
    }

    private fun restoreSession(level: ComparisonLevel, state: Bundle) {
        @Suppress("DEPRECATION", "UNCHECKED_CAST")
        val savedQuestions = state.getSerializable(QUESTIONS_KEY) as ArrayList<IntArray>
        val restoredQuestions = savedQuestions.map { encoded ->
            ComparisonQuestion(level, decodeSide(encoded, 0), decodeSide(encoded, 4))
        }
        questions = restoredQuestions
        val restoredSession = ComparisonGameSession(restoredQuestions)
        session = restoredSession
        deadlineElapsedMs = state.getLong(DEADLINE_KEY)
        feedbackEndUptimeMs = state.getLong(FEEDBACK_END_KEY)
        stoppedRemainingMs = if (state.containsKey(STOPPED_REMAINING_KEY)) {
            state.getLong(STOPPED_REMAINING_KEY)
        } else null
        val savedPhase = ComparisonGameSession.State.valueOf(requireNotNull(state.getString(PHASE_KEY)))
        val savedAnswers = requireNotNull(state.getIntArray(ANSWERS_KEY))
        savedAnswers.forEachIndexed { index, ordinal ->
            val sign = ComparisonSign.entries[ordinal]
            feedbackQuestion = restoredSession.currentQuestion
            restoredSession.answer(sign)
            answerHistory.add(sign)
            val last = index == savedAnswers.lastIndex
            if (!last || savedPhase != ComparisonGameSession.State.FEEDBACK) {
                if (last && savedPhase == ComparisonGameSession.State.FINISHED &&
                    SystemClock.elapsedRealtime() >= deadlineElapsedMs) restoredSession.expireTime()
                restoredSession.finishFeedback()
            }
            if (last) selectedSign = sign
        }
        if (savedPhase == ComparisonGameSession.State.FINISHED &&
            restoredSession.state != ComparisonGameSession.State.FINISHED) restoredSession.expireTime()
        if (restoredSession.state == ComparisonGameSession.State.QUESTION) {
            selectedSign = null
            feedbackQuestion = null
        }
    }

    private fun decodeSide(encoded: IntArray, offset: Int): ComparisonSide =
        if (encoded[offset] == 0) ComparisonSide.Number(encoded[offset + 1])
        else ComparisonSide.Expression(
            encoded[offset + 1],
            ComparisonOperation.entries[encoded[offset + 2]],
            encoded[offset + 3]
        )

    private fun encodeSide(side: ComparisonSide): IntArray = when (side) {
        is ComparisonSide.Number -> intArrayOf(0, side.value, 0, 0)
        is ComparisonSide.Expression -> intArrayOf(
            1, side.firstOperand, side.operation.ordinal, side.secondOperand
        )
    }

    override fun onSaveInstanceState(outState: Bundle) {
        session?.let { gameSession ->
            outState.putSerializable(QUESTIONS_KEY, ArrayList(requireNotNull(questions).map {
                encodeSide(it.left) + encodeSide(it.right)
            }))
            outState.putIntArray(ANSWERS_KEY, answerHistory.map { it.ordinal }.toIntArray())
            outState.putString(PHASE_KEY, gameSession.state.name)
            outState.putLong(DEADLINE_KEY, deadlineElapsedMs)
            outState.putLong(FEEDBACK_END_KEY, feedbackEndUptimeMs)
            stoppedRemainingMs?.let { outState.putLong(STOPPED_REMAINING_KEY, it) }
        }
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.ncsV2ComparisonGameBackButton.cancelButtonPressAnimation()
        answerButtons().forEach { button ->
            button.animate().cancel()
            button.scaleX = 1f
            button.scaleY = 1f
        }
        sounds?.release()
        sounds = null
        finishFeedback?.let { callbackHandler.removeCallbacks(it) }
        updateTimer?.let { callbackHandler.removeCallbacks(it) }
        finishFeedback = null
        updateTimer = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val LEVEL_ARGUMENT = "level"
        private const val FEEDBACK_MS = 500L
        private const val ANIMATION_HALF_MS = 60L
        private const val QUESTIONS_KEY = "comparison_questions"
        private const val ANSWERS_KEY = "comparison_answers"
        private const val PHASE_KEY = "comparison_phase"
        private const val DEADLINE_KEY = "comparison_deadline"
        private const val FEEDBACK_END_KEY = "comparison_feedback_end"
        private const val STOPPED_REMAINING_KEY = "comparison_stopped_remaining"
    }
}
