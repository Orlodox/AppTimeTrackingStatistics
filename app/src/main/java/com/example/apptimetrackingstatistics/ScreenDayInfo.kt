package com.example.apptimetrackingstatistics

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

@Serializable
class ScreenDayInfo(
    val totalTime: Long,
    val awakeCount: Int,
    val firstAwake: Long,
    val lastAwake: Long,
)