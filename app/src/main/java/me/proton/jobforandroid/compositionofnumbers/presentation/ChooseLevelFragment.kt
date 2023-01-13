package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import me.proton.jobforandroid.compositionofnumbers.R


class ChooseLevelFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_choose_level, container, false)
    }

    companion object {

        @JvmStatic
        fun newInstance() = ChooseLevelFragment()
    }
}