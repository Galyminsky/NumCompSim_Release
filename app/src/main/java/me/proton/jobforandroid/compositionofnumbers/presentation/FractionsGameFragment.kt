package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.proton.jobforandroid.compositionofnumbers.R

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

/** Renders an externally chosen visual format, without owning or creating a game session. */
class FractionsGameFragment : Fragment() {
    private var binding: ViewDataBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = when (arguments?.getString("format") ?: "picture") {
            "picture" -> R.layout.ncs_v2_fractions_game_picture
            "numerator", "denominator" -> R.layout.ncs_v2_fractions_game_numerator
            "comparison" -> R.layout.ncs_v2_fractions_game_comparison
            "arithmetic" -> R.layout.ncs_v2_fractions_game_arithmetic
            "reduction" -> R.layout.ncs_v2_fractions_game_reduction
            "mixed" -> R.layout.ncs_v2_fractions_game_mixed
            "feedback" -> R.layout.ncs_v2_fractions_game_feedback
            else -> error("Unknown fractions visual format")
        }
        return DataBindingUtil.inflate<ViewDataBinding>(inflater, layout, container, false).also { binding = it }.root
    }

    override fun onDestroyView() {
        binding?.unbind()
        binding = null
        super.onDestroyView()
    }
}
