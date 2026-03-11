package com.computer.skyvault.ui.myfiles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.computer.skyvault.MainActivity
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemBreadcrumbPathAdapter
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.enums.FileFolderTypeEnum
import com.computer.skyvault.common.enums.FileTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.MyfilesFragmentBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.model.FolderInfo
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.service.FileShareService
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.DataUtils
import com.computer.skyvault.utils.showToast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

private const val TAG = "MyFilesFragment"

class MyFilesFragment : Fragment() {

    private var _binding: MyfilesFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecycleItemMyFilesFileFolderListAdapter
    private lateinit var loginManager: LoginManager
    private lateinit var fileShareService: FileShareService
    private lateinit var fileInfoService: FileInfoService

    // 文件夹导航相关
    private var currentFilePid: String = "0" // 当前文件夹 ID，默认为根目录
    private val folderHistory = mutableListOf<FolderInfo>() // 文件夹访问历史（用于返回）
    private lateinit var breadcrumbAdapter: RecycleItemBreadcrumbPathAdapter
    private var currentFolderName: String = "My Files"

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            fileInfoService.showUploadDialog(uris, onFileSelectRequested = {
                openFilePicker()
            })
        }
    }

    // 跟踪底部操作栏可见性状态
    private var isBottomActionBarVisible = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MyfilesFragmentBinding.inflate(inflater, container, false)
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
                    Log.d(TAG, "Opening: ${it.fileName}, folderType: ${it.folderType}")

                    // 检查是否是文件夹
                    if (it.folderType != null && it.folderType == FileFolderTypeEnum.FOLDER.value) {
                        openFolder(it)
                    } else {
                        // 打开文件
                        fileInfoService.openFile(it)
                    }
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
            setHasFixedSize(false)
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()
        }

        // 初始化服务
        fileShareService = FileShareService(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            loginManager = loginManager,
            onRefreshFileList = { refreshFileList() }
        )

        fileInfoService = FileInfoService(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            loginManager = loginManager,
            binding = binding,
            adapter = adapter,
            currentFilePid = { currentFilePid }
        )

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

        // check login info
        val currentLoginInfo = loginManager.getLoginInfo()
        if (currentLoginInfo != null) {
            Log.d(TAG, "User logged in with token: ${currentLoginInfo.access_token}")
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
                openFilePicker()
            }
        } else {
            // 无登录信息，跳转到登录页
            "请先登录".showToast(requireContext())
            startActivity(Intent(requireContext(), LoginActivity::class.java))
            requireActivity().finish()
        }

        setupBreadcrumbNavigation()
    }

    /**
     * 初始化面包屑导航
     */
    private fun setupBreadcrumbNavigation() {
        breadcrumbAdapter = RecycleItemBreadcrumbPathAdapter { position ->
            navigateToBreadcrumbPosition(position)
        }

        binding.fileDirectory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = breadcrumbAdapter
        }

        updateBreadcrumb()
    }

    /**
     * 打开文件夹
     */
    private fun openFolder(folderItem: FileItem) {
        Log.d(TAG, "进入文件夹：${folderItem.fileId}, 名称：${folderItem.fileName}")

        folderHistory.add(FolderInfo(currentFilePid, currentFolderName))

        currentFilePid = folderItem.fileId
        currentFolderName = folderItem.fileName

        loadFileListInFolder(currentFilePid)
        updateBreadcrumb()
    }

    /**
     * 更新面包屑导航显示
     */
    private fun updateBreadcrumb() {
        val paths = mutableListOf<String>()
        paths.add("My Files")

        folderHistory.forEach { folder ->
            if (folder.id != "0") {
                paths.add(folder.name)
            }
        }

        if (currentFilePid != "0") {
            paths.add(currentFolderName)
        }

        breadcrumbAdapter.submitList(paths)
    }

    /**
     * 导航到指定的面包屑位置
     */
    private fun navigateToBreadcrumbPosition(position: Int) {
        if (position == 0) {
            currentFilePid = "0"
            currentFolderName = "My Files"
            folderHistory.clear()
        } else {
            val targetFolder = folderHistory[position - 1]
            currentFilePid = targetFolder.id
            currentFolderName = targetFolder.name

            while (folderHistory.size >= position) {
                folderHistory.removeAt(folderHistory.lastIndex)
            }
        }

        loadFileListInFolder(currentFilePid)
        updateBreadcrumb()
    }

    /**
     * 返回上一级文件夹
     */
    private fun backToParentFolder() {
        if (folderHistory.isEmpty()) {
            "已经在根目录".showToast(requireContext())
            return
        }

        val parentFolder = folderHistory.removeAt(folderHistory.lastIndex)
        currentFilePid = parentFolder.id
        currentFolderName = parentFolder.name

        loadFileListInFolder(currentFilePid)
        updateBreadcrumb()
    }

    /**
     * 加载指定文件夹的内容
     */
    private fun loadFileListInFolder(filePid: String) {
        val token = loginManager.getLoginInfo()?.access_token ?: run {
            "请先登录".showToast(requireContext())
            return
        }

        Log.d(TAG, "加载文件夹：$filePid")

        loadFileList(
            LoadFileListRequest(
                pageNo = 1,
                pageSize = 100,
                fileNameFuzzy = "",
                category = "all",
                filePid = filePid
            ),
            token
        )
    }

    /**
     * 更新 ActionBar 标题
     */
    private fun updateActionBarTitle(title: String) {
        (requireActivity() as? MainActivity)?.supportActionBar?.title = title
    }


    private fun openFilePicker() {
        filePickerLauncher.launch("*/*")
    }

    private fun showCreateFolderDialog(token: String) {
        val dialog = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.module_dialog_create_folder, null)
        dialog.setView(view)

        val etFolderName = view.findViewById<TextInputEditText>(R.id.etFolderName)
        val tilFolderName = view.findViewById<TextInputLayout>(R.id.tilFolderName)

        dialog.setPositiveButton("完成") { _, _ ->
            val folderName = etFolderName.text.toString().trim()
            if (folderName.isEmpty()) {
                tilFolderName.error = "文件夹名称不能为空"
                return@setPositiveButton
            }

            fileInfoService.createFolder(
                NewFolderRequest(currentFilePid, folderName),
                token
            ) {
                refreshFileList()
            }
        }

        dialog.setNegativeButton("返回") { dg, _ ->
            dg.dismiss()
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

    private fun loadFileList(req: LoadFileListRequest, token: String) {
        if (!::adapter.isInitialized) {
            Log.e(TAG, "Adapter not initialized")
            return
        }

        fileInfoService.loadFileList(req, token) { fileList ->
            // 确保适配器已初始化并更新 UI
            if (::adapter.isInitialized) {
                adapter.submitList(fileList)
            }
        }
    }

    private fun refreshFileList() {
        val token = loginManager.getLoginInfo()?.access_token ?: return
        loadFileList(
            LoadFileListRequest(1, 15, "", "all", currentFilePid),
            token
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

        // 添加返回键监听
        binding.root.apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (currentFilePid != "0" || folderHistory.isNotEmpty()) {
                        backToParentFolder()
                        true
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
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
            // 过滤掉文件夹，只下载文件
            val filesToDownload = selectedItems.filter {
                it.folderType == null || it.folderType == 0
            }

            if (filesToDownload.isEmpty()) {
                "选择的都是文件夹，无法下载".showToast(requireContext())
                adapter.exitSelectionMode()
                return
            }

            // todo 需要同步到传输列表中

            // 逐个下载文件
            var downloadedCount = 0
            filesToDownload.forEachIndexed { index, fileItem ->
                fileInfoService.downloadFile(fileItem) { progress ->
                    // 更新进度（这里可以更新 UI 显示进度）
                    Log.d(TAG, "Downloading ${fileItem.fileName}: $progress%")
                }

                // 简单计数（实际应该用更精确的方式跟踪每个文件的下载状态）
                downloadedCount++
                if (downloadedCount == filesToDownload.size) {
                    "已开始下载 ${filesToDownload.size} 个文件".showToast(requireContext())
                    adapter.exitSelectionMode()
                }
            }
        } else {
            "请先选择文件".showToast(requireContext())
        }
    }

    private fun shareSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        if (selectedItems.isNotEmpty()) {
            if (selectedItems.size > 1) {
                "暂不支持批量分享，请选择单个文件".showToast(requireContext())
                return
            }
            fileShareService.showFileShareDialog(selectedItems.first())
        } else {
            "请先选择文件".showToast(requireContext())
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

    private fun showDeleteConfirmationDialog(selectedItems: List<FileItem>) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Delete")
            .setMessage("Are you sure you want to delete ${selectedItems.size} file(s)? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                // 执行删除操作
                Log.d(TAG, "Deleting files: ${selectedItems.map { it.fileName }}")
                "Deleting ${selectedItems.size} files...".showToast(requireContext())
                // 执行删除操作
                executeDeleteFiles(selectedItems)

            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * 执行删除文件
     */
    private fun executeDeleteFiles(selectedItems: List<FileItem>) {
        // 提取文件 ID 列表
        val fileIds = selectedItems.map { it.fileId }

        // 调用服务层删除
        fileInfoService.deleteFiles(fileIds) {
            // 退出选择模式
            adapter.exitSelectionMode()
            // 删除成功，刷新文件列表
            refreshFileList()
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
                showRenameDialog(selectedItems[0])
            } else {
                "Batch renaming is not supported at this time".showToast(requireContext())
            }
        } else {
            "Please select files first".showToast(requireContext())
        }
    }

    /**
     * 显示重命名对话框
     */
    private fun showRenameDialog(fileItem: FileItem) {
        val dialog = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.module_dialog_create_folder, null)
        dialog.setView(view)

        val operateDesc = view.findViewById<TextView>(R.id.operateDesc)
        val fileCover = view.findViewById<AppCompatImageView>(R.id.fileCover)
        val etFolderName = view.findViewById<TextInputEditText>(R.id.etFolderName)
        val tilFolderName = view.findViewById<TextInputLayout>(R.id.tilFolderName)

        // 设置操作说明
        operateDesc.text = "重命名"
        val typeEnum = FileTypeEnum.getByType(fileItem.fileType)
        DataUtils.setFileCoverByType(typeEnum, fileItem, fileCover)

        // 分离文件名和后缀
        val fileNameWithoutExtension = fileItem.fileName.substringBeforeLast('.', fileItem.fileName)
        val fileExtension = if (fileItem.fileName.contains('.')) {
            "." + fileItem.fileName.substringAfterLast('.')
        } else {
            ""
        }

        // 设置当前文件后缀
        tilFolderName.suffixText = fileExtension

        // 设置当前文件名
        etFolderName.setText(fileNameWithoutExtension)
        etFolderName.setSelection(fileNameWithoutExtension.length)



        dialog.setPositiveButton("确定") { _, _ ->
            val newFileName = etFolderName.text.toString().trim()
            if (newFileName.isEmpty()) {
                tilFolderName.error = "文件名不能为空"
                return@setPositiveButton
            }

            if (newFileName == fileItem.fileName) {
                "文件名未改变".showToast(requireContext())
                return@setPositiveButton
            }

            // 调用重命名接口
            fileInfoService.renameFile(fileItem.fileId, newFileName) {
                // 重命名成功，刷新文件列表
                adapter.exitSelectionMode()
                refreshFileList()
            }
        }

        dialog.setNegativeButton("取消") { dg, _ ->
            dg.dismiss()
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


    fun exitSelectionMode() = adapter.exitSelectionMode()

    fun checkAll(isCheckAll: Boolean) = adapter.checkAll(isCheckAll)

    fun isInSelectionMode() = adapter.isInSelectionMode()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null

        // 清空历史记录
        folderHistory.clear()
    }
}
