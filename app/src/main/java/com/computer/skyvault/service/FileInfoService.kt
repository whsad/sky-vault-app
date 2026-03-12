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
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
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
import com.computer.skyvault.utils.ApiClient.okHttpClient
import com.computer.skyvault.utils.ApiClient.onMain
import com.computer.skyvault.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val TAG = "FileInfoService"

/**
 * 文件信息服务 - 处理文件上传、加载列表等业务逻辑
 */
class FileInfoService(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val loginManager: LoginManager,
    private val binding: MyfilesFragmentBinding,
    private val adapter: RecycleItemMyFilesFileFolderListAdapter,
    private val currentFilePidProvider: () -> String
) {
    // 上传相关
    private var uploadFilesAdapter: RecycleItemSelectedFileAdapter? = null
    private val selectedFilesList = mutableListOf<SelectedFile>()

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
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    val fileList = result.data?.list ?: emptyList()
                    Log.d(TAG, "Loaded ${fileList.size} files")
                    onMain {
                        adapter.submitList(fileList)
                        onSuccess?.invoke(fileList)
                    }
                } else {
                    handleApiError(result.code, result.message)
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
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            uris.forEach { uri ->
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            // 安全获取列索引
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                            // 检查索引是否有效
                            if (nameIndex >= 0 && sizeIndex >= 0) {
                                val name = cursor.getString(nameIndex)
                                val size = cursor.getLong(sizeIndex)
                                val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                                selectedFilesList.add(SelectedFile(uri, name, size, mimeType))
                            } else {
                                Log.e(TAG, "无法获取文件信息列索引")
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "解析文件信息失败：${e.message}", e)
                }
            }

            // 在主线程显示对话框
            withContext(Dispatchers.Main) {
                showUploadDialogUI(onFileSelectRequested)
            }
        }
    }

    /**
     * 显示上传对话框 UI
     */
    private fun showUploadDialogUI(onFileSelectRequested: () -> Unit) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.module_dialog_upload_file, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        // 初始化视图
        val rvSelectedFiles = dialogView.findViewById<RecyclerView>(R.id.rvSelectedFiles)
        val btnSelectFiles = dialogView.findViewById<Button>(R.id.btnSelectFiles)
        val btnUpload = dialogView.findViewById<Button>(R.id.btnUpload)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val pbOverall = dialogView.findViewById<ProgressBar>(R.id.pbOverall)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val tvUploadTo = dialogView.findViewById<TextView>(R.id.tvUploadTo)

        // 设置上传位置
        val currentPid = currentFilePidProvider()
        tvUploadTo.text = if (currentPid == "0") {
            "上传到：My Files"
        } else {
            "上传到：当前文件夹"
        }

        // 设置已选文件列表
        rvSelectedFiles.layoutManager = LinearLayoutManager(context)
        uploadFilesAdapter = RecycleItemSelectedFileAdapter(selectedFilesList) { file ->
            showRemoveFileDialog(file)
        }
        rvSelectedFiles.adapter = uploadFilesAdapter

        // 重新选择文件
        btnSelectFiles.setOnClickListener {
            dialog.dismiss()
            onFileSelectRequested.invoke()
        }

        // 开始上传
        btnUpload.setOnClickListener {
            dialog.dismiss()
            if (selectedFilesList.isNotEmpty()) {
                uploadFiles(selectedFilesList, pbOverall, tvProgress) {
                    refreshCurrentFolder()
                }
            } else {
                "请选择要上传的文件".showToast(context)
            }
        }

        // 取消
        btnCancel.setOnClickListener { dialog.dismiss() }

        // 显示对话框
        dialog.show()
    }

    /**
     * 显示移除文件确认对话框
     */
    private fun showRemoveFileDialog(file: SelectedFile) {
        AlertDialog.Builder(context)
            .setTitle("移除文件")
            .setMessage("确定要从列表中移除 \"${file.name}\" 吗？")
            .setPositiveButton("移除") { _, _ ->
                val index = selectedFilesList.indexOfFirst { it.uri == file.uri }
                if (index != -1) {
                    selectedFilesList.removeAt(index)
                    uploadFilesAdapter?.notifyItemRemoved(index)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 显示重命名对话框
     */
    fun showRenameDialog(fileItem: FileItem, onSuccess: (() -> Unit)? = null) {
        val dialogView =
            LayoutInflater.from(context).inflate(R.layout.module_dialog_create_folder, null)
        val operateDesc = dialogView.findViewById<TextView>(R.id.operateDesc)
        val fileCover = dialogView.findViewById<AppCompatImageView>(R.id.fileCover)
        val etFolderName =
            dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFolderName)
        val tilFolderName =
            dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilFolderName)

        // 设置对话框内容
        operateDesc.text = "重命名"
        val typeEnum = FileTypeEnum.getByType(fileItem.fileType)
        com.computer.skyvault.utils.DataUtils.setFileCoverByType(typeEnum, fileItem, fileCover)

        // 分离文件名和后缀
        val fileNameWithoutExtension = fileItem.fileName.substringBeforeLast('.', fileItem.fileName)
        val fileExtension = if (fileItem.fileName.contains('.')) {
            "." + fileItem.fileName.substringAfterLast('.')
        } else {
            ""
        }

        tilFolderName.suffixText = fileExtension
        etFolderName.setText(fileNameWithoutExtension)
        etFolderName.setSelection(fileNameWithoutExtension.length)

        // 显示对话框
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("确定") { _, _ ->
                val newFileName = etFolderName.text.toString().trim()
                when {
                    newFileName.isEmpty() -> tilFolderName.error = "文件名不能为空"
                    newFileName == fileNameWithoutExtension -> "文件名未改变".showToast(context)
                    else -> renameFile(fileItem.fileId, newFileName + fileExtension, onSuccess)
                }
            }
            .setNegativeButton("取消", null)
            .create().apply {
                // 设置圆角
                window?.setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 16f * context.resources.displayMetrics.density
                        setColor(android.graphics.Color.WHITE)
                    }
                )
                show()
            }
    }

    /**
     * 刷新当前文件夹列表
     */
    private fun refreshCurrentFolder() {
        val token = loginManager.getLoginInfo()?.access_token ?: return
        loadFileList(
            LoadFileListRequest(1, 15, "", "all", currentFilePidProvider()),
            token
        )
    }

    /**
     * 上传文件（优化：分块上传、进度显示）
     */
    private fun uploadFiles(
        files: List<SelectedFile>,
        progressBar: ProgressBar,
        progressText: TextView,
        onUploadComplete: () -> Unit
    ) {
        val token = loginManager.getLoginInfo()?.access_token ?: run {
            "请先登录".showToast(context)
            return
        }

        lifecycleOwner.lifecycleScope.launch {
            var uploadedCount = 0
            val totalBytes = files.sumOf { it.size }
            var uploadedBytes = 0L

            files.forEach { file ->
                try {
                    // 复制文件到缓存目录
                    val cacheFile = copyUriToCache(file.uri, file.name) ?: return@forEach

                    // 分块上传
                    val uploadResult = withContext(Dispatchers.IO) {
                        FileInfoServiceClient.uploadFileWithChunks(
                            filePath = cacheFile.absolutePath,
                            fileName = file.name,
                            filePid = currentFilePidProvider(),
                            token = token
                        ) { uploaded, total ->
                            // 更新进度
                            val totalUploaded = uploadedBytes + uploaded
                            val progress = (totalUploaded * 100 / totalBytes).toInt()
                            onMain {
                                progressBar.progress = progress
                                progressText.text = "正在上传：${file.name} ($progress%)"
                            }
                        }
                    }

                    uploadResult.fold(
                        onSuccess = {
                            uploadedBytes += file.size
                            uploadedCount++
                            Log.d(TAG, "文件上传成功: ${file.name}")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "文件上传失败: ${file.name}, ${error.message}")
                            "${file.name} 上传失败：${error.message}".showToast(context)
                        }
                    )

                    // 删除缓存文件
                    cacheFile.delete()

                } catch (e: Exception) {
                    Log.e(TAG, "上传文件异常: ${e.message}", e)
                    "${file.name} 上传异常：${e.message}".showToast(context)
                }
            }

            // 上传完成
            onMain {
                progressBar.progress = 100
                progressText.text = "上传完成"
                "成功上传 $uploadedCount/${files.size} 个文件".showToast(context)
                onUploadComplete()
            }
        }
    }

    /**
     * 复制 URI 到缓存目录
     */
    private fun copyUriToCache(uri: Uri, fileName: String): File? {
        return try {
            val cacheFile = File(context.cacheDir, fileName)

            // 防止文件名重复
            var finalFile = cacheFile
            var counter = 1
            while (finalFile.exists()) {
                val nameWithoutExt = fileName.substringBeforeLast('.')
                val ext = fileName.substringAfterLast('.', "")
                val newName = if (ext.isNotEmpty()) {
                    "$nameWithoutExt($counter).$ext"
                } else {
                    "$nameWithoutExt($counter)"
                }
                finalFile = File(context.cacheDir, newName)
                counter++
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                finalFile.outputStream().use { outputStream ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                    }
                }
            }

            finalFile
        } catch (e: Exception) {
            Log.e(TAG, "复制文件到缓存失败: ${e.message}", e)
            null
        }
    }

    /**
     * 打开文件（预览）
     */
    fun openFile(fileItem: FileItem) {
        // 检查文件状态
        val fileStatus = FileStatusEnum.getByType(fileItem.status)
        when (fileStatus) {
            FileStatusEnum.TRANSCODING -> {
                "文件正在转码中，请稍后再试".showToast(context)
                return
            }
            FileStatusEnum.TRANSCODE_FAILED -> {
                "文件转码失败，无法打开".showToast(context)
                return
            }
            else -> {
                val mimeType = FileTypeEnum.getMimeTypeByType(fileItem.fileType)
                Log.d(TAG, "打开文件：${fileItem.fileName}, MIME: $mimeType")
                downloadAndOpenFile(fileItem, mimeType)
            }
        }
    }

    /**
     * 下载并打开文件（优化：缓存策略、异常处理）
     */
    private fun downloadAndOpenFile(fileItem: FileItem, mimeType: String) {
        "正在准备打开：${fileItem.fileName}".showToast(context)

        lifecycleOwner.lifecycleScope.launch {
            try {
                // 创建预览缓存目录
                val cacheDir = File(context.cacheDir, "preview_files").apply {
                    mkdirs()
                }
                val tempFile = File(cacheDir, fileItem.fileName)

                // 缓存策略：1小时内的文件直接使用
                if (tempFile.exists() && System.currentTimeMillis() - tempFile.lastModified() < 3600000) {
                    Log.d(TAG, "使用缓存文件：${tempFile.absolutePath}")
                    openFileWithIntent(tempFile, mimeType)
                    return@launch
                }

                // 获取下载链接
                val token = loginManager.getLoginInfo()?.access_token ?: run {
                    "请先登录".showToast(context)
                    return@launch
                }

                // 创建下载链接并下载文件
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
        val req = CreateDownloadUrlRequest(fileId = fileId)
        FileInfoServiceClient.createDownloadUrl(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200 && result.data != null) {
                    val downloadCode = result.data
                    val downloadUrl = "${ApiClient.BASE_URL}file/download/$downloadCode"

                    // 下载文件到临时目录
                    downloadFileToTemp(downloadUrl, tempFile) { success, errorMsg ->
                        if (success) {
                            tempFile.setLastModified(System.currentTimeMillis())
                            openFileWithIntent(tempFile, mimeType)
                        } else {
                            "打开文件失败：$errorMsg".showToast(context)
                            Log.e(TAG, "下载失败：$errorMsg")
                        }
                    }
                } else {
                    handleApiError(result.code, result.message)
                }
            },
            onFailure = { error ->
                "网络错误：$error".showToast(context)
                Log.e(TAG, "创建下载链接失败：$error")
            }
        )
    }

    /**
     * 下载文件到临时目录（优化：使用 OkHttp 替代原生 API）
     */
    private fun downloadFileToTemp(url: String, file: File, callback: (Boolean, String?) -> Unit) {
        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                val request = Request.Builder().url(url).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        callback(false, "HTTP ${response.code}")
                    }
                    return@launch
                }

                val body = response.body ?: run {
                    withContext(Dispatchers.Main) {
                        callback(false, "响应体为空")
                    }
                    return@launch
                }

                // 写入文件
                inputStream = body.byteStream()
                outputStream = FileOutputStream(file)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytes += bytesRead
                }

                Log.d(TAG, "下载完成：${file.absolutePath}，大小：$totalBytes 字节")
                withContext(Dispatchers.Main) {
                    callback(true, null)
                }

            } catch (e: Exception) {
                Log.e(TAG, "下载失败：${e.message}", e)
                withContext(Dispatchers.Main) {
                    callback(false, e.message ?: "未知错误")
                }
            } finally {
                // 确保流关闭
                inputStream?.close()
                outputStream?.close()
            }
        }
    }

    /**
     * 使用 Intent 打开文件（优化：兼容性处理）
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

            // 检查是否有应用可以处理
            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )

            if (resolveInfo != null) {
                context.startActivity(intent)
                "正在打开 ${file.name}".showToast(context)
            } else {
                // 尝试通用方式
                intent.setDataAndType(uri, "*/*")
                val genericResolveInfo = context.packageManager.resolveActivity(
                    intent,
                    android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
                )

                if (genericResolveInfo != null) {
                    context.startActivity(intent)
                } else {
                    "没有找到可以打开此文件的应用".showToast(context)
                    "文件已保存到：${file.absolutePath}".showToast(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "打开文件失败：${e.message}", e)
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
        FileInfoServiceClient.newFolder(
            req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "文件夹 '${req.folderName}' 创建成功".showToast(context)
                    onSuccess?.invoke()
                } else {
                    handleApiError(result.code, result.message)
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
        // 校验：不能下载文件夹
        if (fileItem.folderType != null && fileItem.folderType != 0) {
            "无法下载文件夹".showToast(context)
            return
        }

        val token = loginManager.getLoginInfo()?.access_token ?: run {
            "请先登录".showToast(context)
            return
        }

        // 创建下载链接
        val req = CreateDownloadUrlRequest(fileId = fileItem.fileId)
        FileInfoServiceClient.createDownloadUrl(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200 && result.data != null) {
                    val downloadUrl = FileInfoServiceClient.getDownloadUrl(result.data)
                    downloadFileWithOkHttp(downloadUrl, fileItem.fileName, onProgress)
                } else {
                    handleApiError(result.code, result.message)
                }
            },
            onFailure = { error ->
                "网络错误：$error".showToast(context)
                Log.e(TAG, "Create download url failed: $error")
            }
        )
    }

    /**
     * 使用 OkHttp 下载文件
     */
    private fun downloadFileWithOkHttp(
        downloadUrl: String,
        fileName: String,
        onProgress: ((Int) -> Unit)?
    ) {
        Log.d(TAG, "开始下载：$fileName, URL: $downloadUrl")
        "开始下载：$fileName".showToast(context)

        lifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            var inputStream: InputStream? = null
            var outputStream: FileOutputStream? = null

            try {
                // 创建下载目录
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadDir.exists()) downloadDir.mkdirs()

                // 处理文件名重复
                var destFile = File(downloadDir, fileName)
                var counter = 1
                while (destFile.exists()) {
                    val nameWithoutExt = fileName.substringBeforeLast('.')
                    val ext = fileName.substringAfterLast('.', "")
                    val newName = if (ext.isNotEmpty()) {
                        "$nameWithoutExt($counter).$ext"
                    } else {
                        "$nameWithoutExt($counter)"
                    }
                    destFile = File(downloadDir, newName)
                    counter++
                }

                // 执行下载
                val request = Request.Builder().url(downloadUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("下载失败：HTTP ${response.code}")
                }

                val body = response.body ?: throw Exception("响应体为空")
                val totalBytes = body.contentLength()

                // 下载并更新进度
                inputStream = body.byteStream()
                outputStream = FileOutputStream(destFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var downloadedBytes = 0L

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    downloadedBytes += bytesRead

                    // 更新进度
                    if (totalBytes > 0) {
                        val progress = ((downloadedBytes * 100) / totalBytes).toInt()
                        withContext(Dispatchers.Main) {
                            onProgress?.invoke(progress)
                        }
                    }
                }

                // 扫描媒体文件
                scanMediaFile(destFile)

                // 下载完成
                withContext(Dispatchers.Main) {
                    "下载完成：$fileName".showToast(context)
                }

            } catch (e: Exception) {
                Log.e(TAG, "下载失败: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    "下载失败：${e.message}".showToast(context)
                }
            } finally {
                inputStream?.close()
                outputStream?.close()
            }
        }
    }

    /**
     * 扫描媒体文件
     */
    private fun scanMediaFile(file: File) {
        try {
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            mediaScanIntent.data = Uri.fromFile(file)
            context.sendBroadcast(mediaScanIntent)
        } catch (e: Exception) {
            Log.e(TAG, "媒体扫描失败: ${e.message}", e)
        }
    }

    /**
     * 删除文件
     */
    fun deleteFiles(
        fileIds: List<String>,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = loginManager.getLoginInfo()?.access_token ?: run {
            "请先登录".showToast(context)
            return
        }

        if (fileIds.isEmpty()) {
            "请选择要删除的文件".showToast(context)
            return
        }

        val req = DeleteFileRequest(fileIds = fileIds.joinToString(","))
        FileInfoServiceClient.deleteFile(
            req = req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "成功删除 ${fileIds.size} 个文件".showToast(context)
                    onSuccess?.invoke()
                } else {
                    handleApiError(result.code, result.message)
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
    private fun renameFile(
        fileId: String,
        newFileName: String,
        onSuccess: (() -> Unit)? = null
    ) {
        val token = loginManager.getLoginInfo()?.access_token ?: run {
            "请先登录".showToast(context)
            return
        }

        // 校验文件名
        when {
            newFileName.isEmpty() -> {
                "文件名不能为空".showToast(context)
                return
            }
            newFileName.length > 255 -> {
                "文件名长度不能超过 255".showToast(context)
                return
            }
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
                    onMain {
                        onSuccess?.invoke()
                    }
                } else {
                    when (result.code) {
                        409 -> "文件名已存在".showToast(context)
                        else -> handleApiError(result.code, result.message)
                    }
                }
            },
            onFailure = { error ->
                "重命名失败：$error".showToast(context)
                Log.e(TAG, "Rename file failed: $error")
            }
        )
    }

    /**
     * 统一处理 API 错误
     */
    private fun handleApiError(code: Int, message: String?) {
        when (code) {
            401, 403 -> {
                loginManager.clearLoginInfo()
                context.startActivity(Intent(context, LoginActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                "登录已过期，请重新登录".showToast(context)
            }

            404 -> "请求的资源不存在".showToast(context)
            500 -> "服务器内部错误".showToast(context)
            else -> message?.showToast(context) ?: "请求失败".showToast(context)
        }
        Log.w(TAG, "API error - code: $code, message: $message")
    }
}
