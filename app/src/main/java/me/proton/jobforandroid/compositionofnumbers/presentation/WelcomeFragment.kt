package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.proton.jobforandroid.compositionofnumbers.R
import me.proton.jobforandroid.compositionofnumbers.databinding.FragmentWelcomeBinding

class WelcomeFragment : Fragment() {

    private lateinit var binding: FragmentWelcomeBinding


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWelcomeBinding.inflate(layoutInflater, container, false)
        return binding.root
    }



    companion object {

        @JvmStatic
        fun newInstance() = WelcomeFragment()
    }


}