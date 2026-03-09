package com.computer.skyvault.model

// 传输任务状态
enum class TransferStatus {
    RUNNING, COMPLETED, FAILED, PENDING
}

// 下载任务
data class DownloadTask(
    val id: String,
    val name: String,
    val size: String,
    val progress: Int,
    val speed: String,
    val status: TransferStatus,
    val targetPath: String = "我的资源"
)

// 上传/转存/云添加任务
data class CommonTransferTask(
    val id: String,
    val name: String,
    val size: String,
    val status: TransferStatus,
    val time: String,
    val targetPath: String = "我的资源"
)