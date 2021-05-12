package com.example.apptimetrackingstatistics

import android.icu.text.IDNA
import android.provider.SyncStateContract
import android.util.Log
import com.example.apptimetrackingstatistics.AppInfo
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import okhttp3.*
import java.io.IOException;
import okhttp3.MediaType.Companion.toMediaType


class Repository(
    private var onRequestCompleteListener: OnRequestCompleteListener? = null
) : Callback {



    companion object {
        val client = OkHttpClient()
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        val URL = "http://10.0.2.2:5000"
    }

    fun sendStat(stat: StatInfo) {
        val body: RequestBody = RequestBody.create(JSON, Json.encodeToJsonElement(stat).toString())
        val request = Request.Builder()
            .url("$URL/json-${stat.interval}")
            .post(body)
            .build()
        client.newCall(request).enqueue(this)
    }

    fun sendScreenDaily(info: ScreenDayInfo) {
        val body: RequestBody = RequestBody.create(JSON, Json.encodeToJsonElement(info).toString())
        val request = Request.Builder()
            .url("$URL/json-screen-time")
            .post(body)
            .build()
        client.newCall(request).enqueue(this)
    }

    fun sendRealtimeInfo(info: RealtimeInfo) {
        val body: RequestBody = RequestBody.create(JSON, Json.encodeToJsonElement(info).toString())
        val request = Request.Builder()
            .url("$URL/json-real-time")
            .post(body)
            .build()
        client.newCall(request).enqueue(this)
    }

    //    fun sendKey(key: String) {
//        val body: RequestBody = RequestBody.create("text/plain; charset=utf-8".toMediaType(), key)
//        val request = Request.Builder()
//            .url(URL)
//            .put(body)
//            .build()
//        val client = OkHttpClient()
//        GlobalScope.launch { client.newCall(request).execute() }
//    }
//
    override fun onFailure(call: Call, e: IOException) {
        onRequestCompleteListener?.onError()
    }

    override fun onResponse(call: Call, response: Response) {
//        if (response.isSuccessful) {
//            val body = response.body?.string()
//            newInfo = if (body != null) jsonToInfo(body) else MainActivity.actualInfo
//        }
        onRequestCompleteListener?.onSuccess()
    }
//
//    private fun infoToJson(info: IDNA.Info): String {
//        val jsonString = Gson().toJson(info)
//        return jsonString
//    }
//
//    private fun jsonToInfo(jsonString: String): IDNA.Info {
//        val info: IDNA.Info = Gson().fromJson(jsonString, IDNA.Info::class.java)
//        return info
//    }

}

interface OnRequestCompleteListener {
    fun onSuccess()
    fun onError()
}