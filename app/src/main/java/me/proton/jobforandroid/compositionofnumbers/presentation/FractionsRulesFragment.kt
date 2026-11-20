package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.proton.jobforandroid.compositionofnumbers.R

/** Four real rules layouts share one Fragment; no game is generated here. */
class FractionsRulesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = when (arguments?.getString("level") ?: "TEST") {
            "TEST" -> R.layout.ncs_v2_fractions_rules_test
            "EASY" -> R.layout.ncs_v2_fractions_rules_easy
            "NORMAL" -> R.layout.ncs_v2_fractions_rules_normal
            "HARD" -> R.layout.ncs_v2_fractions_rules_hard
            else -> error("Unknown fractions rules level")
        }
        return inflater.inflate(layout, container, false)
    }
}
