package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2LevelSelectionBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.usecases.GetGameSettingsUseCase

class ChooseLevelFragment : Fragment() {
    private var _binding: NcsV2LevelSelectionBinding? = null
    private val binding get() = _binding!!
    private val getGameSettings = GetGameSettingsUseCase(GameRepositoryImpl)
    private var selectedLevel = Level.TEST

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2LevelSelectionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        selectedLevel = findNavController().currentBackStackEntry
            ?.savedStateHandle?.get<Level>(SELECTED_LEVEL_KEY) ?: Level.TEST

        with(binding) {
            selectedLevel = this@ChooseLevelFragment.selectedLevel
            easyRange = levelRange(Level.EASY)
            normalRange = levelRange(Level.NORMAL)
            hardRange = levelRange(Level.HARD)

            ncsV2TestLevelItem.root.setOnClickListener { selectLevel(Level.TEST) }
            ncsV2EasyLevelItem.root.setOnClickListener { selectLevel(Level.EASY) }
            ncsV2MediumLevelItem.root.setOnClickListener { selectLevel(Level.NORMAL) }
            ncsV2HardLevelItem.root.setOnClickListener { selectLevel(Level.HARD) }
            ncsV2StartGameButton.setOnClickListener { launchGameFragment(this@ChooseLevelFragment.selectedLevel) }
            ncsV2BackButton.setOnClickListener {
                val navController = findNavController()
                if (navController.currentDestination?.id == R.id.chooseLevelFragment2) {
                    navController.popBackStack()
                }
            }
        }
    }

    private fun levelRange(level: Level): String =
        getString(R.string.ncs_v2_level_range, 0, getGameSettings(level).maxSumValue)

    private fun selectLevel(level: Level) {
        selectedLevel = level
        findNavController().currentBackStackEntry?.savedStateHandle?.set(SELECTED_LEVEL_KEY, level)
        binding.selectedLevel = level
    }

    private fun launchGameFragment(level: Level) {
        val navController = findNavController()
        if (navController.currentDestination?.id != R.id.chooseLevelFragment2) return
        requireNotNull(navController.currentBackStackEntry)
            .savedStateHandle.set(SELECTED_LEVEL_KEY, level)
        navController.navigate(
            ChooseLevelFragmentDirections.actionChooseLevelFragment2ToGameFragment(level))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val SELECTED_LEVEL_KEY = "ncs_v2_selected_level"
    }
}
