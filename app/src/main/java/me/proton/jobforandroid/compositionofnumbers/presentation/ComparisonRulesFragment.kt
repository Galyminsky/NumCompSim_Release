package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ComparisonRulesBinding

class ComparisonRulesFragment : Fragment() {
    private var _binding: NcsV2ComparisonRulesBinding? = null
    private val binding get() = _binding!!
    private var sounds: UiSoundPool? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2ComparisonRulesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2ComparisonRulesReadyButton.installButtonPressAnimation()
        binding.ncsV2ComparisonRulesReadyButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.comparisonRulesFragment) {
                sounds?.play(R.raw.key_press)
                navController.navigate(R.id.action_comparisonRulesFragment_to_comparisonLevelFragment)
            }
        }
    }

    override fun onDestroyView() {
        binding.ncsV2ComparisonRulesReadyButton.cancelButtonPressAnimation()
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }
}
