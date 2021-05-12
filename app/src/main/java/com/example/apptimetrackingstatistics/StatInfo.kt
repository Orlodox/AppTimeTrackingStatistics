package com.example.apptimetrackingstatistics

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

@Serializable
class StatInfo(
    var interval: String,
    var info: Array<AppInfo>,
)