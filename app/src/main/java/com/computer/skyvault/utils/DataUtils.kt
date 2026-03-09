package com.computer.skyvault.utils

import android.util.Log
import androidx.appcompat.widget.AppCompatImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.computer.skyvault.R
import com.computer.skyvault.common.enums.FileTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.service.client.FileInfoServiceClient
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

    fun setFileCoverByType(
        fileType: FileTypeEnum?,
        item: FileItem,
        imageView: AppCompatImageView
    ) {

        when (fileType) {
            FileTypeEnum.IMAGE, FileTypeEnum.VIDEO -> {
                val imageUrl = FileInfoServiceClient.getFileCoverPath(item.fileCover.toString())

                // 使用 Glide 加载封面图（仅 IMAGE/VIDEO）
                Glide.with(imageView)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_file_type_folder)  // 加载中占位图
                    .error(R.drawable.ic_file_type_other)         // 失败回退图
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(imageView)
            }

            else -> {
                // 其他类型：直接设置静态图标资源
                val iconRes = when (fileType) {
                    FileTypeEnum.AUDIO -> R.drawable.ic_file_type_audio
                    FileTypeEnum.PDF -> R.drawable.ic_file_type_pdf
                    FileTypeEnum.WORD -> R.drawable.ic_file_type_word
                    FileTypeEnum.EXCEL -> R.drawable.ic_file_type_excel
                    FileTypeEnum.TXT -> R.drawable.ic_file_type_txt
                    FileTypeEnum.CODE -> R.drawable.ic_file_type_code
                    FileTypeEnum.ZIP -> R.drawable.ic_file_type_zip
                    FileTypeEnum.APP -> R.drawable.ic_file_type_app
                    FileTypeEnum.BT_SEEDS -> R.drawable.ic_file_type_bt
                    FileTypeEnum.OTHERS -> R.drawable.ic_file_type_other
                    else -> R.drawable.ic_file_type_folder
                }
                imageView.setImageResource(iconRes)
            }
        }
    }
}

