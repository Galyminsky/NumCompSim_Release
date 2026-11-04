package me.proton.jobforandroid.compositionofnumbers.presentation

import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator

private const val PRESS_PHASE_MS = 60L
private const val PRESSED_SCALE = 0.96f

internal fun View.installButtonPressAnimation(canAnimate: () -> Boolean = { true }) {
    setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (view.isEnabled && view.isClickable && canAnimate()) {
                    view.animate().cancel()
                    view.animate().scaleX(PRESSED_SCALE).scaleY(PRESSED_SCALE)
                        .setDuration(PRESS_PHASE_MS)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.animate().cancel()
                if (view.scaleX != 1f || view.scaleY != 1f) {
                    view.animate().scaleX(1f).scaleY(1f)
                        .setDuration(PRESS_PHASE_MS)
                        .setInterpolator(AccelerateDecelerateInterpolator())
                        .start()
                }
            }
        }
        false
    }
}

internal fun View.cancelButtonPressAnimation() {
    setOnTouchListener(null)
    animate().cancel()
    scaleX = 1f
    scaleY = 1f
}
