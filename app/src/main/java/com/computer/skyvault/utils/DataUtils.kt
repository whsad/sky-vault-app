package com.computer.skyvault.utils

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.format.DateTimeFormatter
import java.util.Locale

object DataUtils {
    const val TAG = "DataUtil"
    const val LOGIN_STACK = "login_stack"
    val ORIGINAL_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    val TARGET_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a", Locale.ENGLISH)



    inline fun <reified T> parseJsonObj(jsonString: String): T? {
        try {
            val gson = Gson()
            val typeOfT = object : TypeToken<T>() {}.type
            val result = gson.fromJson<T>(jsonString, typeOfT)
            return result
        } catch (e: Exception) {
            Log.e(TAG, "parseJsonObj error: ${e.message}")
            return null
        }
    }

    inline fun <reified T> parseJsonArray(jsonString: String): List<T> {
        val dataList = mutableListOf<T>()
        try {
            val gson = Gson()
            val typeOfT = object : TypeToken<List<T>>() {}.type
            val result = gson.fromJson<List<T>>(jsonString, typeOfT)
            dataList.addAll(result)
        } catch (e: Exception) {
            Log.e(TAG, "parseJsonArray error: ${e.message}")
        }
        return dataList
    }

    inline fun <reified T> toJsonRequestBody(data: T): RequestBody {
        val json = Gson().toJson(data)
        return json.toRequestBody("application/json".toMediaType())
    }

    /**
     * 格式化文件大小
     * @param size 文件大小（字节）
     * @return 格式化后的大小字符串，如 "1.5 MB"
     */
    fun formatFileSize(size: Long): String {
        if (size < 0) {
            return "0 B"
        }

        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        var sizeValue = size.toDouble()
        var unitIndex = 0

        while (sizeValue >= 1024 && unitIndex < units.size - 1) {
            sizeValue /= 1024
            unitIndex++
        }

        return if (unitIndex == 0) {
            "${sizeValue.toInt()} ${units[unitIndex]}"
        } else {
            "%.2f ${units[unitIndex]}".format(Locale.ENGLISH, sizeValue)
        }
    }

}

