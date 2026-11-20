package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R

/** Only the chosen level belongs to this screen; the game is created after the rules. */
class FractionsLevelFragment : Fragment() {
    private var sounds: UiSoundPool? = null
    var selectedLevel: String? = null
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = inflater.inflate(R.layout.ncs_v2_fractions_level, container, false)
        val rows = mapOf(
            "TEST" to root.findViewById<View>(R.id.fractions_level_test),
            "EASY" to root.findViewById<View>(R.id.fractions_level_easy),
            "NORMAL" to root.findViewById<View>(R.id.fractions_level_normal),
            "HARD" to root.findViewById<View>(R.id.fractions_level_hard)
        )
        selectedLevel = savedInstanceState?.getString("selectedLevel") ?: selectedLevel
        rows.forEach { (level, row) ->
            row.isSelected = selectedLevel == level
            row.setOnClickListener {
                sounds?.play(R.raw.key_press)
                selectedLevel = level
                rows.forEach { (candidate, item) -> item.isSelected = candidate == level }
                root.findViewById<View>(R.id.fractions_rules_action).isEnabled = true
            }
        }
        root.findViewById<View>(R.id.fractions_rules_action).apply {
            isEnabled = selectedLevel != null
            setOnClickListener {
                val level = selectedLevel ?: return@setOnClickListener
                val nav = findNavController()
                if (nav.currentDestination?.id == R.id.fractionsLevelFragment) {
                    sounds?.play(R.raw.key_press)
                    nav.navigate(FractionsLevelFragmentDirections.actionFractionsLevelFragmentToFractionsRulesFragment(level))
                }
            }
        }
        root.findViewById<View>(R.id.fractions_back).setOnClickListener {
            val nav = findNavController()
            if (nav.currentDestination?.id == R.id.fractionsLevelFragment) {
                sounds?.play(R.raw.key_press)
                nav.popBackStack()
            }
        }
        return root
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedLevel?.let { outState.putString("selectedLevel", it) }
    }
}
