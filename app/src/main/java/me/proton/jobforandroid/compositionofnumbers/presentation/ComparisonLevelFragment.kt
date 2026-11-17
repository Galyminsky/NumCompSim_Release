package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2ComparisonLevelBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.ComparisonLevel

class ComparisonLevelFragment : Fragment() {
    private var _binding: NcsV2ComparisonLevelBinding? = null
    private val binding get() = _binding!!
    private var selectedLevel = ComparisonLevel.NORMAL
    private var sounds: UiSoundPool? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2ComparisonLevelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2ComparisonLevelStartButton.installButtonPressAnimation()
        binding.ncsV2ComparisonLevelBackButton.installButtonPressAnimation()
        savedInstanceState?.getString(SELECTED_LEVEL_KEY)?.let {
            selectedLevel = ComparisonLevel.valueOf(it)
        }
        binding.selectedLevel = selectedLevel
        binding.ncsV2ComparisonTestLevel.root.setOnClickListener { selectLevel(ComparisonLevel.TEST) }
        binding.ncsV2ComparisonEasyLevel.root.setOnClickListener { selectLevel(ComparisonLevel.EASY) }
        binding.ncsV2ComparisonNormalLevel.root.setOnClickListener { selectLevel(ComparisonLevel.NORMAL) }
        binding.ncsV2ComparisonHardLevel.root.setOnClickListener { selectLevel(ComparisonLevel.HARD) }
        binding.ncsV2ComparisonLevelStartButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.comparisonLevelFragment) {
                sounds?.play(R.raw.key_press)
                navController.navigate(
                    R.id.action_comparisonLevelFragment_to_comparisonGameFragment,
                    Bundle().apply { putSerializable("level", selectedLevel) }
                )
            }
        }
        binding.ncsV2ComparisonLevelBackButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.comparisonLevelFragment) {
                sounds?.play(R.raw.key_press)
                navController.popBackStack()
            }
        }
    }

    private fun selectLevel(level: ComparisonLevel) {
        sounds?.play(R.raw.key_press)
        selectedLevel = level
        binding.selectedLevel = level
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SELECTED_LEVEL_KEY, selectedLevel.name)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        binding.ncsV2ComparisonLevelStartButton.cancelButtonPressAnimation()
        binding.ncsV2ComparisonLevelBackButton.cancelButtonPressAnimation()
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SELECTED_LEVEL_KEY = "comparison_selected_level"
    }
}
