package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2DivisionIntroBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question

class DivisionIntroFragment : Fragment() {
    private var _binding: NcsV2DivisionIntroBinding? = null
    private val binding get() = requireNotNull(_binding)
    private var sounds: UiSoundPool? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = NcsV2DivisionIntroBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sounds = UiSoundPool(requireContext(), R.raw.key_press)
        binding.ncsV2ReadyButton.installButtonPressAnimation()
        binding.ncsV2ReadyButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id != R.id.divisionIntroFragment) return@setOnClickListener
            sounds?.play(R.raw.key_press)
            navController.navigate(
                R.id.action_divisionIntroFragment_to_chooseLevelFragment2,
                bundleOf("operation" to Question.Operation.DIVISION.name)
            )
        }
    }

    override fun onDestroyView() {
        binding.ncsV2ReadyButton.cancelButtonPressAnimation()
        sounds?.release()
        sounds = null
        super.onDestroyView()
        _binding = null
    }
}
