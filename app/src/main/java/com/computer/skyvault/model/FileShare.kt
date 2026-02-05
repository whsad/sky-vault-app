package com.computer.skyvault.model

import java.time.LocalDateTime

/**
 * 文件分享信息实体类
 */
data class FileShare(
    val shareId: String,
    val fileId: String,
    val userId: String,
    val validType: Int, // 有效期类型 0:7天 1:14天 2:30天 3:365天 4:永久有效
    val expireTime: LocalDateTime, // 失效时间
    val shareTime: LocalDateTime, // 分享时间，默认当前时间
    val code: String, // 提取码
    val showCount: Int // 浏览次数，默认为0
)