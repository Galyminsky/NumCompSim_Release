package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.NcsV2GameBinding
import me.proton.jobforandroid.compositionofnumbers.domain.entity.GameResult
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Level
import me.proton.jobforandroid.compositionofnumbers.domain.entity.Question

class GameFragment : Fragment() {
    private val args by navArgs<GameFragmentArgs>()
    private val viewModelFactory by lazy {
        GameViewModelFactory(args.level, requireActivity().application)
    }
    private val viewModel by lazy {
        ViewModelProvider(
            this, viewModelFactory)[GameViewModel::class.java]
    }
    private var _binding: NcsV2GameBinding? = null
    private val binding: NcsV2GameBinding
        get() = _binding ?: throw RuntimeException("NcsV2GameBinding == null")
    private var showingFeedback = false
    private var queuedQuestion: Question? = null
    private val showNextQuestion = Runnable {
        _binding?.let { gameBinding ->
            queuedQuestion?.let { gameBinding.displayedQuestion = it }
            gameBinding.feedbackVisible = false
            gameBinding.selectedOptionIndex = -1
        }
        queuedQuestion = null
        showingFeedback = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = NcsV2GameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        binding.levelContext = getString(
            R.string.ncs_v2_game_level_context,
            getString(levelNameResId(viewModel.level)),
            getString(R.string.ncs_v2_level_range, 0, viewModel.maxSumValue)
        )
        binding.displayedQuestion = viewModel.question.value
        binding.selectedOptionIndex = -1
        binding.feedbackVisible = false
        binding.feedbackCorrect = false
        bindAnswerClicks()
        binding.ncsV2GameBackButton.setOnClickListener {
            val navController = findNavController()
            if (navController.currentDestination?.id == R.id.gameFragment) {
                navController.popBackStack()
            }
        }
        observeViewModel()
    }

    private fun levelNameResId(level: Level): Int = when (level) {
        Level.TEST -> R.string.ncs_v2_test_level
        Level.EASY -> R.string.ncs_v2_easy_level
        Level.NORMAL -> R.string.ncs_v2_medium_level
        Level.HARD -> R.string.ncs_v2_hard_level
    }

    private fun bindAnswerClicks() {
        binding.ncsV2GameAnswer1.root.setOnClickListener { chooseAnswer(0) }
        binding.ncsV2GameAnswer2.root.setOnClickListener { chooseAnswer(1) }
        binding.ncsV2GameAnswer3.root.setOnClickListener { chooseAnswer(2) }
        binding.ncsV2GameAnswer4.root.setOnClickListener { chooseAnswer(3) }
        binding.ncsV2GameAnswer5.root.setOnClickListener { chooseAnswer(4) }
        binding.ncsV2GameAnswer6.root.setOnClickListener { chooseAnswer(5) }
    }

    private fun chooseAnswer(optionIndex: Int) {
        if (showingFeedback || findNavController().currentDestination?.id != R.id.gameFragment) return
        val question = binding.displayedQuestion ?: return
        val answer = question.options.getOrNull(optionIndex) ?: return
        showingFeedback = true
        binding.selectedOptionIndex = optionIndex
        binding.feedbackCorrect = answer == question.rightAnswer
        binding.feedbackVisible = true
        viewModel.chooseAnswer(question, answer)
        binding.root.postDelayed(showNextQuestion, FEEDBACK_DISPLAY_MS)
    }

    private fun observeViewModel() {
        viewModel.question.observe(viewLifecycleOwner) { question ->
            if (showingFeedback) {
                queuedQuestion = question
            } else {
                binding.displayedQuestion = question
            }
        }
        viewModel.gameResult.observe(viewLifecycleOwner) {
            launchGameFinishedFragment(it)
        }
    }

    override fun onDestroyView() {
        binding.root.removeCallbacks(showNextQuestion)
        queuedQuestion = null
        showingFeedback = false
        super.onDestroyView()
        _binding = null
    }

    private fun launchGameFinishedFragment(gameResult: GameResult) {
        if (findNavController().currentDestination?.id != R.id.gameFragment) return
        findNavController().navigate(
            GameFragmentDirections.actionGameFragmentToGameFinishedFragment(gameResult)
        )
    }

    companion object {
        private const val FEEDBACK_DISPLAY_MS = 600L
    }
}

