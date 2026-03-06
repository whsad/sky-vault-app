package com.computer.skyvault.common.dto



/**
 * 分享文件请求
 */
data class ShareFileRequest(
    val fileId: String,
    val validType: Int, // 有效期类型 0:7 天 1:14 天 2:30 天 3:365 天 4:永久有效
    val code: String? = null // 分享代码，可选
)

/**
 * 分享文件响应
 */
data class ShareFileResponse(
    val shareId: String,
    val fileId: String,
    val userId: String,
    val validType: Int,
    val expireTime: String,
    val shareTime: String,
    val code: String,
    val showCount: Int,
    val shareUrl: String? = null
)