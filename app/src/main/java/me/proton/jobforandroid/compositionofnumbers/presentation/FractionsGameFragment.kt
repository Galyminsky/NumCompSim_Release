package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.BR
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.domain.entity.*
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level

/** Displays Domain state and forwards events to the Fragment-scoped session owner. */
class FractionsGameFragment : Fragment() {
    private val model: FractionsGameViewModel by viewModels()
    private var binding: ViewDataBinding? = null
    private var content: FrameLayout? = null
    private var layoutId = 0
    private var errorDialog: AlertDialog? = null
    private var sounds: UiSoundPool? = null
    private val handler = Handler(Looper.getMainLooper())
    private val update = Runnable { render() }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        FrameLayout(requireContext()).also { content = it }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press, R.raw.answer_correct, R.raw.answer_wrong)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = leaveGame()
        })
        // Populate the first view before it is drawn; do not show an empty FrameLayout first.
        render(initial = true)
    }

    override fun onResume() {
        super.onResume()
        // View lifecycle reaches RESUMED after Fragment.onResume returns.
        handler.post(update) // elapsedRealtime includes the entire background interval.
    }

    override fun onPause() {
        handler.removeCallbacks(update)
        super.onPause()
    }

    private fun render(initial: Boolean = false) {
        handler.removeCallbacks(update)
        if (content == null || (!initial &&
                !viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))) return
        val state = model.refresh()
        if (state == null) {
            if (initial) return // Dialogs and navigation wait for the normal resumed render.
            model.errorMessage?.let { message ->
                if (errorDialog == null) {
                    errorDialog = AlertDialog.Builder(requireContext()).setMessage(message)
                        .setCancelable(false).setPositiveButton(R.string.fractions_choose_again) { _, _ -> leaveGame() }
                        .create().also { it.show() }
                }
            }
            return
        }
        if (state.phase == FractionGameSession.State.FINISHED) {
            if (!initial) showResult(state)
            return
        }
        val question = requireNotNull(state.question)
        val exercise = question.exercise
        val feedback = state.phase == FractionGameSession.State.FEEDBACK
        val layout = when (exercise) {
            is FractionExercise.IdentifyFraction -> R.layout.ncs_v2_fractions_game_picture
            is FractionExercise.IdentifyNumerator, is FractionExercise.IdentifyDenominator -> R.layout.ncs_v2_fractions_game_numerator
            is FractionExercise.Compare -> if (feedback) R.layout.ncs_v2_fractions_game_feedback else R.layout.ncs_v2_fractions_game_comparison
            is FractionExercise.Reduce -> R.layout.ncs_v2_fractions_game_reduction
            is FractionExercise.ToMixedNumber -> R.layout.ncs_v2_fractions_game_mixed
            is FractionExercise.Add, is FractionExercise.Subtract -> R.layout.ncs_v2_fractions_game_arithmetic
        }
        val screen = screen(layout)
        fun set(id: Int, value: Any?) { screen.setVariable(id, value) }
        set(BR.contextLabel, getString(R.string.fractions_level_context, getString(levelLabel(state.level))))
        set(BR.instruction, getString(instruction(exercise)))
        set(BR.enabled, !feedback)
        set(BR.feedbackVisible, feedback)
        set(BR.feedbackCorrect, state.lastAnswerCorrect == true)
        set(BR.highlightedIndex, if (state.lastAnswerCorrect == true) state.selectedIndex else null)
        set(BR.wrongIndex, if (state.lastAnswerCorrect == false) state.selectedIndex else null)
        set(BR.feedback, getString(if (feedback) {
            if (state.lastAnswerCorrect == true) R.string.fractions_answer_correct else R.string.fractions_answer_wrong
        } else R.string.fractions_answer_hint))
        set(BR.statistics, getString(R.string.fractions_statistics, state.correct, state.answered))
        set(BR.minimum, getString(R.string.fractions_minimum, state.settings.minimumAnswers))
        val seconds = (state.remainingMs + 999) / 1000
        set(BR.time, getString(R.string.fractions_time, seconds / 60, seconds % 60))
        set(BR.support, getString(R.string.fractions_support))
        set(BR.progress, state.answered * 100 / state.settings.questionCount)
        var left: Fraction? = null
        var right: Fraction? = null
        when (exercise) {
            is FractionExercise.IdentifyFraction -> {
                set(BR.totalParts, exercise.totalParts)
                set(BR.shadedParts, exercise.shadedParts)
            }
            is FractionExercise.IdentifyNumerator -> {
                left = exercise.source
                set(BR.choiceLabel, getString(R.string.fractions_numerator_choice))
            }
            is FractionExercise.IdentifyDenominator -> {
                left = exercise.source
                set(BR.choiceLabel, getString(R.string.fractions_denominator_choice))
            }
            is FractionExercise.Compare -> {
                left = exercise.left; right = exercise.right
                set(BR.comparisonSign, if (feedback) sign(exercise.correctAnswer.value) else null)
            }
            is FractionExercise.Reduce -> left = exercise.source
            is FractionExercise.ToMixedNumber -> left = exercise.source
            is FractionExercise.Add -> { left = exercise.left; right = exercise.right }
            is FractionExercise.Subtract -> { left = exercise.left; right = exercise.right }
        }
        set(BR.left, left)
        set(BR.right, right)
        set(BR.operator, getString(if (exercise is FractionExercise.Subtract) R.string.fractions_minus else R.string.fractions_plus))
        val fractionVariables = intArrayOf(BR.fraction1, BR.fraction2, BR.fraction3, BR.fraction4)
        val mixedVariables = intArrayOf(BR.mixed1, BR.mixed2, BR.mixed3, BR.mixed4)
        val textVariables = intArrayOf(BR.text1, BR.text2, BR.text3, BR.text4)
        val optionIds = intArrayOf(R.id.fractions_option_1, R.id.fractions_option_2, R.id.fractions_option_3, R.id.fractions_option_4)
        question.options.forEachIndexed { index, answer ->
            set(fractionVariables[index], (answer as? FractionAnswer.FractionValue)?.value)
            set(mixedVariables[index], answer as? FractionAnswer.MixedNumber)
            set(textVariables[index], when (answer) {
                is FractionAnswer.Integer -> answer.value.toString()
                is FractionAnswer.Sign -> sign(answer.value)
                else -> null
            })
            screen.root.findViewById<View>(optionIds[index]).setOnClickListener {
                if (findNavController().currentDestination?.id == R.id.fractionsGameFragment &&
                    viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                    val answeredBefore = model.refresh()?.answered ?: return@setOnClickListener
                    model.answer(question, index)
                    val accepted = model.refresh()
                    if (accepted != null && accepted.answered > answeredBefore &&
                        accepted.lastAnswerCorrect != null) {
                        sounds?.play(if (accepted.lastAnswerCorrect) R.raw.answer_correct else R.raw.answer_wrong)
                    }
                    render()
                }
            }
        }
        screen.root.findViewById<View>(R.id.fractions_back).setOnClickListener {
            sounds?.play(R.raw.key_press)
            leaveGame()
        }
        screen.executePendingBindings()
        // Rendering/serialization time must not be added to the 500 ms feedback interval.
        if (!initial) {
            handler.postDelayed(update, (state.nextUpdateElapsedMs - SystemClock.elapsedRealtime()).coerceAtLeast(0))
        }
    }

    private fun screen(layout: Int): ViewDataBinding {
        if (layoutId != layout) {
            binding?.unbind()
            requireNotNull(content).removeAllViews()
            binding = DataBindingUtil.inflate<ViewDataBinding>(layoutInflater, layout, content, false)
            requireNotNull(content).addView(requireNotNull(binding).root)
            layoutId = layout
        }
        return requireNotNull(binding)
    }

    private fun showResult(state: FractionsGameViewModel.UiState) {
        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.fractionsGameFragment) return
        if (nav.currentBackStackEntry?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != true) {
            handler.postDelayed(update, 16)
            return
        }
        // A restored delivery marker must never deliver the same result a second time.
        if (state.resultNavigated) {
            nav.popBackStack(R.id.fractionsLevelFragment, false)
            return
        }
        val result = requireNotNull(state.result)
        val direction = FractionsGameFragmentDirections.actionFractionsGameFragmentToFractionsResultFragment(
            state.level.name, result.isSuccess, result.answeredCount, result.correctCount,
            state.minimumReached, state.accuracyReached)
        model.markResultNavigated()
        nav.navigate(direction)
    }

    private fun leaveGame() {
        val nav = findNavController()
        if (nav.currentDestination?.id != R.id.fractionsGameFragment) return
        handler.removeCallbacks(update)
        model.cancel()
        nav.popBackStack(R.id.fractionsLevelFragment, false)
    }

    private fun instruction(exercise: FractionExercise<out FractionAnswer>): Int = when (exercise) {
        is FractionExercise.IdentifyFraction -> R.string.fractions_picture_title
        is FractionExercise.IdentifyNumerator -> R.string.fractions_numerator_title
        is FractionExercise.IdentifyDenominator -> R.string.fractions_denominator_title
        is FractionExercise.Compare -> R.string.fractions_compare_title
        is FractionExercise.Reduce -> R.string.fractions_reduce_title
        is FractionExercise.Add -> R.string.fractions_add_title
        is FractionExercise.Subtract -> R.string.fractions_subtract_title
        is FractionExercise.ToMixedNumber -> R.string.fractions_mixed_title
    }

    private fun levelLabel(level: Level) = when (level) {
        Level.TEST -> R.string.fractions_test
        Level.EASY -> R.string.fractions_easy
        Level.NORMAL -> R.string.fractions_normal
        Level.HARD -> R.string.fractions_hard
    }

    private fun sign(value: FractionAnswer.ComparisonValue) = getString(when (value) {
        FractionAnswer.ComparisonValue.LESS -> R.string.fractions_less
        FractionAnswer.ComparisonValue.EQUAL -> R.string.fractions_equal
        FractionAnswer.ComparisonValue.GREATER -> R.string.fractions_greater
    })

    override fun onDestroyView() {
        handler.removeCallbacks(update)
        sounds?.release()
        sounds = null
        errorDialog?.dismiss()
        errorDialog = null
        binding?.unbind()
        binding = null
        content = null
        layoutId = 0
        super.onDestroyView()
    }
}
