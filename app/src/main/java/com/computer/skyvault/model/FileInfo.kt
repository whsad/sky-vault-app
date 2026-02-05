package com.computer.skyvault.model

import java.util.Date

data class FileInfo(
    val id: Int? = null,
    val fileId: String,
    val userId: String,
    val fileMd5: String,
    val filePid: String = "0",
    val fileSize: Int = 0,
    val fileName: String,
    val fileCover: String? = null,
    val filePath: String,
    val createTime: Date,
    val lastUpdateTime: Date,
    val folderType: Int = 0, // 0:文件 1:目录
    val fileCategory: Int, // 1:image 2:video 3:audio 4:document 5:app 6:bt seeds 7:other
    val fileType: Int, // 1:图片 2:视频 3:音频 4:pdf 5:word 6:excel 7:txt 8:code 9:zip 10:App 11:BT seeds 12:Other
    val status: Int = 2, // 转码状态
    val recoveryTime: Date? = null, // 回收站时间
    val delFlag: Int = 2 // 删除标记
)