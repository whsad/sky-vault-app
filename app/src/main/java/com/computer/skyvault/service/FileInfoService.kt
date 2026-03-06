package com.computer.skyvault.service

import android.content.Context
import android.net.Uri
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
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemSelectedFileAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.dto.SelectedFile
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.ModuleFragmentMyFilesBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.service.client.FileInfoServiceClient
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.ApiClient.onMain
import com.computer.skyvault.utils.showToast
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
    private val binding: ModuleFragmentMyFilesBinding,
    private val adapter: RecyclerView.Adapter<*>
) {
    private var uploadFilesAdapter: RecycleItemSelectedFileAdapter? = null
    private var selectedFilesList: MutableList<SelectedFile> = mutableListOf()

    /**
     * 打开文件选择器并显示上传对话框
     */
    fun showUploadDialog(uris: List<Uri>) {
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
        val dialogView = LayoutInflater.from(context).inflate(R.layout.module_dialog_upload_file, null)
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

        // 设置上传位置
        tvUploadTo.text = "上传到：我的文件"

        // 重新选择文件
        btnSelectFiles.setOnClickListener {
            dialog.dismiss()
            // 回调到 Fragment 重新打开文件选择器
            onFileSelectRequested?.invoke()
        }

        // 选择文件夹（暂不实现）
        btnSelectFolder.visibility = View.GONE

        // 开始上传
        btnUpload.setOnClickListener {
            dialog.dismiss()
            uploadFiles(selectedFilesList, pbOverall, tvProgress) {
                // 上传完成后刷新文件列表
                val token = loginManager.getLoginInfo()?.access_token
                if (token != null) {
                    loadFileList(
                        LoadFileListRequest(1, 15, "", "all", "0"),
                        token
                    )
                }
            }
        }

        // 取消
        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

    /**
     * 文件选择器回调
     */
    var onFileSelectRequested: (() -> Unit)? = null

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
    fun uploadFiles(
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
                                filePid = "0",
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
                    context.startActivity(android.content.Intent(context, LoginActivity::class.java))
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
}
