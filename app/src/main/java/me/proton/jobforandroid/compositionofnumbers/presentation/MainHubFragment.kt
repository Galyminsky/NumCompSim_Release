package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2MainHubBinding

class MainHubFragment : Fragment() {
    private var _binding: NcsV2MainHubBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2MainHubBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.ncsV2HubArithmeticCard.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.mainHubFragment) {
                navController.navigate(R.id.action_mainHubFragment_to_ncsV2LessonSelectionFragment)
            }
        }
        binding.ncsV2HubComparisonCard.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.mainHubFragment) {
                navController.navigate(R.id.action_mainHubFragment_to_comparisonRulesFragment)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
