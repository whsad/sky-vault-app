package com.computer.skyvault.ui.myfiles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.computer.skyvault.MainActivity
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
import com.computer.skyvault.adapter.RecycleItemSelectedFileAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.dto.SelectedFile
import com.computer.skyvault.databinding.ModuleFragmentMyFilesBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.ApiClient.onMain
import com.computer.skyvault.utils.showToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private const val TAG = "MyFilesFragment"

class MyFilesFragment : Fragment() {

    private var _binding: ModuleFragmentMyFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecycleItemMyFilesFileFolderListAdapter
    private lateinit var loginManager: LoginManager

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            showUploadDialog(uris)
        }
    }

    // 上传对话框的适配器
    private var uploadFilesAdapter: RecycleItemSelectedFileAdapter? = null
    private var selectedFilesList: MutableList<SelectedFile> = mutableListOf()

    // 跟踪底部操作栏可见性状态
    private var isBottomActionBarVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModuleFragmentMyFilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 初始化 LoginManager
        loginManager = LoginManager.getInstance(requireActivity().application)

        // 初始化适配器
        adapter = RecycleItemMyFilesFileFolderListAdapter(
            onItemClick = {
                // 处理点击事件
                Log.d(TAG, "Clicked on: ${it.fileName}")
                // 在选择状态下，点击切换选中状态
                if (adapter.isInSelectionMode()) {
                    adapter.toggleSelection(it.fileId)
                } else {
                    // 普通状态下，点击打开文件
                    Log.d(TAG, "Opening file: ${it.fileName}")
                    "Opening: ${it.fileName}".showToast(requireContext())
                }
            },
            onItemLongClick = {
                Log.d(TAG, "Long clicked on: ${it.fileName}")
                // 处理长按事件
                if (!adapter.isInSelectionMode()) {
                    adapter.enterSelectionMode()
                    adapter.toggleSelection(it.fileId)
                }
            }
        )

        // 设置 recycleView
        binding.fileFolderList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MyFilesFragment.adapter
        }

        // 设置底部操作栏点击事件
        setupBottomActionBarListeners()

        // 监听选择状态变化
        adapter.onSelectionModeChanged = { isInSelectionMode, selectedCount ->
            if (isInSelectionMode) {
                showTopSelectionBar(selectedCount)
                showBottomActionBar()
            } else {
                hideTopSelectionBar()
                hideBottomActionBar()
            }
        }

        // 先检查是否有登录信息
        val currentLoginInfo = loginManager.getLoginInfo()
        if (currentLoginInfo != null) {
            Log.d(TAG, currentLoginInfo.access_token)
            loadFileList(
                LoadFileListRequest(
                    pageNo = 1,
                    pageSize = 15,
                    fileNameFuzzy = "",
                    category = "all",
                    filePid = "0"
                ),
                currentLoginInfo.access_token
            )

            binding.btnNewFolder.setOnClickListener {
                showCreateFolderDialog(currentLoginInfo.access_token)
            }
            binding.btnUpload.setOnClickListener {
                // TODO: 实现上传逻辑
                openFilePicker()
            }
        } else {
            // 无登录信息，跳转到登录页
            "请先登录".showToast(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }
    }

    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    private fun showUploadDialog(uris: List<Uri>) {
        val context = requireContext()
        selectedFilesList.clear()

        // 获取文件信息
        uris.forEach { uri ->
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)

                    val name = cursor.getString(nameIndex)
                    val size = cursor.getLong(sizeIndex)

                    // 获取 MIME 类型
                    val mimeType = context.contentResolver.getType(uri)

                    selectedFilesList.add(SelectedFile(uri, name, size, mimeType))
                }
            }
        }

        // 创建并显示对话框
        val dialogView = layoutInflater.inflate(R.layout.module_dialog_upload_file, null)
        val dialog = AlertDialog.Builder(context)
            .setView(dialogView)
            .create()

        val rvSelectedFiles = dialogView.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvSelectedFiles)
        val btnSelectFiles = dialogView.findViewById<Button>(R.id.btnSelectFiles)
        val btnSelectFolder = dialogView.findViewById<Button>(R.id.btnSelectFolder)
        val btnUpload = dialogView.findViewById<Button>(R.id.btnUpload)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
        val pbOverall = dialogView.findViewById<ProgressBar>(R.id.pbOverall)
        val tvProgress = dialogView.findViewById<TextView>(R.id.tvProgress)
        val tvUploadTo = dialogView.findViewById<TextView>(R.id.tvUploadTo)

