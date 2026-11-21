package me.proton.jobforandroid.compositionofnumbers.presentation

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import kotlin.math.roundToInt

@BindingAdapter("numberAsText")
fun bindNumberAsText(textView: TextView, number: Int) {
    textView.text = number.toString()
}

@BindingAdapter("fractionTopMargin")
fun bindFractionTopMargin(view: View, marginPx: Float) {
    // Data Binding resolves @dimen to pixels already; do not apply density again.
    val params = view.layoutParams as? ViewGroup.MarginLayoutParams ?: return
    val topMargin = marginPx.roundToInt()
    if (params.topMargin != topMargin) {
        params.topMargin = topMargin
        view.layoutParams = params
    }
}

