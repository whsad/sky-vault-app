package com.computer.skyvault.service.client

import com.computer.skyvault.common.dto.CreateDownloadUrlRequest
import com.computer.skyvault.common.dto.DeleteFileRequest
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.dto.RenameRequest
import com.computer.skyvault.common.enums.UploadStatusEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.common.vo.FileInfoVo
import com.computer.skyvault.common.vo.PageResponseResult
import com.computer.skyvault.common.vo.R
import com.computer.skyvault.common.vo.UploadResultVo
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.DataUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest
import kotlin.math.min

private const val TAG = "FileInfoService"

object FileInfoServiceClient {

    private const val PREFIX = "file"
    private const val CHUNK_SIZE = 5 * 1024 * 1024 // 5MB per chunk

    fun loadFileList(
        req: LoadFileListRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (PageResponseResult<FileItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<PageResponseResult<FileItem>>(
            path = "$PREFIX/loadFileList",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }

    private suspend fun calculateFileMd5(filePath: String): String = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: $filePath")
        }

        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }

        return@withContext digest.digest().joinToString("") { "%02x".format(it) }
    }

    suspend fun uploadFileWithChunks(
        filePath: String,
        fileName: String,
        filePid: String,
        token: String,
        onProgress: (uploaded: Long, total: Long) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) {
                return@withContext Result.failure(IllegalArgumentException("File does not exist: $filePath"))
            }

            // 计算文件 MD5
            val fileMd5 = calculateFileMd5(filePath)

            // 计算分片数量
            val totalSize = file.length()
            val chunkTotal = ((totalSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
            var fileId: String? = null

            // 分片上传
            for (chunkIndex in 0 until chunkTotal) {
                val start = chunkIndex * CHUNK_SIZE.toLong()
                val end = min((chunkIndex + 1) * CHUNK_SIZE.toLong(), totalSize)
                val chunkSize = (end - start).toInt()

                // 读取分片数据
                val buffer = ByteArray(chunkSize)
                RandomAccessFile(file, "r").use { raf ->
                    raf.seek(start)
                    raf.readFully(buffer)
                }

                // 构建 Multipart 请求
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart(
                        "file", fileName,
                        buffer.toRequestBody("application/octet-stream".toMediaTypeOrNull(), 0, chunkSize)
                    )
                    .addFormDataPart("fileName", fileName)
                    .addFormDataPart("filePid", filePid)
                    .addFormDataPart("fileMd5", fileMd5)
                    .addFormDataPart("chunkIndex", chunkIndex.toString())
                    .addFormDataPart("chunkTotal", chunkTotal.toString())
                    .also { builder ->
                        fileId?.let { builder.addFormDataPart("fileId", it) }
                    }
                    .build()

                val request = Request.Builder()
                    .url("${ApiClient.BASE_URL}$PREFIX/uploadFile")
                    .post(requestBody)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = ApiClient.okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Upload failed: ${response.code}"))
                }

                val responseBody = response.body?.string()
                val result = DataUtils.parseJsonObj<R<UploadResultVo>>(responseBody.orEmpty())
                if (result?.code != 200) {
                    return@withContext Result.failure(Exception("Upload failed: ${result?.message}, code: ${result?.code}"))
                }

                // 保存 fileId 用于后续分片
                result.data?.file_id?.let { fileId = it }

                // 获取上传状态
                val status = result.data?.status ?: 1

                // 如果是秒传，直接返回
                if (status == UploadStatusEnum.INSTANT_UPLOAD.status) {
                    return@withContext Result.success("Instant upload successfully")
                }

                // 更新进度
                onProgress(end, totalSize)
            }

            Result.success("Upload completed successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getFileCoverPath(
        fileCoverPath: String
    ): String {
        val normalizedPath = fileCoverPath.replace("\\\\", "/").replace("\\", "/")
        return "${ApiClient.BASE_URL}$PREFIX/getFileCover/$normalizedPath"
    }

    fun newFolder(
        req: NewFolderRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (PageResponseResult<FileItem>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<PageResponseResult<FileItem>>(
            path = "$PREFIX/newFolder",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }

    fun createDownloadUrl(
        req: CreateDownloadUrlRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (R<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<String>>(
            path = "$PREFIX/createDownloadUrl",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }

    /**
     * 获取下载 URL（拼接完整的下载链接）
     */
    fun getDownloadUrl(code: String): String {
        return "${ApiClient.BASE_URL}file/download/$code"
    }

    /**
     * 删除文件（移到回收站）
     */
    fun deleteFile(
        req: DeleteFileRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (R<String>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<String>>(
            path = "$PREFIX/delFile",
            method = ApiClient.DELETE,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }

    fun rename(
        req: RenameRequest,
        addHeaders: (Request.Builder) -> Unit = {},
        onSuccess: (R<FileInfoVo>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        ApiClient.sendOkHttpRequest<R<FileInfoVo>>(
            path = "$PREFIX/rename",
            method = ApiClient.POST,
            body = DataUtils.toJsonRequestBody(req),
            addHeaders = addHeaders,
            onFailure = onFailure,
            onResponse = onSuccess
        )
    }
}