package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R

/** Four real rules layouts share one Fragment; no game is generated here. */
class FractionsRulesFragment : Fragment() {
    private var sounds: UiSoundPool? = null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val level = FractionsRulesFragmentArgs.fromBundle(requireArguments()).level
        val layout = when (level) {
            "TEST" -> R.layout.ncs_v2_fractions_rules_test
            "EASY" -> R.layout.ncs_v2_fractions_rules_easy
            "NORMAL" -> R.layout.ncs_v2_fractions_rules_normal
            "HARD" -> R.layout.ncs_v2_fractions_rules_hard
            else -> error("Unknown fractions rules level")
        }
        return inflater.inflate(layout, container, false).also { root ->
            root.findViewById<View>(R.id.fractions_start).setOnClickListener {
                val nav = findNavController()
                if (nav.currentDestination?.id == R.id.fractionsRulesFragment) {
                    sounds?.play(R.raw.key_press)
                    nav.navigate(FractionsRulesFragmentDirections.actionFractionsRulesFragmentToFractionsGameFragment(level))
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
    }

    override fun onDestroyView() {
        sounds?.release()
        sounds = null
        super.onDestroyView()
    }
}
