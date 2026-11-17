package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ComparisonResultBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonSessionSettings

class ComparisonResultFragment : Fragment() {
    private var _binding: NcsV2ComparisonResultBinding? = null
    private val binding get() = _binding!!
    private var navigationStarted = false
    private var sounds: UiSoundPool? = null
    private var resultSoundTriggered = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2ComparisonResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        val level = args.getSerializable("level") as ComparisonLevel
        val result = ComparisonResult(
            level,
            args.getInt("answeredCount"),
            args.getInt("correctCount")
        )
        sounds = UiSoundPool(
            requireContext(), R.raw.key_press,
            if (result.isSuccess) R.raw.game_win else R.raw.game_lose
        )
        resultSoundTriggered = resultSoundTriggered ||
            (savedInstanceState?.getBoolean(RESULT_SOUND_TRIGGERED) == true)
        if (!resultSoundTriggered) {
            resultSoundTriggered = true
            sounds?.play(if (result.isSuccess) R.raw.game_win else R.raw.game_lose)
        }
        binding.ncsV2ComparisonResultRetryButton.installButtonPressAnimation()
        binding.ncsV2ComparisonResultChooseLevelButton.installButtonPressAnimation()
        val settings = ComparisonSessionSettings.forLevel(level)
        val minimumAchieved = result.answeredCount >= settings.minimumAnswers
        val accuracyAchieved = result.accuracyPercent >= settings.requiredAccuracyPercent
        binding.success = result.isSuccess
        binding.levelLabel = getString(when (level) {
            ComparisonLevel.TEST -> R.string.ncs_v2_test_level
            ComparisonLevel.EASY -> R.string.ncs_v2_easy_level
            ComparisonLevel.NORMAL -> R.string.ncs_v2_medium_level
            ComparisonLevel.HARD -> R.string.ncs_v2_hard_level
        })
        binding.descriptionText = if (result.isSuccess) {
            getString(R.string.ncs_v2_comparison_result_success_description)
        } else {
            getString(
                R.string.ncs_v2_comparison_result_failure_dynamic,
                settings.minimumAnswers,
                settings.requiredAccuracyPercent
            )
        }
        binding.countText = "${result.correctCount} / ${result.answeredCount}"
        binding.accuracyText = getString(R.string.ncs_v2_percentage, result.accuracyPercent)
        binding.minimumAchieved = minimumAchieved
        binding.accuracyAchieved = accuracyAchieved
        binding.minimumStatusText = getString(
            if (minimumAchieved) R.string.ncs_v2_comparison_result_minimum_met_dynamic
            else R.string.ncs_v2_comparison_result_minimum_missing_dynamic,
            settings.minimumAnswers
        )
        binding.accuracyStatusText = getString(
            if (accuracyAchieved) R.string.ncs_v2_comparison_result_accuracy_met_dynamic
            else R.string.ncs_v2_comparison_result_accuracy_missing_dynamic,
            settings.requiredAccuracyPercent
        )
        binding.ncsV2ComparisonResultRetryButton.setOnClickListener {
            if (navigationStarted) return@setOnClickListener
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.comparisonResultFragment) return@setOnClickListener
            sounds?.play(R.raw.key_press)
            navigationStarted = true
            binding.ncsV2ComparisonResultRetryButton.isEnabled = false
            binding.ncsV2ComparisonResultChooseLevelButton.isEnabled = false
            navController.navigate(
                R.id.action_comparisonResultFragment_to_comparisonGameFragment,
                Bundle().apply { putSerializable("level", level) }
            )
        }
        binding.ncsV2ComparisonResultChooseLevelButton.setOnClickListener {
            if (navigationStarted) return@setOnClickListener
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.comparisonResultFragment) return@setOnClickListener
            sounds?.play(R.raw.key_press)
            navigationStarted = true
            binding.ncsV2ComparisonResultRetryButton.isEnabled = false
            binding.ncsV2ComparisonResultChooseLevelButton.isEnabled = false
            navController.popBackStack(R.id.comparisonLevelFragment, false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(RESULT_SOUND_TRIGGERED, resultSoundTriggered)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.ncsV2ComparisonResultRetryButton.cancelButtonPressAnimation()
        binding.ncsV2ComparisonResultChooseLevelButton.cancelButtonPressAnimation()
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val RESULT_SOUND_TRIGGERED = "comparison_result_sound_triggered"
    }
}
