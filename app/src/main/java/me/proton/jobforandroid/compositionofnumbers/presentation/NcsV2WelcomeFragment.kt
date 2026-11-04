package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2WelcomeBinding

class NcsV2WelcomeFragment : Fragment() {
    private var _binding: NcsV2WelcomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2WelcomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ncsV2ChooseLessonButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.ncsV2WelcomeFragment) {
                navController.navigate(R.id.action_ncsV2WelcomeFragment_to_ncsV2LessonSelectionFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
