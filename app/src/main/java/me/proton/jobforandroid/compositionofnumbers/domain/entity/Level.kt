package me.proton.jobforandroid.compositionofnumbers.domain.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class Level : Parcelable{
    TEST, EASY, NORMAL, HARD;

    val maxFactor: Int
        get() = when (this) {
            TEST -> 3
            EASY -> 5
            NORMAL -> 10
            HARD -> 12
        }
}
