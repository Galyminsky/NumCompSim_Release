package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.google.firebase.analytics.FirebaseAnalytics
import me.proton.jobforandroid.compositionofnumbers.R
import com.google.android.gms.games.PlayGamesSdk
import me.proton.jobforandroid.compositionofnumbers.data.GameRepositoryImpl
import me.proton.jobforandroid.compositionofnumbers.domain.repository.GameRepository

class MainActivity : AppCompatActivity() {
    val gameRepository: GameRepository = GameRepositoryImpl
    private lateinit var analytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = true
        if (Build.VERSION.SDK_INT >= 35) {
            val statusProtection = View(this).apply {
                setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.ncs_v2_paper))
                isClickable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                setOnApplyWindowInsetsListener { view, insets ->
                    val statusHeight = insets.getInsets(WindowInsets.Type.statusBars()).top
                    if (view.layoutParams.height != statusHeight) {
                        view.layoutParams = (view.layoutParams as FrameLayout.LayoutParams).apply {
                            height = statusHeight
                        }
                    }
                    insets
                }
            }
            (window.decorView as FrameLayout).addView(
                statusProtection,
                FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    0,
                    Gravity.TOP
                )
            )
            statusProtection.requestApplyInsets()
        }
        analytics = FirebaseAnalytics.getInstance(this)
        PlayGamesSdk.initialize(this)
    }
}
