package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.proton.jobforandroid.compositionofnumbers.R

/** Level selection is presentation state; navigation is connected in the integration step. */
class FractionsLevelFragment : Fragment() {
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
        selectedLevel = savedInstanceState?.getString("selectedLevel") ?: arguments?.getString("level")
        rows.forEach { (level, row) ->
            row.isSelected = selectedLevel == level
            row.setOnClickListener {
                selectedLevel = level
                rows.forEach { (candidate, item) -> item.isSelected = candidate == level }
            }
        }
        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedLevel?.let { outState.putString("selectedLevel", it) }
    }
}
