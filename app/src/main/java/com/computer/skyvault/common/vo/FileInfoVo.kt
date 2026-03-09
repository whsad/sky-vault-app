package com.computer.skyvault.common.vo

import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName
import java.util.Date

// 文件信息 VO
data class FileInfoVo(
    @SerializedName("file_id")
    val fileId: String = "",
    @SerializedName("file_pid")
    val filePid: String = "0",
    @SerializedName("file_size")
    val fileSize: Long? = null,
    @SerializedName("file_name")
    val fileName: String = "",
    @SerializedName("file_cover")
    val fileCover: String? = null,
    @SerializedName("recovery_time")
    val recoveryTime: Date? = null,
    @SerializedName("last_update_time")
    @JsonAdapter(IsoDateDeserializer::class)
    val lastUpdateTime: Date? = null,
    @SerializedName("folder_type")
    val folderType: Int = 0,
    @SerializedName("file_category")
    val fileCategory: Int? = null,
    @SerializedName("file_type")
    val fileType: Int? = null,
    @SerializedName("status")
    val status: Int = 0
)

/**
 * ISO 8601 日期反序列化器
 * 将 "2026-03-09T21:13:02" 格式转换为 Date
 */
class IsoDateDeserializer : com.google.gson.JsonDeserializer<Date?> {
    override fun deserialize(
        json: com.google.gson.JsonElement,
        typeOfT: java.lang.reflect.Type,
        context: com.google.gson.JsonDeserializationContext
    ): Date? {
        return try {
            val dateString = json.asString
            // 处理 ISO 8601 格式：2026-03-09T21:13:02
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoFormat.parse(dateString)
        } catch (e: Exception) {
            try {
                // 尝试其他格式：2026-03-09 21:13:02
                val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                format.parse(json.asString.replace("T", " "))
            } catch (e2: Exception) {
                null
            }
        }
    }
}

// 上传结果 VO
data class UploadResultVo(
    val file_id: String? = null,
    val status: Int? = null
)

// 文件夹 VO
data class FolderVo(
    val fileName: String,
    val fileId: String
)
