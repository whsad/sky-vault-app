package com.computer.skyvault.manager

import android.content.Context
import android.util.Log
import com.computer.skyvault.model.TransferStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 下载任务管理器 - 单例模式
 * 管理所有下载任务的状态和进度
 */
class DownloadManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: DownloadManager? = null

        fun getInstance(context: Context): DownloadManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: DownloadManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val TAG = "DownloadManager"
    }

    // 所有下载任务列表
    private val _downloadTasks = MutableStateFlow<List<DownloadTaskInfo>>(emptyList())
    val downloadTasks: StateFlow<List<DownloadTaskInfo>> = _downloadTasks.asStateFlow()

    /**
     * 添加下载任务
     */
    fun addDownloadTask(task: DownloadTaskInfo) {
        Log.d(TAG, "添加下载任务：${task.fileName}")
        val currentList = _downloadTasks.value.toMutableList()
        currentList.add(0, task) // 添加到列表开头
        _downloadTasks.value = currentList
    }

    /**
     * 更新下载进度
     */
    fun updateProgress(taskId: String, progress: Int, speed: String = "") {
        Log.d(TAG, "更新进度：$taskId, progress: $progress%, speed: $speed")
        val currentList = _downloadTasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = currentList[index]
            currentList[index] = task.copy(
                progress = progress,
                speed = speed,
                status = if (progress >= 100) TransferStatus.COMPLETED else TransferStatus.RUNNING
            )
            _downloadTasks.value = currentList
        }
    }

    /**
     * 标记任务为完成
     */
    fun markTaskCompleted(taskId: String) {
        Log.d(TAG, "任务完成：$taskId")
        val currentList = _downloadTasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = currentList[index]
            currentList[index] = task.copy(
                progress = 100,
                status = TransferStatus.COMPLETED
            )
            _downloadTasks.value = currentList
        }
    }

    /**
     * 标记任务为失败
     */
    fun markTaskFailed(taskId: String, errorMessage: String = "下载失败") {
        Log.d(TAG, "任务失败：$taskId, error: $errorMessage")
        val currentList = _downloadTasks.value.toMutableList()
        val index = currentList.indexOfFirst { it.id == taskId }
        if (index != -1) {
            val task = currentList[index]
            currentList[index] = task.copy(
                status = TransferStatus.FAILED,
                errorMessage = errorMessage
            )
            _downloadTasks.value = currentList
        }
    }

    /**
     * 清除已完成的任务
     */
    fun clearCompletedTasks() {
        Log.d(TAG, "清除已完成任务")
        val currentList = _downloadTasks.value.filter { it.status != TransferStatus.COMPLETED }
        _downloadTasks.value = currentList
    }

    /**
     * 获取任务数量统计
     */
    fun getTaskStats(): Map<TransferStatus, Int> {
        val list = _downloadTasks.value
        return mapOf(
            TransferStatus.RUNNING to list.count { it.status == TransferStatus.RUNNING },
            TransferStatus.COMPLETED to list.count { it.status == TransferStatus.COMPLETED },
            TransferStatus.FAILED to list.count { it.status == TransferStatus.FAILED }
        )
    }
}

/**
 * 下载任务信息
 */
data class DownloadTaskInfo(
    val id: String,
    val fileName: String,
    val fileSize: Long,
    val fileSizeStr: String,
    val progress: Int = 0,
    val speed: String = "",
    val status: TransferStatus,
    val downloadUrl: String,
    val createTime: Long = System.currentTimeMillis(),
    val errorMessage: String? = null
)
