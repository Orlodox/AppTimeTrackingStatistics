package com.example.apptimetrackingstatistics

import android.app.*
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.IBinder
import android.widget.Toast
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json


class BackgroundService : Service(), OnRequestCompleteListener {

    companion object {
        private const val DAY_IN_MILLIS = 1000 * 3600 * 24L
    }

    lateinit var userStatsManager: UsageStatsManager
    private val repository = Repository(this)

    override fun onCreate() {
        super.onCreate()
        userStatsManager = this.getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
        val notification = createNotification()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_LONG).show()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        GlobalScope.launch {
            repository.sendStat(getData(GregorianCalendar(), UsageStatsManager.INTERVAL_DAILY))
            repository.sendStat(getData(GregorianCalendar(), UsageStatsManager.INTERVAL_WEEKLY))
            repository.sendStat(getData(GregorianCalendar(), UsageStatsManager.INTERVAL_MONTHLY))
            repository.sendScreenDaily(getScreenTodayInfo(GregorianCalendar()))
            while (true) {
                val realtimeInfo = getRealtimeInfo()
                repository.sendRealtimeInfo(getRealtimeInfo())
                println("${realtimeInfo.isScreenOn}  /  ${getDurationBreakdown(realtimeInfo.timeInfo * 1000)}  /  ${realtimeInfo.lastApp}")
                delay(1000)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun getData(date: GregorianCalendar, interval: Int): StatInfo {

        var beginTime: Long = 0
        var endTime: Long = 0
        var intervalName = "";

        when (interval) {
            UsageStatsManager.INTERVAL_DAILY -> {
                intervalName = "day"
                beginTime = date.startDayTimestamp()
                endTime = beginTime + DAY_IN_MILLIS
            }
            UsageStatsManager.INTERVAL_WEEKLY -> {
                intervalName = "week"
                endTime = date.startDayTimestamp()
                beginTime = endTime - DAY_IN_MILLIS * 7
            }
            UsageStatsManager.INTERVAL_MONTHLY -> {
                intervalName = "month"
                endTime = date.startDayTimestamp()
                beginTime = endTime - DAY_IN_MILLIS * 30
            }
        }

        val usageStatList: List<UsageStats> = userStatsManager.queryUsageStats(interval, beginTime, endTime)
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
        val totalTime = usageStatList.sumOf { it.totalTimeInForeground }
        val data = Array(usageStatList.size) { i -> usageStatsToApp(usageStatList[i], totalTime) }
        return StatInfo(intervalName, data)
    }

    private fun usageStatsToApp(usageStats: UsageStats, totalTime: Long): AppInfo {
        val packageName = usageStats.packageName
        val applicationInfo = applicationContext.packageManager.getApplicationInfo(packageName, 0)
        val icon = applicationContext.packageManager.getApplicationIcon(applicationInfo)
        val appName = applicationContext.packageManager.getApplicationLabel(applicationInfo).toString()
        val usageDuration: Long = usageStats.totalTimeInForeground / 1000
        val usagePercentage = (usageStats.totalTimeInForeground * 100 / totalTime).toInt()
        return AppInfo(appName, usagePercentage, usageDuration)
    }

    private fun getDurationBreakdown(millis: Long): String {
        var millis = millis
        require(millis >= 0) { "Duration must be greater than zero!" }
        val hours: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toHours(millis)
        millis -= java.util.concurrent.TimeUnit.HOURS.toMillis(hours)
        val minutes: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toMinutes(millis)
        millis -= java.util.concurrent.TimeUnit.MINUTES.toMillis(minutes)
        val seconds: Long = java.util.concurrent.TimeUnit.MILLISECONDS.toSeconds(millis)
        return "$hours : $minutes : $seconds"
    }


    private fun getScreenTodayInfo(date: GregorianCalendar): ScreenDayInfo {
        val beginTime = date.startDayTimestamp()
        val endTime = beginTime + DAY_IN_MILLIS

        val screenInfo = userStatsManager.queryEventStats(UsageStatsManager.INTERVAL_DAILY, beginTime, endTime)
            .first { it.eventType == UsageEvents.Event.SCREEN_INTERACTIVE }

        val totalTime = 10947L//screenInfo.totalTime / 1000
        var firstAwake = (screenInfo.firstTimeStamp - date.startDayTimestamp()) / 1000
        if (firstAwake < 0) firstAwake += DAY_IN_MILLIS / 1000
        var lastAwake = (screenInfo.lastTimeStamp - date.startDayTimestamp()) / 1000
        if (lastAwake < 0) lastAwake += DAY_IN_MILLIS / 1000

        val screenDayInfo = ScreenDayInfo(totalTime, screenInfo.count, firstAwake, lastAwake)
//        println(Json.encodeToString(screenDayInfo))

        return screenDayInfo
    }

    private fun getRealtimeInfo(): RealtimeInfo {
        val nowTime = System.currentTimeMillis()
        val beginTime = nowTime - DAY_IN_MILLIS

        val usageStatList: List<UsageStats> = userStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, beginTime, nowTime).filter { it.totalTimeInForeground > 0 }
        val lastApp = usageStatList.maxByOrNull { it.lastTimeUsed }
        val appName =
            if (lastApp != null) applicationContext.packageManager.getApplicationLabel(applicationContext.packageManager.getApplicationInfo(lastApp.packageName, 0)).toString()
            else ""

        val screenInfo = userStatsManager.queryEventStats(UsageStatsManager.INTERVAL_DAILY, beginTime, nowTime)
            .filter { it.eventType == UsageEvents.Event.SCREEN_INTERACTIVE || it.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE }
        val lastEvent = screenInfo.maxByOrNull { it.lastEventTime }!!
        val isScreenOn = lastEvent.eventType == UsageEvents.Event.SCREEN_INTERACTIVE

        // продолжительность включенного экрана и текущее приложение или время последнего захода в приложение
        val timeInfo = if (isScreenOn)
            (nowTime - lastEvent.lastEventTime) / 1000 else
            (lastEvent.lastEventTime - GregorianCalendar().startDayTimestamp()) / 1000

//        println(Json.encodeToString(RealtimeInfo(isScreenOn, timeInfo, appName)))
        return RealtimeInfo(isScreenOn, timeInfo, appName)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager;
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent = Intent(this, LaunchActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Notification.Builder(
            this,
            notificationChannelId
        ) else Notification.Builder(this)

        return builder
            .setContentTitle("Parents service")
            .setContentText("This is a service started by your parent")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH)
            .build()
    }

    override fun onSuccess() {
    }

    override fun onError() {
    }

}

fun GregorianCalendar.startDayTimestamp(): Long = GregorianCalendar(get(Calendar.YEAR), get(Calendar.MONTH), get(Calendar.DAY_OF_MONTH)).timeInMillis


