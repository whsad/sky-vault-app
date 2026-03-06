package com.computer.skyvault.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Locale

object DateUtils {
    // 移除静态字段，改用函数动态创建 SimpleDateFormat 实例
    private fun getInputFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

    private fun getOutputFormat(): SimpleDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    fun formatIsoDateTime(timeString: String?): String {
        return try {
            timeString?.let { getInputFormat().parse(it) }?.let { getOutputFormat().format(it) }
                ?: "未知时间"
        } catch (e: Exception) {
            Log.w("DateUtils", "Parse failed for '$timeString'", e)
            "未知时间"
        }
    }


}