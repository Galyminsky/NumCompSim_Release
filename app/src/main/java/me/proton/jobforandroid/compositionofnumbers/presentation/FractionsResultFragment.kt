package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.BR
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.domain.entity.FractionGameSession
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.FractionSessionGenerator.Level

/** Displays the Domain result passed by the owner; replay always gets a new owner. */
class FractionsResultFragment : Fragment() {
    private var binding: ViewDataBinding? = null
    private var sounds: UiSoundPool? = null
    private var resultSoundTriggered = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val args = FractionsResultFragmentArgs.fromBundle(requireArguments())
        val resultSound = if (args.success) R.raw.game_win else R.raw.game_lose
        sounds = UiSoundPool(requireContext(), R.raw.key_press, resultSound)
        resultSoundTriggered = resultSoundTriggered ||
            (savedInstanceState?.getBoolean(RESULT_SOUND_TRIGGERED) == true)
        if (!resultSoundTriggered) {
            resultSoundTriggered = true
            sounds?.play(resultSound)
        }
        val level = Level.valueOf(args.level)
        val settings = FractionGameSession.settingsFor(level)
        val layout = if (args.success) R.layout.ncs_v2_fractions_result_success else R.layout.ncs_v2_fractions_result_failure
        val screen = DataBindingUtil.inflate<ViewDataBinding>(inflater, layout, container, false)
        binding = screen
        screen.setVariable(BR.levelLabel, getString(when (level) {
            Level.TEST -> R.string.fractions_test
            Level.EASY -> R.string.fractions_easy
            Level.NORMAL -> R.string.fractions_normal
            Level.HARD -> R.string.fractions_hard
        }))
        screen.setVariable(BR.minimumReached, args.minimumReached)
        screen.setVariable(BR.accuracyReached, args.accuracyReached)
        screen.setVariable(BR.countText, getString(R.string.fractions_result_count, args.correctCount, args.answeredCount))
        // Formatting only; success and criterion flags came from the game owner.
        val percent = if (args.answeredCount == 0) 0 else args.correctCount * 100 / args.answeredCount
        screen.setVariable(BR.accuracyText, getString(R.string.fractions_percent, percent))
        screen.setVariable(BR.countStatus, getString(R.string.fractions_count_status, settings.minimumAnswers,
            getString(if (args.minimumReached) R.string.fractions_goal_reached else R.string.fractions_goal_missed)))
        screen.setVariable(BR.accuracyStatus, getString(R.string.fractions_accuracy_status, settings.accuracyPercent,
            getString(if (args.accuracyReached) R.string.fractions_goal_reached else R.string.fractions_goal_missed)))
        screen.setVariable(BR.summary, getString(when {
            args.success -> R.string.fractions_success_summary
            !args.minimumReached && !args.accuracyReached -> R.string.fractions_failure_both
            !args.minimumReached -> R.string.fractions_failure_count
            else -> R.string.fractions_failure_accuracy
        }))
        screen.root.findViewById<View>(R.id.fractions_repeat).setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.fractionsResultFragment) {
                sounds?.play(R.raw.key_press)
                nav.navigate(FractionsResultFragmentDirections.actionFractionsResultFragmentToFractionsGameFragment(args.level))
            }
        }
        screen.root.findViewById<View>(R.id.fractions_choose_again).setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.fractionsResultFragment) {
                sounds?.play(R.raw.key_press)
                nav.popBackStack(R.id.fractionsLevelFragment, false)
            }
        }
        screen.executePendingBindings()
        return screen.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(RESULT_SOUND_TRIGGERED, resultSoundTriggered)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        sounds?.release()
        sounds = null
        binding?.unbind()
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val RESULT_SOUND_TRIGGERED = "fractions_result_sound_triggered"
    }
}
