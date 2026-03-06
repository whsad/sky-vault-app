package com.computer.skyvault.common.vo

import java.util.Date

// 文件信息 VO
data class FileInfoVo(
    val fileId: String = "",
    val filePid: String = "0",
    val fileSize: Long? = null,
    val fileName: String = "",
    val fileCover: String? = null,
    val recoveryTime: Date? = null,
    val lastUpdateTime: Date? = null,
    val folderType: Int = 0,
    val fileCategory: Int? = null,
    val fileType: Int? = null,
    val status: Int = 0
)

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
