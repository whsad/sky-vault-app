package com.computer.skyvault.common.recycleitem

import com.google.gson.annotations.SerializedName

data class FileItem(
    @SerializedName("file_id")
    val fileId: String,
    @SerializedName("file_pid")
    val filePid: String,
    @SerializedName("file_size")
    val fileSize: Long,
    @SerializedName("file_name")
    val fileName: String,
    @SerializedName("file_cover")
    val fileCover: String?,
    @SerializedName("recovery_time")
    val recoveryTime: String?,
    @SerializedName("last_update_time")
    val lastUpdateTime: String?,
    @SerializedName("folder_type")
    val folderType: Int?,
    @SerializedName("file_category")
    val fileCategory: Int?,
    @SerializedName("file_type")
    val fileType: Int?,
    @SerializedName("status")
    val status: Int?,
    val isStarred: Boolean = false,
    val isSelected: Boolean = false,
    val isSelectable: Boolean = true
)