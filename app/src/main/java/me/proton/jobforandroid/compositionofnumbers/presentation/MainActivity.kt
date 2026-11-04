package me.proton.jobforandroid.compositionofnumbers.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import me.proton.jobforandroid.compositionofnumbers.R
import com.google.android.gms.games.PlayGamesSdk

class MainActivity : AppCompatActivity() {
    private lateinit var analytics: FirebaseAnalytics
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        analytics = FirebaseAnalytics.getInstance(this)
        PlayGamesSdk.initialize(this)
    }
}
