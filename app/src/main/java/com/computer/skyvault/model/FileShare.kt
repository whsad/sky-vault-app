package com.computer.skyvault.model

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

/**
 * 文件分享信息实体类
 */
data class FileShare(
    @SerializedName("share_id")
    val shareId: String,

    @SerializedName("file_id")
    val fileId: String,

    @SerializedName("user_id")
    val userId: String,

    @SerializedName("valid_type")
    val validType: Int,

    @SerializedName("expire_time")
    val expireTime: String,

    @SerializedName("share_time")
    val shareTime: String,

    @SerializedName("code")
    val code: String,

    @SerializedName("show_count")
    val showCount: Int = 0
)