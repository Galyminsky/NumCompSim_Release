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
import androidx.navigation.fragment.navArgs
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2LevelSelectionBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GetGameSettingsUseCase

class ChooseLevelFragment : Fragment() {
    private val args by navArgs<ChooseLevelFragmentArgs>()
    private var _binding: NcsV2LevelSelectionBinding? = null
    private val binding get() = _binding!!
    private val getGameSettings = GetGameSettingsUseCase(GameRepositoryImpl)
    private var selectedLevel = Level.TEST
    private var sounds: UiSoundPool? = null
    private var animatedCard: View? = null
    private var colorAnimator: ValueAnimator? = null
    private var scaleAnimator: ValueAnimator? = null
    private var selectionInProgress = false
    private var startPending = false
    private var navigationStarted = false
    private val finishSelection = Runnable {
        selectionInProgress = false
        if (startPending) {
            startPending = false
            launchGameFragment(selectedLevel)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2LevelSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigationStarted = false
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2BackButton.installButtonPressAnimation { !selectionInProgress && !navigationStarted }
        binding.ncsV2StartGameButton.installButtonPressAnimation()
        val savedLevel: Level? = findNavController().currentBackStackEntry
            ?.savedStateHandle?.get(SELECTED_LEVEL_KEY)
        selectedLevel = savedLevel ?: Level.TEST

        with(binding) {
            selectedLevel = this@ChooseLevelFragment.selectedLevel
            easyRange = levelRange(Level.EASY)
            normalRange = levelRange(Level.NORMAL)
            hardRange = levelRange(Level.HARD)

            ncsV2TestLevelItem.root.setOnClickListener { selectLevel(Level.TEST) }
            ncsV2EasyLevelItem.root.setOnClickListener { selectLevel(Level.EASY) }
            ncsV2MediumLevelItem.root.setOnClickListener { selectLevel(Level.NORMAL) }
            ncsV2HardLevelItem.root.setOnClickListener { selectLevel(Level.HARD) }
            ncsV2StartGameButton.setOnClickListener { launchGameFragment(this@ChooseLevelFragment.selectedLevel) }
            ncsV2BackButton.setOnClickListener {
                if (selectionInProgress || navigationStarted) return@setOnClickListener
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.chooseLevelFragment2) {
                    sounds?.play(R.raw.key_press)
                    navController.popBackStack()
                }
            }
        }
    }

    private fun levelRange(level: Level): String =
        getString(R.string.ncs_v2_level_range, 0, getGameSettings(level).maxSumValue)

    private fun selectLevel(level: Level) {
        if (selectionInProgress || navigationStarted) return
        sounds?.play(R.raw.key_press)
        val previousLevel = selectedLevel
        selectedLevel = level
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SELECTED_LEVEL_KEY, level)
        binding.selectedLevel = level
        binding.executePendingBindings()
        val card = when (level) {
            Level.TEST -> binding.ncsV2TestLevelItem.root
            Level.EASY -> binding.ncsV2EasyLevelItem.root
            Level.NORMAL -> binding.ncsV2MediumLevelItem.root
            Level.HARD -> binding.ncsV2HardLevelItem.root
        }
        animatedCard = card
        selectionInProgress = true
        val startColor = ContextCompat.getColor(
            requireContext(), if (previousLevel == level) R.color.ncs_v2_selected else R.color.ncs_v2_surface
        )
        val endColor = ContextCompat.getColor(requireContext(), R.color.ncs_v2_selected)
        val fill = requireNotNull(
            ContextCompat.getDrawable(requireContext(), R.drawable.ncs_v2_selected_outline)
        ).mutate() as GradientDrawable
        fill.setColor(startColor)
        card.background = fill
        colorAnimator = ValueAnimator.ofArgb(startColor, endColor).apply {
            duration = SELECTION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { fill.setColor(it.animatedValue as Int) }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    fill.setColor(endColor)
                    card.setBackgroundResource(R.drawable.ncs_v2_level_card)
                }
            })
            start()
        }
        scaleAnimator = ValueAnimator.ofFloat(1f, 0.96f, 1f).apply {
            duration = SELECTION_DURATION_MS
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                val scale = it.animatedValue as Float
                card.scaleX = scale
                card.scaleY = scale
            }
            start()
        }
        binding.root.postDelayed(finishSelection, SELECTION_DURATION_MS + SELECTED_HOLD_MS)
    }

    private fun launchGameFragment(level: Level) {
        val navController = findNavController()
        if (navigationStarted || navController.currentDestination?.id != R.id.chooseLevelFragment2) return
        if (selectionInProgress) {
            startPending = true
            return
        }
        navigationStarted = true
        sounds?.play(R.raw.key_press)
        requireNotNull(navController.currentBackStackEntry)
            .savedStateHandle.set(SELECTED_LEVEL_KEY, level)
        navController.navigate(
            ChooseLevelFragmentDirections.actionChooseLevelFragment2ToGameFragment(level)
                .setOperation(Question.Operation.valueOf(args.operation).name))
    }

    override fun onDestroyView() {
        binding.ncsV2BackButton.cancelButtonPressAnimation()
        binding.ncsV2StartGameButton.cancelButtonPressAnimation()
        binding.root.removeCallbacks(finishSelection)
        colorAnimator?.cancel()
        colorAnimator = null
        scaleAnimator?.cancel()
        scaleAnimator = null
        animatedCard?.setBackgroundResource(R.drawable.ncs_v2_level_card)
        animatedCard?.scaleX = 1f
        animatedCard?.scaleY = 1f
        animatedCard = null
        selectionInProgress = false
        startPending = false
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SELECTED_LEVEL_KEY = "ncs_v2_selected_level"
        private const val SELECTION_DURATION_MS = 120L
        private const val SELECTED_HOLD_MS = 150L
    }
}
