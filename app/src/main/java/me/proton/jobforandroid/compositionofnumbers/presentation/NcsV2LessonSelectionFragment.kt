package me.proton.jobforandroid.compositionofnumbers.presentation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2LessonSelectionBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question

class NcsV2LessonSelectionFragment : Fragment() {
    private var selectedOperation = Question.Operation.ADDITION
    private var _binding: NcsV2LessonSelectionBinding? = null
    private val binding get() = _binding!!
    private var sounds: UiSoundPool? = null
    private var colorAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null
    private var selectionStarted = false
    private val navigateAfterFeedback = Runnable {
        if (_binding != null &&
            findNavController().currentDestination?.id == R.id.ncsV2LessonSelectionFragment) {
            findNavController().navigate(when (selectedOperation) {
                Question.Operation.ADDITION -> R.id.action_ncsV2LessonSelectionFragment_to_welcomeFragment
                Question.Operation.SUBTRACTION -> R.id.action_ncsV2LessonSelectionFragment_to_subtractionIntroFragment
                Question.Operation.MULTIPLICATION -> R.id.action_ncsV2LessonSelectionFragment_to_multiplicationIntroFragment
                Question.Operation.DIVISION -> R.id.action_ncsV2LessonSelectionFragment_to_divisionIntroFragment
            })
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2LessonSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedOperation = savedInstanceState?.getString(SELECTED_OPERATION_KEY)
            ?.let(Question.Operation::valueOf) ?: selectedOperation
        selectionStarted = false
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2BackButton.installButtonPressAnimation { !selectionStarted }
        binding.ncsV2StartAdditionButton.installButtonPressAnimation { !selectionStarted }
        binding.selectedOperation = selectedOperation
        binding.ncsV2AdditionItem.root.setOnClickListener { selectLesson(Question.Operation.ADDITION) }
        binding.ncsV2SubtractionItem.root.setOnClickListener { selectLesson(Question.Operation.SUBTRACTION) }
        binding.ncsV2MultiplicationItem.root.setOnClickListener { selectLesson(Question.Operation.MULTIPLICATION) }
        binding.ncsV2DivisionItem.root.setOnClickListener { selectLesson(Question.Operation.DIVISION) }
        binding.ncsV2StartAdditionButton.setOnClickListener { startSelectedLesson() }
        binding.ncsV2AdditionItem.ncsV2LessonSymbolContainer.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ncs_v2_primary))
        binding.ncsV2SubtractionItem.ncsV2LessonSymbolContainer.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.ncs_v2_success))
        binding.ncsV2MultiplicationItem.ncsV2LessonSymbolContainer.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.holo_orange_light))
        binding.ncsV2DivisionItem.ncsV2LessonSymbolContainer.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(requireContext(), R.color.light_red))
        binding.ncsV2BackButton.setOnClickListener {
            if (selectionStarted) return@setOnClickListener
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.ncsV2LessonSelectionFragment) {
                sounds?.play(R.raw.key_press)
                navController.popBackStack()
            }
        }
    }

    private fun selectLesson(operation: Question.Operation) {
        if (selectionStarted ||
            findNavController().currentDestination?.id != R.id.ncsV2LessonSelectionFragment) return
        if (selectedOperation == operation) return
        selectedOperation = operation
        binding.selectedOperation = operation
        sounds?.play(R.raw.key_press)
    }

    private fun startSelectedLesson() {
        if (selectionStarted ||
            findNavController().currentDestination?.id != R.id.ncsV2LessonSelectionFragment) return
        selectionStarted = true
        val operation = selectedOperation
        sounds?.play(R.raw.key_press)
        val card = when (operation) {
            Question.Operation.ADDITION -> binding.ncsV2AdditionItem.root
            Question.Operation.SUBTRACTION -> binding.ncsV2SubtractionItem.root
            Question.Operation.MULTIPLICATION -> binding.ncsV2MultiplicationItem.root
            Question.Operation.DIVISION -> binding.ncsV2DivisionItem.root
        }
        val selectedColor = ContextCompat.getColor(requireContext(), when (operation) {
            Question.Operation.ADDITION -> R.color.ncs_v2_selected
            Question.Operation.SUBTRACTION -> R.color.ncs_v2_success_soft
            Question.Operation.MULTIPLICATION -> R.color.ncs_v2_warm
            Question.Operation.DIVISION -> R.color.ncs_v2_division_soft
        })
        val accentColor = ContextCompat.getColor(requireContext(), when (operation) {
            Question.Operation.ADDITION -> R.color.ncs_v2_primary
            Question.Operation.SUBTRACTION -> R.color.ncs_v2_success
            Question.Operation.MULTIPLICATION -> R.color.holo_orange_light
            Question.Operation.DIVISION -> R.color.light_red
        })
        val fill = requireNotNull(
            ContextCompat.getDrawable(requireContext(), R.drawable.ncs_v2_selected_outline)
        ).mutate() as GradientDrawable
        fill.setColor(selectedColor)
        card.background = fill
        colorAnimator = ValueAnimator.ofArgb(selectedColor, accentColor, selectedColor).apply {
            duration = FEEDBACK_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { fill.setColor(it.animatedValue as Int) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    card.setBackgroundResource(R.drawable.ncs_v2_lesson_card)
                }
            })
            start()
        }
        scaleAnimator = ValueAnimator.ofFloat(1f, 0.96f, 1f).apply {
            duration = FEEDBACK_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                card.scaleX = scale
                card.scaleY = scale
            }
            start()
        }
        binding.root.postDelayed(navigateAfterFeedback, FEEDBACK_DURATION_MS + SELECTED_HOLD_MS)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_OPERATION_KEY, selectedOperation.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.ncsV2BackButton.cancelButtonPressAnimation()
        binding.ncsV2StartAdditionButton.cancelButtonPressAnimation()
        binding.root.removeCallbacks(navigateAfterFeedback)
        colorAnimator?.cancel()
        colorAnimator = null
        scaleAnimator?.cancel()
        scaleAnimator = null
        binding.ncsV2AdditionItem.root.apply {
            setBackgroundResource(R.drawable.ncs_v2_lesson_card)
            scaleX = 1f
            scaleY = 1f
        }
        binding.ncsV2SubtractionItem.root.apply {
            setBackgroundResource(R.drawable.ncs_v2_lesson_card)
            scaleX = 1f
            scaleY = 1f
        }
        binding.ncsV2MultiplicationItem.root.apply {
            setBackgroundResource(R.drawable.ncs_v2_lesson_card)
            scaleX = 1f
            scaleY = 1f
        }
        binding.ncsV2DivisionItem.root.apply {
            setBackgroundResource(R.drawable.ncs_v2_lesson_card)
            scaleX = 1f
            scaleY = 1f
        }
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SELECTED_OPERATION_KEY = "selected_operation"
        private const val FEEDBACK_DURATION_MS = 120L
        private const val SELECTED_HOLD_MS = 150L
    }
}
