package com.example.apptimetrackingstatistics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val backgroundServiceIntent = Intent(context, BackgroundService::class.java)
        context.startService(backgroundServiceIntent)
    }

}