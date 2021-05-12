package com.example.apptimetrackingstatistics

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_launch.*
import java.io.IOException


class LaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launch)

//        if (checkSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS)
//            != PackageManager.PERMISSION_GRANTED
//        ) {
//            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
//        }
//        if (ContextCompat.checkSelfPermission(
//                this,
//                Manifest.permission.PACKAGE_USAGE_STATS
//            ) != PackageManager.PERMISSION_GRANTED
//        ) {
//            ContextCompat.requestPermissions(
//                arrayOf(Manifest.permission.PACKAGE_USAGE_STATS),
//                1
//            )
//        } else {
        if (!isServiceAlreadyRunning(BackgroundService::class.java))
            startForegroundService(Intent(this, BackgroundService::class.java))
        loadGif(gifimage)
//        }

    }

    private fun loadGif(iv: ImageView) {
        try {
            val source: ImageDecoder.Source = ImageDecoder.createSource(resources, R.drawable.done)
            val drawable: Drawable = ImageDecoder.decodeDrawable(source)
            iv.setImageDrawable(drawable)
            if (drawable is AnimatedImageDrawable) {
                drawable.start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun isServiceAlreadyRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }
}