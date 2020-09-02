package de.rki.coronawarnapp.util

import android.content.Context
import android.text.TextUtils
import androidx.work.ListenableWorker
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
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

        writeToFile(strings, ctx)
        writeWorkerExecutionToLog(ctx)
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

    private fun writeToFile(strings: List<String>, ctx: Context) {
        CoroutineScope(Dispatchers.IO)
            .launch(Dispatchers.IO) {
                try {
                    val file = File(ctx.filesDir, "WorkerLog.txt")
                    if (!file.exists()) {
                        file.createNewFile()
                    }

                    FileOutputStream(file, false)
                        .bufferedWriter()
                        .use { writer ->
                            writer.appendln("------------------------------------------------------------------------------------------------------------------------------------")
                            writer.appendln(TextUtils.join(System.lineSeparator(), strings))
                            writer.appendln("------------------------------------------------------------------------------------------------------------------------------------")
                            writer.appendln()
                        }
                } catch (e: Exception) {
                    Timber.e(e)
                }
            }
    }

    fun writeWorkerExecutionToLog(ctx: Context) {
        Timber.d("------------------------------------------------------------------------------------------------------------------------------------")
        Timber.d(TextUtils.join(System.lineSeparator(), get(ctx)))
        Timber.d("------------------------------------------------------------------------------------------------------------------------------------")
    }
}
