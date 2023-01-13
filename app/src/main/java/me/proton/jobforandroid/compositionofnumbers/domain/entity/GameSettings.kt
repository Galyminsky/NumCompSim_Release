package me.proton.jobforandroid.compositionofnumbers.domain.entity

data class GameSettings(

    val maxSumValue: Int,
    val minCountOfRightAnswers: Int,
    val minPercentOfRightAnswers: Int,
    val gameTimeInSeconds: Int,

    )
