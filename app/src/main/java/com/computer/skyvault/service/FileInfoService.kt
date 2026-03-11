package com.computer.skyvault.service

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemSelectedFileAdapter
import com.computer.skyvault.common.dto.CreateDownloadUrlRequest
import com.computer.skyvault.common.dto.DeleteFileRequest
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.dto.RenameRequest
import com.computer.skyvault.common.dto.SelectedFile
import com.computer.skyvault.common.enums.FileStatusEnum
import com.computer.skyvault.common.enums.FileTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.MyfilesFragmentBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.service.client.FileInfoServiceClient
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.ApiClient
import com.computer.skyvault.utils.ApiClient.onMain
import com.computer.skyvault.utils.showToast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "FileInfoService"

/**
 * 文件信息服务 - 处理文件上传、加载列表等业务逻辑
 */
class FileInfoService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val loginManager: LoginManager,
    private val binding: MyfilesFragmentBinding,
    private val adapter: RecyclerView.Adapter<*>,
    private val currentFilePid: () -> String = { "0" }
) {
    private var uploadFilesAdapter: RecycleItemSelectedFileAdapter? = null
    private var selectedFilesList: MutableList<SelectedFile> = mutableListOf()

    /**
     * 加载文件列表
     */
    fun loadFileList(
        req: LoadFileListRequest,
        token: String,
        onSuccess: ((List<FileItem>) -> Unit)? = null
    ) {
        FileInfoServiceClient.loadFileList(
            req,
            addHeaders = {
                it.addHeader("Authorization", "Bearer $token")
            },
            onSuccess = { result ->
                if (result.code == 200) {
                    result.data?.list?.let { fileList ->
                        Log.d(TAG, "Loaded ${fileList.size} files")
                        // 在主线程中更新适配器
                        onMain {
                            if (adapter is com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter) {
                                adapter.submitList(fileList)
                            }
                        }
                        onSuccess?.invoke(fileList)
                    }
                } else {
                    result.message.showToast(context)
                    loginManager.clearLoginInfo()
                    context.startActivity(Intent(context, LoginActivity::class.java))
                    Log.w(TAG, "Load file list failed with code: ${result.code}")
                }
            },
            onFailure = { error ->
                "Server error. Please try again.".showToast(context)
                Log.e(TAG, "Load file list error: $error")
            }
        )
    }

    /**
     * 打开文件选择器并显示上传对话框
     */
    fun showUploadDialog(uris: List<Uri>, onFileSelectRequested: () -> Unit) {
        selectedFilesList.clear()

        // 获取文件信息
        uris.forEach { uri ->
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    val name = cursor.getString(nameIndex)
                    val size = cursor.getLong(sizeIndex)
                    val mimeType = context.contentResolver.getType(uri)
                    selectedFilesList.add(SelectedFile(uri, name, size, mimeType))
                }
            }
        }

        // 创建并显示对话框
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.module_dialog_upload_file, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val rvSelectedFiles = dialogView.findViewById<RecyclerView>(R.id.rvSelectedFiles)
        val btnSelectFiles = dialogView.findViewById<Button>(R.id.btnSelectFiles)
        val btnSelectFolder = dialogView.findViewById<Button>(R.id.btnSelectFolder)
        val btnUpload = dialogView.findViewById<Button>(R.id.btnUpload)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val pbOverall = dialogView.findViewById<ProgressBar>(R.id.pbOverall)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val tvUploadTo = dialogView.findViewById<TextView>(R.id.tvUploadTo)

        // 设置已选文件列表
        rvSelectedFiles.layoutManager = LinearLayoutManager(context)
        uploadFilesAdapter = RecycleItemSelectedFileAdapter(selectedFilesList) { file ->
            showRemoveFileDialog(file, selectedFilesList, uploadFilesAdapter!!)
        }
        rvSelectedFiles.adapter = uploadFilesAdapter

        // 动态显示上传位置
        val currentPid = currentFilePid()
        tvUploadTo.text = if (currentPid == "0") {
            "上传到：My Files"
        } else {
            "上传到：Current folder"
        }

        // 重新选择文件
        btnSelectFiles.setOnClickListener {
            dialog.dismiss()
            // 回调到 Fragment 重新打开文件选择器
            onFileSelectRequested.invoke()
        }

        // 选择文件夹（暂不实现）
        btnSelectFolder.visibility = View.GONE

        // 开始上传
        btnUpload.setOnClickListener {
            dialog.dismiss()
            uploadFiles(selectedFilesList, pbOverall, tvProgress) {
                // 上传完成后刷新当前文件夹列表
                refreshCurrentFolder()
            }
        }

        // 取消
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * 刷新当前文件夹列表
     */
    private fun refreshCurrentFolder() {
        val token = loginManager.getLoginInfo()?.access_token ?: return
        loadFileList(
            LoadFileListRequest(1, 15, "", "all", currentFilePid()),
            token
        )
    }

    /**
     * 显示移除文件对话框
     */
    private fun showRemoveFileDialog(
        file: SelectedFile,
        files: MutableList<SelectedFile>,
        adapter: RecycleItemSelectedFileAdapter
    ) {
        AlertDialog.Builder(context)
            .setTitle("移除文件")
            .setMessage("确定要从列表中移除 \"${file.name}\" 吗？")
            .setPositiveButton("移除") { _, _ ->
                val index = files.indexOfFirst { it.uri == file.uri }
                if (index != -1) {
                    files.removeAt(index)
                    adapter.notifyItemRemoved(index)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 上传文件
     */
    private fun uploadFiles(
        files: List<SelectedFile>,
        progressBar: ProgressBar,
        progressText: TextView,
        onUploadComplete: () -> Unit
    ) {
        val token = loginManager.getLoginInfo()?.access_token ?: return

        if (files.isEmpty()) {
            "请选择要上传的文件".showToast(context)
            return
        }

        var uploadedCount = 0
        var totalBytes: Long = 0
        var uploadedBytes: Long = 0

        // 计算总大小
        files.forEach { totalBytes += it.size }

        lifecycleOwner.lifecycleScope.launch {
            files.forEachIndexed { index, file ->
                try {
                    // 复制文件到缓存目录
                    val cacheFile = copyUriToCache(file.uri, file.name)

                    if (cacheFile != null && cacheFile.exists()) {
                        // 使用协程上传
                        val result = withContext(Dispatchers.IO) {
                            FileInfoServiceClient.uploadFileWithChunks(
                                filePath = cacheFile.absolutePath,
                                fileName = file.name,
                                filePid = currentFilePid(),
                                token = token
                            ) { uploaded, total ->
                                // 更新单个文件进度
                                val fileProgress = (uploaded + uploadedBytes) * 100 / totalBytes
                                onMain {
                                    progressBar.progress = fileProgress.toInt()
                                    progressText.text = "正在上传：${file.name} ($fileProgress%)"
                                }
                            }
                        }

                        result.fold(
                            onSuccess = { message ->
                                uploadedBytes += file.size
                                uploadedCount++
                                Log.d(TAG, "File uploaded: ${file.name}, $message")
                                // 删除缓存文件
                                cacheFile.delete()
                            },
                            onFailure = { error ->
                                Log.e(TAG, "Upload failed: ${file.name}, ${error.message}")
                                cacheFile.delete()
                            }
                        )
                    } else {
                        Log.e(TAG, "Could not copy file to cache: ${file.uri}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Upload error: ${e.message}", e)
                }
            }

            // 所有文件上传完成
            onMain {
                progressBar.progress = 100
                progressText.text = "上传完成"
                "成功上传 $uploadedCount/${files.size} 个文件".showToast(context)
                onUploadComplete()
            }
        }
    }

    /**
     * 复制 URI 到缓存
     */
    private fun copyUriToCache(uri: Uri, fileName: String): File? {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                cacheFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }
            cacheFile
        } catch (e: Exception) {
            Log.e(TAG, "Error copying file to cache: ${e.message}", e)
            null
        }
    }



    /**
     * 打开文件（预览）
     */
    fun openFile(fileItem: FileItem) {
        // 检查文件状态
        val fileStatusEnum = FileStatusEnum.getByType(fileItem.status)
        when (fileStatusEnum) {
            FileStatusEnum.TRANSCODING -> {
                "文件正在转码中，请稍后再试".showToast(context)
                return
            }

            FileStatusEnum.TRANSCODE_FAILED -> {
                "文件转码失败，无法打开".showToast(context)
                return
            }

            else -> {
                // 根据文件类型打开
                val mimeTypeByType = FileTypeEnum.getMimeTypeByType(fileItem.fileType)
                Log.d(TAG, "打开文件：${fileItem.fileName}, MIME: $mimeTypeByType")

                // 下载并打开文件
                downloadAndOpenFile(fileItem, mimeTypeByType)
            }
        }
    }

    /**
     * 下载并打开文件
     */
    private fun downloadAndOpenFile(fileItem: FileItem, mimeType: String) {
        // 显示加载提示
        "正在准备打开：${fileItem.fileName}".showToast(context)

        lifecycleOwner.lifecycleScope.launch {
            try {
                // 创建缓存目录
                val cacheDir = File(context.cacheDir, "preview_files")
                if (!cacheDir.exists()) {
                    cacheDir.mkdirs()
                }

                val tempFile = File(cacheDir, fileItem.fileName)

                // 如果文件已存在且小于 1 小时，直接打开（避免重复下载）
                if (tempFile.exists() && System.currentTimeMillis() - tempFile.lastModified() < 3600000) {
                    Log.d(TAG, "使用缓存文件：${tempFile.absolutePath}")
                    openFileWithIntent(tempFile, mimeType)
                    return@launch
                }

                // 获取下载链接
                val token = loginManager.getLoginInfo()?.access_token
                if (token == null) {
                    "请先登录".showToast(context)
                    return@launch
                }

                // 第一步：调用后端接口创建下载链接
                createDownloadUrlAndGetFile(fileItem.fileId, token, tempFile, mimeType)

            } catch (e: Exception) {
                Log.e(TAG, "打开文件异常：${e.message}", e)
                "打开文件失败：${e.message}".showToast(context)
            }
        }
    }

    /**
     * 创建下载链接并获取文件
     */
    private fun createDownloadUrlAndGetFile(
        fileId: String,
        token: String,
        tempFile: File,
        mimeType: String
    ) {
        // 第一步：创建下载链接
        val req = CreateDownloadUrlRequest(fileId = fileId)
        FileInfoServiceClient.createDownloadUrl(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200 && result.data != null) {
                    val downloadCode = result.data
                    val downloadUrl = "${ApiClient.BASE_URL}file/download/$downloadCode"

                    Log.d(TAG, "获取下载链接成功：$downloadUrl")

                    // 第二步：下载文件到临时目录
                    downloadFileToTemp(downloadUrl, tempFile) { success, errorMsg ->
                        if (success) {
                            Log.d(TAG, "下载完成，准备打开：${tempFile.absolutePath}")
                            // 更新文件最后修改时间
                            tempFile.setLastModified(System.currentTimeMillis())
                            openFileWithIntent(tempFile, mimeType)
                        } else {
                            "打开文件失败：$errorMsg".showToast(context)
                            Log.e(TAG, "下载失败：$errorMsg")
                        }
                    }
                } else {
                    "获取下载链接失败：${result.message}".showToast(context)
                }
            },
            onFailure = { error ->
                "网络错误：$error".showToast(context)
                Log.e(TAG, "创建下载链接失败：$error")
            }
        )
    }

    /**
     * 下载文件到临时目录
     */
    private fun downloadFileToTemp(url: String, file: File, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var connection: java.net.HttpURLConnection? = null
            try {
                connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                connection.apply {
                    requestMethod = "GET"
                    connectTimeout = 15000
                    readTimeout = 15000
                    doInput = true
                    // 允许重定向
                    instanceFollowRedirects = true
                }

                val responseCode = connection.responseCode
                Log.d(TAG, "下载响应码：$responseCode")

                if (responseCode != 200) {
                    withContext(Dispatchers.Main) {
                        callback(false, "HTTP $responseCode")
                    }
                    return@launch
                }

                // 获取文件大小
                val contentLength = connection.contentLength
                Log.d(TAG, "文件大小：$contentLength bytes")

                if (contentLength <= 0) {
                    Log.w(TAG, "文件大小为 0 或未知")
                }

                // 下载文件
                connection.inputStream.use { input ->
                    file.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var totalBytes = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            totalBytes += bytesRead

                            // 可以在这里更新进度
                            Log.d(TAG, "已下载：$totalBytes / $contentLength")
                        }
                    }
                }

                Log.d(TAG, "下载完成：${file.absolutePath}")

                withContext(Dispatchers.Main) {
                    callback(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败：${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, e.message ?: "未知错误")
                }
            } finally {
                connection?.disconnect()
            }
        }
    }


    /**
     * 使用 Intent 调用其他应用打开文件
     */
    private fun openFileWithIntent(file: File, mimeType: String) {
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            Log.d(TAG, "尝试打开文件：URI=$uri, MIME=$mimeType")

            // 检查是否有应用可以处理该文件类型
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            if (resolveInfo != null) {
                context.startActivity(intent)
                Log.d(TAG, "✅ 成功启动应用打开文件：${file.name}")
                "正在打开 ${file.name}".showToast(context)
            } else {
                // 没有应用可以打开，提示用户
                Log.w(TAG, "⚠️ 没有找到可以打开此类型文件的应用 (MIME: $mimeType)")

                // 尝试使用通用打开方式
                val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                val genericResolveInfo = context.packageManager.resolveActivity(
                    genericIntent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                )
                if (genericResolveInfo != null) {
                    context.startActivity(genericIntent)
                    Log.d(TAG, "✅ 使用通用方式打开文件")
                } else {
                    Log.e(TAG, "❌ 无法打开此文件，没有可用的应用")
                    "没有找到可以打开此文件的应用".showToast(context)

                    // 提示文件保存位置
                    "文件已保存到：${file.absolutePath}".showToast(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ 打开文件失败：${e.message}", e)
            "打开文件失败：${e.message}".showToast(context)
        }
    }

    /**
     * 创建文件夹
     */
    fun createFolder(
        req: NewFolderRequest,
        token: String,
        onSuccess: (() -> Unit)? = null
    ) {
        "filePid: ${req.filePid}, folderName: ${req.folderName}".showToast(context)
        FileInfoServiceClient.newFolder(
            req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "文件夹 '${req.folderName}' 创建成功".showToast(context)
                    onSuccess?.invoke()
                } else {
                    result.message.showToast(context)
                }
            },
            onFailure = { error ->
                "创建失败：$error".showToast(context)
                Log.e(TAG, "Create folder failed: $error")
            }
        )
    }

    /**
     * 下载文件
     */
    fun downloadFile(fileItem: FileItem, onProgress: ((Int) -> Unit)? = null) {
        val token = loginManager.getLoginInfo()?.access_token
        if (token == null) {
            "请先登录".showToast(context)
            return
        }

        // 检查文件夹类型，不能下载文件夹
        if (fileItem.folderType != null && fileItem.folderType != 0) {
            "无法下载文件夹".showToast(context)
            return
        }

        // 第一步：创建下载链接获取下载码
        val req = CreateDownloadUrlRequest(fileId = fileItem.fileId)
        FileInfoServiceClient.createDownloadUrl(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    val downloadCode = result.data
                    if (downloadCode != null) {
                        Log.d(TAG, "Download code created: $downloadCode")
                        val downloadUrl = FileInfoServiceClient.getDownloadUrl(downloadCode)
                        // 第二步：使用下载码下载文件
                        downloadFileWithOkHttp(downloadUrl, fileItem.fileName, onProgress)
                    } else {
                        "获取下载链接失败".showToast(context)
                    }
                } else {
                    result.message.showToast(context)
                    if (result.code == 401 || result.code == 403) {
                        loginManager.clearLoginInfo()
                        context.startActivity(Intent(context, LoginActivity::class.java))
                    }
                }
            },
            onFailure = { error ->
                "网络错误：$error".showToast(context)
                Log.e(TAG, "Create download url failed: $error")
            }
        )
    }

    /**
     * 使用 OkHttp 下载文件（替代 DownloadManager）
     */
    private fun downloadFileWithOkHttp(
        downloadUrl: String,
        fileName: String,
        onProgress: ((Int) -> Unit)?
    ) {
        Log.d(TAG, "开始下载：$fileName")
        Log.d(TAG, "下载 URL: $downloadUrl")

        lifecycleOwner.lifecycleScope.launch {
            try {
                // 创建下载目录
                val downloadDir =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val destFile = File(downloadDir, fileName)

                // 如果文件已存在，先删除
                if (destFile.exists()) {
                    destFile.delete()
                }

                // 创建 OkHttp 客户端
                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                // 创建请求
                val request = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .addHeader("User-Agent", "SkyVault-App/1.0")
                    .addHeader("Connection", "Keep-Alive")
                    .build()

                "开始下载：$fileName".showToast(context)

                // 执行下载
                withContext(Dispatchers.IO) {
                    val response = client.newCall(request).execute()

                    if (!response.isSuccessful) {
                        throw Exception("下载失败：HTTP ${response.code}")
                    }

                    val totalBytes = response.body?.contentLength() ?: 0L
                    if (totalBytes <= 0) {
                        throw Exception("无法获取文件大小")
                    }

                    var downloadedBytes = 0L

                    // 写入文件
                    response.body?.byteStream()?.use { input ->
                        destFile.outputStream().use { output ->
                            val buffer = ByteArray(8192)
                            var bytesRead: Int
                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                output.write(buffer, 0, bytesRead)
                                downloadedBytes += bytesRead

                                // 更新进度
                                val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                                withContext(Dispatchers.Main) {
                                    onProgress?.invoke(progress)
                                    Log.d(TAG, "下载进度：$progress% ($downloadedBytes/$totalBytes)")
                                }
                            }
                        }
                    }

                    Log.d(TAG, "下载完成：${destFile.absolutePath}")

                    // 扫描媒体库，使文件在相册等应用中可见
                    scanMediaFile(destFile)

                    withContext(Dispatchers.Main) {
                        "下载完成：$fileName".showToast(context)
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Download failed: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    "下载失败：${e.message}".showToast(context)
                }
            }
        }
    }

    /**
     * 扫描媒体文件（使下载的文件在相册等应用中可见）
     */
    private fun scanMediaFile(file: File) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = android.net.Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)
            Log.d(TAG, "Media scan completed for: ${file.name}")
        } catch (e: Exception) {
            Log.e(TAG, "Media scan failed: ${e.message}", e)
        }
    }

    /**
     * 删除文件（移到回收站）
     */
    fun deleteFiles(
        fileIds: List<String>,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = loginManager.getLoginInfo()?.access_token
        if (token == null) {
            "请先登录".showToast(context)
            return
        }

        if (fileIds.isEmpty()) {
            "请选择要删除的文件".showToast(context)
            return
        }

        // 将文件 ID 列表转换为逗号分隔的字符串
        val fileIdsStr = fileIds.joinToString(",")
        val req = DeleteFileRequest(fileIds = fileIdsStr)

        FileInfoServiceClient.deleteFile(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "成功删除 ${fileIds.size} 个文件".showToast(context)
                    onSuccess?.invoke()
                } else {
                    result.message.showToast(context)
                }
            },
            onFailure = { error ->
                "删除失败：$error".showToast(context)
                Log.e(TAG, "Delete files failed: $error")
            }
        )
    }

    /**
     * 重命名文件
     */
    fun renameFile(
        fileId: String,
        newFileName: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = loginManager.getLoginInfo()?.access_token
        if (token == null) {
            "请先登录".showToast(context)
            return
        }

        if (fileId.isEmpty()) {
            "文件 ID 不能为空".showToast(context)
            return
        }

        if (newFileName.isEmpty()) {
            "文件名不能为空".showToast(context)
            return
        }

        if (newFileName.length > 255) {
            "文件名长度不能超过 255".showToast(context)
            return
        }

        val req = RenameRequest(
            fileId = fileId,
            fileName = newFileName.trim()
        )

        FileInfoServiceClient.rename(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "重命名成功".showToast(context)
                    Log.d(TAG, "renameFile: $result")
                    onMain {
                        (context as? android.app.Activity)?.runOnUiThread {
                            onSuccess?.invoke()
                        }
                    }
                } else {
                    when (result.code) {
                        409 -> "文件名已存在".showToast(context)
                        else -> result.message.showToast(context)
                    }
                }
            },
            onFailure = { error ->
                "重命名失败：$error".showToast(context)
                Log.e(TAG, "Rename file failed: $error")
            }
        )
    }
}
