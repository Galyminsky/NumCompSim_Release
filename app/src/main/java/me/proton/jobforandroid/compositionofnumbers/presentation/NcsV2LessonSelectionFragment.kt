package me.proton.jobforandroid.compositionofnumbers.presentation

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
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

class NcsV2LessonSelectionFragment : Fragment() {
    private var _binding: NcsV2LessonSelectionBinding? = null
    private val binding get() = _binding!!
    private var sounds: UiSoundPool? = null
    private var colorAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null
    private var selectionStarted = false
    private val navigateAfterFeedback = Runnable {
        if (_binding != null &&
            findNavController().currentDestination?.id == R.id.ncsV2LessonSelectionFragment) {
            findNavController().navigate(R.id.action_ncsV2LessonSelectionFragment_to_welcomeFragment)
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
        selectionStarted = false
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2BackButton.installButtonPressAnimation { !selectionStarted }
        binding.ncsV2StartAdditionButton.installButtonPressAnimation { !selectionStarted }
        binding.ncsV2AdditionItem.root.setOnClickListener { selectAddition() }
        binding.ncsV2StartAdditionButton.setOnClickListener { selectAddition() }
        binding.ncsV2BackButton.setOnClickListener {
            if (selectionStarted) return@setOnClickListener
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.ncsV2LessonSelectionFragment) {
                sounds?.play(R.raw.key_press)
                navController.popBackStack()
            }
        }
    }

    private fun selectAddition() {
        if (selectionStarted ||
            findNavController().currentDestination?.id != R.id.ncsV2LessonSelectionFragment) return
        selectionStarted = true
        sounds?.play(R.raw.key_press)
        val card = binding.ncsV2AdditionItem.root
        val selectedColor = ContextCompat.getColor(requireContext(), R.color.ncs_v2_selected)
        val accentColor = ContextCompat.getColor(requireContext(), R.color.ncs_v2_primary)
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
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val FEEDBACK_DURATION_MS = 120L
        private const val SELECTED_HOLD_MS = 150L
    }
}
