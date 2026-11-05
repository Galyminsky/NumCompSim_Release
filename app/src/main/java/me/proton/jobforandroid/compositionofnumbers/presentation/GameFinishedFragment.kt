package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ResultFailureBinding
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ResultSuccessBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level

class GameFinishedFragment : Fragment() {
    private val args by navArgs<GameFinishedFragmentArgs>()
    private var successBinding: NcsV2ResultSuccessBinding? = null
    private var failureBinding: NcsV2ResultFailureBinding? = null
    private var retryNavigationStarted = false
    private var sounds: UiSoundPool? = null
    private var resultSoundTriggered = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return if (args.gameResult.winner) {
            NcsV2ResultSuccessBinding.inflate(inflater, container, false)
                .also { successBinding = it }.root
        } else {
            NcsV2ResultFailureBinding.inflate(inflater, container, false)
                .also { failureBinding = it }.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val result = args.gameResult
        sounds = UiSoundPool(
            requireContext(), R.raw.key_press,
            if (result.winner) R.raw.game_win else R.raw.game_lose
        )
        resultSoundTriggered = resultSoundTriggered ||
            (savedInstanceState?.getBoolean(RESULT_SOUND_TRIGGERED) == true)
        if (!resultSoundTriggered) {
            resultSoundTriggered = true
            sounds?.play(if (result.winner) R.raw.game_win else R.raw.game_lose)
        }
        successBinding?.apply {
            ncsV2ResultPlayAgainButton.installButtonPressAnimation()
            ncsV2ResultChooseLevelButton.installButtonPressAnimation()
            gameResult = result
            levelLabel = getString(levelNameResId(args.level))
            correctAnswerText = result.countOfRightAnswers.toString()
            accuracyText = getString(
                R.string.ncs_v2_percentage,
                calculatePercentOfRightAnswers(result.countOfRightAnswers, result.countOfQuestions)
            )
            ncsV2ResultPlayAgainButton.setOnClickListener { playAgain() }
            ncsV2ResultChooseLevelButton.setOnClickListener { returnToLevel() }
        }
        failureBinding?.apply {
            ncsV2ResultFailureRetryButton.installButtonPressAnimation()
            ncsV2ResultFailureChooseLevelButton.installButtonPressAnimation()
            val requiredCount = result.gameSettings.minCountOfRightAnswers
            val requiredAccuracy = result.gameSettings.minPercentOfRightAnswers
            val accuracy = calculatePercentOfRightAnswers(
                result.countOfRightAnswers,
                result.countOfQuestions
            )
            val hasCountGoal = result.countOfRightAnswers >= requiredCount
            val hasAccuracyGoal = accuracy >= requiredAccuracy
            levelLabel = getString(levelNameResId(args.level))
            correctAnswerText = result.countOfRightAnswers.toString()
            accuracyText = getString(R.string.ncs_v2_percentage, accuracy)
            countAchieved = hasCountGoal
            accuracyAchieved = hasAccuracyGoal
            countTargetText = if (hasCountGoal) {
                getString(R.string.ncs_v2_required_reached, requiredCount)
            } else {
                getString(
                    R.string.ncs_v2_required_missing,
                    requiredCount,
                    requiredCount - result.countOfRightAnswers
                )
            }
            accuracyTargetText = if (hasAccuracyGoal) {
                getString(R.string.ncs_v2_accuracy_required_reached, requiredAccuracy)
            } else {
                getString(
                    R.string.ncs_v2_accuracy_required_missing,
                    requiredAccuracy,
                    requiredAccuracy - accuracy
                )
            }
            ncsV2ResultFailureRetryButton.setOnClickListener { playAgain() }
            ncsV2ResultFailureChooseLevelButton.setOnClickListener { returnToLevel() }
        }
    }

    private fun levelNameResId(level: Level): Int = when (level) {
        Level.TEST -> R.string.ncs_v2_test_level
        Level.EASY -> R.string.ncs_v2_easy_level
        Level.NORMAL -> R.string.ncs_v2_medium_level
        Level.HARD -> R.string.ncs_v2_hard_level
    }

    private fun playAgain() {
        if (retryNavigationStarted) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.gameFinishedFragment) return
        val level = args.level
        sounds?.play(R.raw.key_press)
        retryNavigationStarted = true
        if (navController.popBackStack(R.id.chooseLevelFragment2, false)) {
            navController.navigate(
                ChooseLevelFragmentDirections.actionChooseLevelFragment2ToGameFragment(level)
                    .setOperation(args.gameResult.operation.name)
            )
        } else {
            retryNavigationStarted = false
        }
    }

    private fun returnToLevel() {
        if (retryNavigationStarted) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.gameFinishedFragment) return
        sounds?.play(R.raw.key_press)
        retryNavigationStarted = true
        navController.popBackStack()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(RESULT_SOUND_TRIGGERED, resultSoundTriggered)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        successBinding?.apply {
            ncsV2ResultPlayAgainButton.cancelButtonPressAnimation()
            ncsV2ResultChooseLevelButton.cancelButtonPressAnimation()
        }
        failureBinding?.apply {
            ncsV2ResultFailureRetryButton.cancelButtonPressAnimation()
            ncsV2ResultFailureChooseLevelButton.cancelButtonPressAnimation()
        }
        sounds?.release()
        sounds = null
        super.onDestroyView()
        successBinding = null
        failureBinding = null
    }

    companion object {
        private const val RESULT_SOUND_TRIGGERED = "ncs_v2_result_sound_triggered"
    }
}
