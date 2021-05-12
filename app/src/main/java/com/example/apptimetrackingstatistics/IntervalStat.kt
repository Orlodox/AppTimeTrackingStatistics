package com.example.apptimetrackingstatistics

import android.graphics.drawable.Drawable
import kotlinx.serialization.Serializable

@Serializable
class IntervalStat(
    var intervalType: String,
    var begin: Int,
    var usageDuration: Long
)