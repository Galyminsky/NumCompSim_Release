package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import me.proton.jobforandroid.compositionofnumbers.R

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

/** The caller supplies the result state; no accuracy or success calculation is performed here. */
class FractionsResultFragment : Fragment() {
    private var binding: ViewDataBinding? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val layout = if (arguments?.getBoolean("success") == true)
            R.layout.ncs_v2_fractions_result_success else R.layout.ncs_v2_fractions_result_failure
        return DataBindingUtil.inflate<ViewDataBinding>(inflater, layout, container, false).also { binding = it }.root
    }

    override fun onDestroyView() {
        binding?.unbind()
        binding = null
        super.onDestroyView()
    }
}
