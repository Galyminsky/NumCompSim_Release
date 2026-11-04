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
        successBinding?.apply {
            gameResult = result
            levelLabel = getString(levelNameResId(selectedLevel()))
            correctAnswerText = result.countOfRightAnswers.toString()
            accuracyText = getString(
                R.string.ncs_v2_percentage,
                calculatePercentOfRightAnswers(result.countOfRightAnswers, result.countOfQuestions)
            )
            ncsV2ResultPlayAgainButton.setOnClickListener { playAgain() }
            ncsV2ResultChooseLevelButton.setOnClickListener { returnToLevel() }
        }
        failureBinding?.apply {
            val requiredCount = result.gameSettings.minCountOfRightAnswers
            val requiredAccuracy = result.gameSettings.minPercentOfRightAnswers
            val accuracy = calculatePercentOfRightAnswers(
                result.countOfRightAnswers,
                result.countOfQuestions
            )
            val hasCountGoal = result.countOfRightAnswers >= requiredCount
            val hasAccuracyGoal = accuracy >= requiredAccuracy
            levelLabel = getString(levelNameResId(selectedLevel()))
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

    private fun selectedLevel(): Level =
        requireNotNull(
            findNavController().getBackStackEntry(R.id.chooseLevelFragment2)
                .savedStateHandle.get<Level>(SELECTED_LEVEL_KEY)
        ) { "Selected level is missing from the level selection back stack entry" }

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
        val level = selectedLevel()
        retryNavigationStarted = true
        if (navController.popBackStack(R.id.chooseLevelFragment2, false)) {
            navController.navigate(
                ChooseLevelFragmentDirections.actionChooseLevelFragment2ToGameFragment(level)
            )
        } else {
            retryNavigationStarted = false
        }
    }

    private fun returnToLevel() {
        if (retryNavigationStarted) return
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.gameFinishedFragment) return
        retryNavigationStarted = true
        navController.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        successBinding = null
        failureBinding = null
    }

    companion object {
        private const val SELECTED_LEVEL_KEY = "ncs_v2_selected_level"
    }
}
