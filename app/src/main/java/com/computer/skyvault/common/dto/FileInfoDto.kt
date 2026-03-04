package com.computer.skyvault.common.dto

import androidx.annotation.Size

// 分页加载文件列表请求
data class LoadFileListRequest(
    val pageNo: Int = 1,
    @field:Size(min = 1, max = 100)
    val pageSize: Int = 15,
    val fileNameFuzzy: String? = null,
    val category: String? = "all",
    val filePid: String? = "0"
)

// 获取文件封面
data class GetFileCoverRequest(
    val month: String,      // 2026-2
    val imageFolder: String, // videos
    val imageName: String   // fileId.png
)

// 上传文件请求
data class UploadFileRequest(
    val fileId: String? = null,
    val file: ByteArray, // 使用 ByteArray 模拟 UploadFile
    @field:Size(min = 1, max = 255)
    val fileName: String,
    @field:Size(min = 1, max = 20)
    val filePid: String,
    @field:Size(min = 32, max = 32)
    val fileMd5: String,
    val chunkIndex: Int,
    val chunkTotal: Int
)


data class SelectedFile(
    val uri: android.net.Uri,
    val name: String,
    val size: Long,
    val type: String?
)


data class ChunkUploadRequest(
    val file: okhttp3.MultipartBody.Part,
    val fileName: String,
    val filePid: String,
    val fileMd5: String,
    val chunkIndex: Int,
    val chunkTotal: Int,
    val fileId: String
)

// 新建文件夹请求
data class NewFolderRequest(
    @field:Size(min = 1, max = 20)
    val filePid: String,
    @field:Size(min = 1, max = 255)
    val folderName: String
)

// 获取文件夹信息请求
data class GetFolderInfoRequest(
    val folderPath: String
)

// 重命名文件请求
data class RenameRequest(
    @field:Size(min = 1, max = 20)
    val fileId: String,
    @field:Size(min = 1, max = 255)
    val fileName: String
)

// 加载可用文件夹请求
data class LoadAvailableFoldersRequest(
    @field:Size(min = 1, max = 20)
    val filePid: String,
    val excludeFolderIds: String? = null
)

// 更改文件夹请求
data class ChangeFileFolderRequest(
    val fileIds: String,
    @field:Size(min = 1, max = 20)
    val filePid: String
)

// 创建下载链接请求
data class CreateDownloadUrlRequest(
    @field:Size(min = 1, max = 20)
    val fileId: String
)

// 下载文件 DTO
data class DownloadFileDto(
    val downloadCode: String,
    val fileId: String,
    val fileName: String,
    val filePath: String
)

// 下载请求
data class DownloadRequest(
    @field:Size(min = 1, max = 32) // 假设 DOWNLOAD_CODE_LENGTH 为 32
    val code: String
)

// 删除文件请求
data class DeleteFileRequest(
    val fileIds: String
)
