package de.rki.coronawarnapp.util

import android.content.Context
import android.text.TextUtils
import androidx.work.ListenableWorker
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date

object ForegroundPocTracker {

    private const val sharedPrefName = "BackgroundPocPrefs"
    private const val sharedPrefStringsName = "StringList"
    private val sdf: SimpleDateFormat by lazy {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    }

    private val gson: Gson by lazy {
        Gson()
    }

    private val itemType = object : TypeToken<MutableList<String>>() {}.type

    fun save(ctx: Context, name: String, time: Date, result: ListenableWorker.Result) {
        val strings = get(ctx)

        strings.add("Worker $name finished with result $result at ${sdf.format(time)}")
        val json = gson.toJson(strings, itemType)

        ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
            .edit()
            .putString(sharedPrefStringsName, json)
            .apply()
    }

    fun get(ctx: Context): MutableList<String> {
        val json = ctx.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE).getString(sharedPrefStringsName, "")
        val strings: MutableList<String>

        strings = if (TextUtils.isEmpty(json)) {
            mutableListOf()
        } else {
            gson.fromJson<MutableList<String>>(json, itemType)
        }

        return strings
    }
}