//        // 设置已选文件列表
//        rvSelectedFiles.layoutManager = LinearLayoutManager(context)
//        val filesAdapter = RecycleItemSelectedFileAdapter(selectedFiles) { file ->
//            // 点击已选文件，可以移除
//            showRemoveFileDialog(file, selectedFiles, filesAdapter)
//        }
//        rvSelectedFiles.adapter = filesAdapter

        // 设置已选文件列表
        rvSelectedFiles.layoutManager = LinearLayoutManager(context)
        uploadFilesAdapter = RecycleItemSelectedFileAdapter(selectedFilesList) { file ->
            // 点击已选文件，可以移除
            showRemoveFileDialog(file, selectedFilesList, uploadFilesAdapter!!)
        }
        rvSelectedFiles.adapter = uploadFilesAdapter

        // 设置上传位置
        tvUploadTo.text = "上传到：我的文件"

        // 重新选择文件
        btnSelectFiles.setOnClickListener {
            dialog.dismiss()
            openFilePicker()
        }

        // 选择文件夹（暂不实现）
        btnSelectFolder.visibility = View.GONE

        // 开始上传
        btnUpload.setOnClickListener {
            dialog.dismiss()
            uploadFiles(selectedFilesList, pbOverall, tvProgress)
        }

        // 取消
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showRemoveFileDialog(file: SelectedFile, files: MutableList<SelectedFile>, adapter: RecycleItemSelectedFileAdapter) {
        AlertDialog.Builder(requireContext())
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

    private fun uploadFiles(files: List<SelectedFile>, progressBar: ProgressBar, progressText: TextView) {
        val token = loginManager.getLoginInfo()?.access_token ?: return

        if (files.isEmpty()) {
            "请选择要上传的文件".showToast(requireContext())
            return
        }

        var uploadedCount = 0
        var totalBytes: Long = 0
        var uploadedBytes: Long = 0

        // 计算总大小
        files.forEach { totalBytes += it.size }

        /*        viewLifecycleOwner.lifecycleScope.launch {
                    files.forEachIndexed { index, file ->
                        try {
                            // 获取文件路径
                            val filePath = getRealPathFromUri(file.uri)

                            if (filePath != null) {
                                // 使用协程上传
                                val result = withContext(Dispatchers.IO) {
                                    FileInfoService.uploadFileWithChunks(
                                        filePath = filePath,
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
                                    },
                                    onFailure = { error ->
                                        Log.e(TAG, "Upload failed: ${file.name}, ${error.message}")
                                    }
                                )
                            } else {
                                Log.e(TAG, "Could not get file path from URI: ${file.uri}")
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Upload error: ${e.message}", e)
                        }
                    }

                    // 所有文件上传完成
                    onMain {
                        progressBar.progress = 100
                        progressText.text = "上传完成"
                        "成功上传 $uploadedCount/${files.size} 个文件".showToast(requireContext())

                        // 刷新文件列表
                        loadFileList(
                            LoadFileListRequest(
                                pageNo = 1,
                                pageSize = 15,
                                fileNameFuzzy = "",
                                category = "all",
                                filePid = "0"
                            ),
                            token
                        )
                    }
                }
            */

        viewLifecycleOwner.lifecycleScope.launch {
            files.forEachIndexed { index, file ->
                try {
                    // 复制文件到缓存目录
                    val cacheFile = copyUriToCache(file.uri, file.name)

                    if (cacheFile != null && cacheFile.exists()) {
                        // 使用协程上传
                        val result = withContext(Dispatchers.IO) {
                            FileInfoService.uploadFileWithChunks(
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
                "成功上传 $uploadedCount/${files.size} 个文件".showToast(requireContext())

                // 刷新文件列表
                loadFileList(
                    LoadFileListRequest(
                        pageNo = 1,
                        pageSize = 15,
                        fileNameFuzzy = "",
                        category = "all",
                        filePid = "0"
                    ),
                    token
                )
            }
        }
    }

    private fun copyUriToCache(uri: Uri, fileName: String): File? {
        return try {
            val context = requireContext()
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

    private fun getRealPathFromUri(uri: Uri): String? {
        val context = requireContext()
        var filePath: String? = null

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Files.FileColumns.DATA)
            if (cursor.moveToFirst()) {
                filePath = cursor.getString(columnIndex)
            }
        }

        return filePath
    }

    private fun showCreateFolderDialog(token: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.module_dialog_create_folder, null)
        dialog.setView(view)

        val etFolderName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFolderName)
        val tilFolderName = view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilFolderName)

        dialog.setPositiveButton("完成") { _, _ ->
            val folderName = etFolderName.text.toString().trim()
            if (folderName.isEmpty()) {
                tilFolderName.error = "文件夹名称不能为空"
                return@setPositiveButton
            }

            // 调用创建文件夹 API
            createFolder(
                NewFolderRequest(
                    "0", folderName
                ), token
            )
        }

        dialog.setNegativeButton("返回") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = dialog.create()
        // 设置圆角
        alertDialog.window?.setBackgroundDrawable(
            android.graphics.drawable.GradientDrawable().apply {
                cornerRadius = 16f * requireContext().resources.displayMetrics.density
                setColor(android.graphics.Color.WHITE)
            }
        )
        alertDialog.show()
    }

    private fun createFolder(req: NewFolderRequest, token: String) {
        "filePid: ${req.filePid}, folderName: ${req.folderName}".showToast(requireContext())
        FileInfoService.newFolder(
            req,
            addHeaders = { it.addHeader("Authorization", "Bearer $token") },
            onSuccess = { result ->
                if (result.code == 200) {
                    "文件夹 '${req.folderName}' 创建成功".showToast(requireContext())
                    // 刷新列表（可选）
                    loadFileList(LoadFileListRequest(1, 15, "", "all", "0"), token)
                } else {
                    result.message.showToast(requireContext())
                }
            },
            onFailure = { error ->
                "创建失败: $error".showToast(requireContext())
                Log.e(TAG, "Create folder failed: $error")
            }
        )
    }

    private fun loadFileList(req: LoadFileListRequest, token: String) {
        FileInfoService.loadFileList(
            req,
            addHeaders = {
                it.addHeader("Authorization", "Bearer $token")
            },
            onSuccess = { result ->
                if (result.code == 200) {
                    result.data?.list?.let { fileList ->
                        adapter.submitList(fileList)
                        Log.d(TAG, "Loaded ${fileList.size} files")
                    }
                } else {
                    result.message.showToast(requireContext())
                    loginManager.clearLoginInfo()
                    startActivity(Intent(requireContext(), LoginActivity::class.java))
                    requireActivity().finish()
                    Log.w(TAG, "Load file list failed with code: ${result.code}")
                }
            },
            onFailure = { error ->
                "Server error. Please try again.".showToast(requireContext())
                Log.e(TAG, "Load file list error: $error")
            }
        )
    }

    private fun setupBottomActionBarListeners() {
        binding.bottomActionBar.btnDelete.setOnClickListener { deleteSelectedFiles() }
        binding.bottomActionBar.btnShare.setOnClickListener { shareSelectedFiles() }
        binding.bottomActionBar.btnMove.setOnClickListener { moveSelectedFiles() }
        binding.bottomActionBar.btnDownload.setOnClickListener { downloadSelectedFiles() }
        binding.bottomActionBar.btnFavorite.setOnClickListener { favoriteSelectedFiles() }
        binding.bottomActionBar.btnRename.setOnClickListener { renameSelectedFiles() }
        binding.bottomActionBar.btnFileDetails.setOnClickListener { showFileDetails() }
    }

    private fun showTopSelectionBar(selectedCount: Int) {
        (requireActivity() as? MainActivity)?.showTopSelectionBar(selectedCount)
    }

    private fun hideTopSelectionBar() {
        (requireActivity() as? MainActivity)?.hideTopSelectionBar()
    }

    private fun showBottomActionBar() {
        if (isBottomActionBarVisible) return

        isBottomActionBarVisible = true
        binding.bottomActionBar.root.visibility = View.VISIBLE
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.bottomActionBar.root.startAnimation(slideUp)
    }

    private fun hideBottomActionBar() {
        if (!isBottomActionBarVisible) return

        val slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
        slideDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}
            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                binding.bottomActionBar.root.visibility = View.GONE
                isBottomActionBarVisible = false
            }
        })
        binding.bottomActionBar.root.startAnimation(slideDown)
    }

    private fun downloadSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            "Downloading ${selectedItems.size} files".showToast(requireContext())
            Log.d(TAG, "Downloading files: ${selectedItems.map { it.fileName }}")
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun shareSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            "Sharing ${selectedItems.size} files".showToast(requireContext())
            Log.d(TAG, "Sharing files: ${selectedItems.map { it.fileName }}")
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun deleteSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            showDeleteConfirmationDialog(selectedItems)
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun favoriteSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            "Adding ${selectedItems.size} files to favorites".showToast(requireContext())
            Log.d(TAG, "Favoriting files: ${selectedItems.map { it.fileName }}")
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun renameSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            if (selectedItems.size == 1) {
                "Renaming file: ${selectedItems[0].fileName}".showToast(requireContext())
            } else {
                "Renaming ${selectedItems.size} files".showToast(requireContext())
            }
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun moveSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            "Moving ${selectedItems.size} files".showToast(requireContext())
            Log.d(TAG, "Moving files: ${selectedItems.map { it.fileName }}")
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun showFileDetails() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            if (selectedItems.size == 1) {
                "Showing details for: ${selectedItems[0].fileName}".showToast(requireContext())
            } else {
                "Showing details for ${selectedItems.size} files".showToast(requireContext())
            }
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    private fun showDeleteConfirmationDialog(selectedItems: List<com.computer.skyvault.common.recycleitem.FileItem>) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete ${selectedItems.size} file(s)? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // 执行删除操作
                Log.d(TAG, "Deleting files: ${selectedItems.map { it.fileName }}")
                "Deleting ${selectedItems.size} files...".showToast(requireContext())
                // 这里可以调用删除API
                adapter.exitSelectionMode()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    fun exitSelectionMode() = adapter.exitSelectionMode()

    fun checkAll(isCheckAll: Boolean) = adapter.checkAll(isCheckAll)

    fun isInSelectionMode() = adapter.isInSelectionMode()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
