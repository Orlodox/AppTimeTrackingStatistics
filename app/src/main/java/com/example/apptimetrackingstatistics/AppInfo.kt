package com.example.apptimetrackingstatistics

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

@Serializable
class AppInfo(
    var appName: String,
    var usagePercentage: Int,
    var usageDuration: Long
)