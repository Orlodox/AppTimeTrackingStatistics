package com.example.apptimetrackingstatistics

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

@Serializable
class RealtimeInfo(
    val isScreenOn: Boolean,
    val timeInfo: Long,
    val lastApp: String,
)