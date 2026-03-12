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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.computer.skyvault.MainActivity
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemBreadcrumbPathAdapter
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.enums.FileFolderTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.MyfilesFragmentBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.model.FolderInfo
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.service.FileShareService
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.showToast
import kotlinx.coroutines.launch

/**
 * 文件管理 Fragment
 * 负责文件/文件夹的展示、导航、选择、基础操作等 UI 逻辑
 */
private const val TAG = "MyFilesFragment"

class MyFilesFragment : Fragment() {

    // ViewBinding 优化：使用 lazy 初始化，避免空值判断
    private val binding: MyfilesFragmentBinding by lazy {
        MyfilesFragmentBinding.inflate(layoutInflater)
    }

    // 服务与管理器（延迟初始化）
    private val loginManager: LoginManager by lazy {
        LoginManager.getInstance(requireActivity().application)
    }
    private lateinit var fileShareService: FileShareService
    private lateinit var fileInfoService: FileInfoService

    // 适配器
    private val fileListAdapter: RecycleItemMyFilesFileFolderListAdapter by lazy {
        createFileListAdapter()
    }
    private lateinit var breadcrumbAdapter: RecycleItemBreadcrumbPathAdapter

    // 文件夹导航相关
    private var currentFilePid: String = "0" // 当前文件夹 ID，默认为根目录
    private val folderHistory = mutableListOf<FolderInfo>() // 文件夹访问历史
    private var currentFolderName: String = "My Files"

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            fileInfoService.showUploadDialog(uris) { openFilePicker() }
        }
    }

    // 状态管理
    private var isBottomActionBarVisible = false
    private var isViewInitialized = false // 防止重复初始化
    private var searchJob: kotlinx.coroutines.Job? = null // 搜索协程任务

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = binding.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 防止重建时重复初始化
        if (isViewInitialized) return

        // 检查登录状态（提前校验，避免后续空指针）
        val loginInfo = loginManager.getLoginInfo()
        if (loginInfo == null) {
            handleNotLoggedIn()
            return
        }

        // 初始化核心组件
        initRecyclerView()
        initServices(loginInfo.access_token)
        initBreadcrumbNavigation()
        initBottomActionBar()
        initClickListeners(loginInfo.access_token)
        initBackKeyListener()

        // 加载初始文件列表
        loadFileList(LoadFileListRequest(
            pageNo = 1,
            pageSize = 20, // 优化分页大小
            fileNameFuzzy = "",
            category = "all",
            filePid = "0"
        ), loginInfo.access_token)

        isViewInitialized = true
    }

    /**
     * 处理未登录状态
     */
    private fun handleNotLoggedIn() {
        "请先登录".showToast(requireContext())
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    /**
     * 创建文件列表适配器（提取为独立方法，提高可读性）
     */
    private fun createFileListAdapter(): RecycleItemMyFilesFileFolderListAdapter {
        return RecycleItemMyFilesFileFolderListAdapter(
            onItemClick = ::handleFileItemClick,
            onItemLongClick = ::handleFileItemLongClick
        ).apply {
            // 监听选择状态变化
            onSelectionModeChanged = { isInSelectionMode, selectedCount ->
                updateSelectionUI(isInSelectionMode, selectedCount)
            }
        }
    }

    /**
     * 初始化 RecyclerView
     */
    private fun initRecyclerView() {
        binding.fileFolderList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = fileListAdapter
            setHasFixedSize(true) // 优化：固定大小提升性能
            itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator().apply {
                addDuration = 150
                removeDuration = 150
            }
        }
    }

    /**
     * 初始化服务类（优化依赖注入）
     */
    private fun initServices(token: String) {
        fileShareService = FileShareService(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            loginManager = loginManager,
            onRefreshFileList = ::refreshFileList
        )

        fileInfoService = FileInfoService(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            loginManager = loginManager,
            binding = binding,
            adapter = fileListAdapter,
            currentFilePidProvider = { currentFilePid } // 使用 Provider 模式解耦
        )
    }

    /**
     * 初始化面包屑导航
     */
    private fun initBreadcrumbNavigation() {
        breadcrumbAdapter = RecycleItemBreadcrumbPathAdapter { position ->
            navigateToBreadcrumbPosition(position)
        }

        binding.fileDirectory.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = breadcrumbAdapter
        }

        updateBreadcrumb()
        // 初始化搜索框
        initSearchBox()
    }

    /**
     * 初始化搜索框（添加防抖功能）
     */
    private fun initSearchBox() {
        binding.searchLayout.editText?.addTextChangedListener(
            object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: android.text.Editable?) {
                    // 取消之前的搜索任务
                    searchJob?.cancel()

                    // 延迟 500ms 执行搜索（防抖）
                    searchJob = viewLifecycleOwner.lifecycleScope.launch {
                        kotlinx.coroutines.delay(500)
                        val keyword = s?.toString()?.trim() ?: ""
                        searchFiles(keyword)
                    }
                }
            }
        )
    }

    /**
     * 搜索文件
     */
    private fun searchFiles(keyword: String) {
        val token = loginManager.getLoginInfo()?.access_token ?: return

        if (keyword.isEmpty()) {
            // 关键词为空，加载当前文件夹列表
            loadFileListInFolder(currentFilePid)
            Log.d(TAG, "搜索关键词为空，加载当前文件夹列表")
            return
        }

        Log.d(TAG, "开始搜索文件：$keyword")

        // 调用 API 搜索文件
        fileInfoService.loadFileList(
            LoadFileListRequest(
                pageNo = 1,
                pageSize = 50, // 搜索时加载更多结果
                fileNameFuzzy = keyword,
                category = "all",
                filePid = "0" // 搜索时在整个云盘搜索
            ),
            token
        ) { fileList ->
            fileListAdapter.submitList(fileList)
            Log.d(TAG, "搜索完成，找到 ${fileList.size} 个文件")
        }
    }

    /**
     * 初始化底部操作栏
     */
    private fun initBottomActionBar() {
        with(binding.bottomActionBar) {
            btnDelete.setOnClickListener { deleteSelectedFiles() }
            btnShare.setOnClickListener { shareSelectedFiles() }
            btnMove.setOnClickListener { moveSelectedFiles() }
            btnDownload.setOnClickListener { downloadSelectedFiles() }
            btnFavorite.setOnClickListener { favoriteSelectedFiles() }
            btnRename.setOnClickListener { renameSelectedFiles() }
            btnFileDetails.setOnClickListener { showFileDetails() }
        }
    }

    /**
     * 初始化点击事件监听器
     */
    private fun initClickListeners(token: String) {
        // 新建文件夹
        binding.btnNewFolder.setOnClickListener {
            showCreateFolderDialog(token)
        }

        // 上传文件
        binding.btnUpload.setOnClickListener {
            openFilePicker()
        }
    }

    /**
     * 初始化返回键监听（优化事件处理）
     */
    private fun initBackKeyListener() {
        binding.root.apply {
            isFocusableInTouchMode = true
            requestFocus()
            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK) {
                    if (fileListAdapter.isInSelectionMode()) {
                        // 选择模式下按返回键退出选择
                        fileListAdapter.exitSelectionMode()
                        true
                    } else if (currentFilePid != "0" || folderHistory.isNotEmpty()) {
                        // 非根目录返回上一级
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

    /**
     * 处理文件项点击事件
     */
    private fun handleFileItemClick(fileItem: FileItem) {
        Log.d(TAG, "Clicked on: ${fileItem.fileName}")

        if (fileListAdapter.isInSelectionMode()) {
            fileListAdapter.toggleSelection(fileItem.fileId)
        } else {
            // 普通模式下的操作
            if (fileItem.isFolder()) { // 提取为扩展函数，提高可读性
                openFolder(fileItem)
            } else {
                fileInfoService.openFile(fileItem)
            }
        }
    }

    /**
     * 处理文件项长按事件
     */
    private fun handleFileItemLongClick(fileItem: FileItem) {
        Log.d(TAG, "Long clicked on: ${fileItem.fileName}")
        if (!fileListAdapter.isInSelectionMode()) {
            fileListAdapter.enterSelectionMode()
            fileListAdapter.toggleSelection(fileItem.fileId)
        }
    }

    /**
     * 更新选择状态 UI
     */
    private fun updateSelectionUI(isInSelectionMode: Boolean, selectedCount: Int) {
        if (isInSelectionMode) {
            showTopSelectionBar(selectedCount)
            showBottomActionBar()
        } else {
            hideTopSelectionBar()
            hideBottomActionBar()
        }
    }

    /**
     * 打开文件夹（修复：严格防止历史记录重复）
     */
    private fun openFolder(folderItem: FileItem) {
        Log.d(TAG, "进入文件夹：${folderItem.fileId}, 名称：${folderItem.fileName}")

        // 严格检查：如果目标文件夹已在历史记录中，先清理掉它及之后的所有记录
        val existingIndex = folderHistory.indexOfFirst { it.id == folderItem.fileId }

        if (existingIndex >= 0) {
            // 发现重复，清理掉从该位置开始的所有记录
            for (i in folderHistory.lastIndex downTo existingIndex) {
                folderHistory.removeAt(i)
            }
            Log.d(TAG, "✅ 清理重复历史记录，从索引 $existingIndex 开始，清理后：${folderHistory.size}条")
        }

        // 只在非根目录时才保存历史记录
        if (currentFilePid != "0") {
            // 额外检查：避免添加重复的历史记录
            val lastHistory = folderHistory.lastOrNull()
            if (lastHistory?.id != currentFilePid) {
                folderHistory.add(FolderInfo(currentFilePid, currentFolderName))
                Log.d(TAG, "添加历史记录：${currentFolderName}(${currentFilePid})")
            } else {
                Log.w(TAG, "跳过重复历史记录添加：${currentFolderName}(${currentFilePid})")
            }
        }

        // 更新当前路径
        currentFilePid = folderItem.fileId
        currentFolderName = folderItem.fileName

        // 加载新文件夹内容
        loadFileListInFolder(currentFilePid)

        // 更新导航和标题
        updateBreadcrumb()
        updateActionBarTitle(currentFolderName)

        Log.d(TAG, "打开文件夹后 - PID: $currentFilePid, Name: $currentFolderName, History: ${folderHistory.map { "${it.name}(${it.id})" }}")
    }

    /**
     * 返回上一级文件夹（修复返回逻辑）
     */
    private fun backToParentFolder() {
        if (folderHistory.isEmpty()) {
            "已经在根目录".showToast(requireContext())
            return
        }

        // 恢复上一级路径（移除最后一条历史记录并赋值）
        val parentFolder = folderHistory.removeAt(folderHistory.lastIndex)
        currentFilePid = parentFolder.id
        currentFolderName = parentFolder.name

        // 刷新列表和导航
        loadFileListInFolder(currentFilePid)
        updateBreadcrumb()
        updateActionBarTitle(currentFolderName)
    }

    /**
     * 更新面包屑导航（修复：不过滤根目录历史记录）
     */
    private fun updateBreadcrumb() {
        val paths = mutableListOf<String>()

        // 添加根目录
        paths.add("My Files")

        // 添加所有历史记录（包括根目录）
        folderHistory.forEach { paths.add(it.name) }

        // 如果当前不在根目录，且当前目录不在历史记录末尾，则添加
        if (currentFilePid != "0") {
            val lastHistoryId = folderHistory.lastOrNull()?.id
            if (currentFilePid != lastHistoryId) {
                paths.add(currentFolderName)
            }
        }

        Log.d(TAG, "面包屑路径：$paths, 历史记录：${folderHistory.map { "${it.name}(${it.id})" }}")
        breadcrumbAdapter.submitList(paths)
    }

    /**
     * 导航到面包屑指定位置（修复：历史记录清理逻辑）
     */
    private fun navigateToBreadcrumbPosition(position: Int) {
        val currentPaths = breadcrumbAdapter.currentList
        if (position < 0 || position >= currentPaths.size) {
            Log.w(TAG, "无效的面包屑位置：$position")
            return
        }

        // 点击当前层级（最后一项），不操作
        if (position == currentPaths.size - 1) {
            Log.d(TAG, "点击当前目录层级，不执行操作")
            return
        }

        Log.d(TAG, "导航到面包屑位置：$position, 路径：${currentPaths[position]}")

        // 点击根目录
        if (position == 0) {
            currentFilePid = "0"
            currentFolderName = "My Files"
            folderHistory.clear()
            Log.d(TAG, "跳转到根目录，历史记录已清空")
        }
        // 点击中间层级
        else {
            // 关键修复：面包屑位置与历史记录的对应关系
            // 面包屑：[My Files(0), A(1), B(2), C(3)]
            // 历史记录：[(0, My Files), (A_ID, A), (B_ID, B)]
            // 点击位置 1(A) → 应该保留历史记录的前 1 项（索引 0）
            // 点击位置 2(B) → 应该保留历史记录的前 2 项（索引 0,1）

            // 需要保留的历史记录数量 = position
            val keepCount = position

            Log.d(TAG, "需要保留的历史记录数：$keepCount，当前历史记录数：${folderHistory.size}")

            // 清理掉多余的历史记录
            while (folderHistory.size > keepCount) {
                folderHistory.removeAt(folderHistory.lastIndex)
            }

            // 设置目标文件夹为当前文件夹
            if (keepCount > 0 && folderHistory.isNotEmpty()) {
                val targetFolder = folderHistory[keepCount - 1]
                currentFilePid = targetFolder.id
                currentFolderName = targetFolder.name
                Log.d(TAG, "跳转到：${targetFolder.name}, ID: ${targetFolder.id}")
            } else {
                Log.e(TAG, "数据异常：keepCount=$keepCount, folderHistory.size=${folderHistory.size}")
                currentFilePid = "0"
                currentFolderName = "My Files"
            }
        }

        Log.d(TAG, "导航完成 - PID: $currentFilePid, Name: $currentFolderName, History: ${folderHistory.map { "${it.name}(${it.id})" }}")

        loadFileListInFolder(currentFilePid)
        updateBreadcrumb()
        updateActionBarTitle(currentFolderName)
    }

    /**
     * 加载指定文件夹内容
     */
    private fun loadFileListInFolder(filePid: String) {
        val token = loginManager.getLoginInfo()?.access_token ?: return

        loadFileList(
            LoadFileListRequest(
                pageNo = 1,
                pageSize = 20,
                fileNameFuzzy = "",
                category = "all",
                filePid = filePid
            ),
            token
        )
    }

    /**
     * 加载文件列表（核心方法）
     */
    private fun loadFileList(req: LoadFileListRequest, token: String) {
        fileInfoService.loadFileList(req, token) { fileList ->
            fileListAdapter.submitList(fileList)
        }
    }

    /**
     * 刷新当前文件夹列表
     */
    fun refreshFileList() {
        val token = loginManager.getLoginInfo()?.access_token ?: return
        loadFileListInFolder(currentFilePid)
    }

    /**
     * 打开文件选择器
     */
    private fun openFilePicker() {
        try {
            filePickerLauncher.launch("*/*")
        } catch (e: Exception) {
            Log.e(TAG, "打开文件选择器失败", e)
            "无法打开文件选择器".showToast(requireContext())
        }
    }

    /**
     * 显示创建文件夹对话框
     */
    private fun showCreateFolderDialog(token: String) {
        val dialogView = layoutInflater.inflate(R.layout.module_dialog_create_folder, null)
        val etFolderName = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFolderName)
        val tilFolderName = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilFolderName)

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("完成") { _, _ ->
                val folderName = etFolderName.text.toString().trim()
                when {
                    folderName.isEmpty() -> tilFolderName.error = "文件夹名称不能为空"
                    folderName.length > 50 -> tilFolderName.error = "文件夹名称不能超过50个字符"
                    else -> {
                        fileInfoService.createFolder(NewFolderRequest(currentFilePid, folderName), token) {
                            refreshFileList()
                        }
                    }
                }
            }
            .setNegativeButton("返回", null)
            .create().apply {
                // 设置圆角
                window?.setBackgroundDrawable(
                    android.graphics.drawable.GradientDrawable().apply {
                        cornerRadius = 16f * requireContext().resources.displayMetrics.density
                        setColor(android.graphics.Color.WHITE)
                    }
                )
                show()
            }
    }

    /**
     * 更新ActionBar标题
     */
    private fun updateActionBarTitle(title: String) {
        (activity as? MainActivity)?.supportActionBar?.title = title
    }

    /**
     * 显示顶部选择栏
     */
    private fun showTopSelectionBar(selectedCount: Int) {
        (activity as? MainActivity)?.showTopSelectionBar(selectedCount)
    }

    /**
     * 隐藏顶部选择栏
     */
    private fun hideTopSelectionBar() {
        (activity as? MainActivity)?.hideTopSelectionBar()
    }

    /**
     * 显示底部操作栏（带动画）
     */
    private fun showBottomActionBar() {
        if (isBottomActionBarVisible) return

        isBottomActionBarVisible = true
        binding.bottomActionBar.root.visibility = View.VISIBLE
        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
        binding.bottomActionBar.root.startAnimation(slideUp)
    }

    /**
     * 隐藏底部操作栏（带动画）
     */
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

    // ========== 底部操作栏功能实现 ==========

    /**
     * 下载选中文件
     */
    private fun downloadSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        when {
            selectedItems.isEmpty() -> "请先选择文件".showToast(requireContext())
            selectedItems.all { it.isFolder() } -> "选择的都是文件夹，无法下载".showToast(requireContext())
            else -> {
                val filesToDownload = selectedItems.filter { !it.isFolder() }
                fileListAdapter.exitSelectionMode()

                filesToDownload.forEachIndexed { _, fileItem ->
                    fileInfoService.downloadFile(fileItem) { progress ->
                        Log.d(TAG, "Downloading ${fileItem.fileName}: $progress%")
                    }
                }
            }
        }
    }

    /**
     * 分享选中文件
     */
    private fun shareSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        when {
            selectedItems.isEmpty() -> "请先选择文件".showToast(requireContext())
            selectedItems.size > 1 -> "暂不支持批量分享，请选择单个文件".showToast(requireContext())
            else -> fileShareService.showFileShareDialog(selectedItems.first())
        }
    }

    /**
     * 删除选中文件
     */
    private fun deleteSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            "请先选择文件".showToast(requireContext())
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("确认删除")
            .setMessage("确定要删除 ${selectedItems.size} 个文件/文件夹吗？此操作不可撤销。")
            .setPositiveButton("删除") { _, _ ->
                fileInfoService.deleteFiles(selectedItems.map { it.fileId }) {
                    fileListAdapter.exitSelectionMode()
                    refreshFileList()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 收藏选中文件
     */
    private fun favoriteSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            "请先选择文件".showToast(requireContext())
        } else {
            "将 ${selectedItems.size} 个文件添加到收藏".showToast(requireContext())
            Log.d(TAG, "Favoriting files: ${selectedItems.map { it.fileName }}")
        }
    }

    /**
     * 重命名选中文件
     */
    private fun renameSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        when {
            selectedItems.isEmpty() -> "请先选择文件".showToast(requireContext())
            selectedItems.size > 1 -> "暂不支持批量重命名".showToast(requireContext())
            else -> fileInfoService.showRenameDialog(selectedItems.first()) {
                fileListAdapter.exitSelectionMode()
                refreshFileList()
            }
        }
    }

    /**
     * 移动选中文件
     */
    private fun moveSelectedFiles() {
        val selectedItems = fileListAdapter.getSelectedItems()
        if (selectedItems.isEmpty()) {
            "请先选择文件".showToast(requireContext())
        } else {
            "移动 ${selectedItems.size} 个文件".showToast(requireContext())
            Log.d(TAG, "Moving files: ${selectedItems.map { it.fileName }}")
        }
    }

    /**
     * 显示文件详情
     */
    private fun showFileDetails() {
        val selectedItems = fileListAdapter.getSelectedItems()
        when {
            selectedItems.isEmpty() -> "请先选择文件".showToast(requireContext())
            selectedItems.size == 1 -> "显示 ${selectedItems[0].fileName} 的详情".showToast(requireContext())
            else -> "显示 ${selectedItems.size} 个文件的详情".showToast(requireContext())
        }
    }

    // ========== 公共方法 ==========

    fun exitSelectionMode() = fileListAdapter.exitSelectionMode()
    fun checkAll(isCheckAll: Boolean) = fileListAdapter.checkAll(isCheckAll)
    fun isInSelectionMode() = fileListAdapter.isInSelectionMode()

    // ========== 生命周期管理 ==========

    override fun onDestroyView() {
        super.onDestroyView()
        // 清空历史记录
        folderHistory.clear()
        // 重置状态
        isViewInitialized = false
        // 退出选择模式
        fileListAdapter.exitSelectionMode()
        // 取消搜索任务
        searchJob?.cancel()
    }

    // ========== 扩展函数（提高代码可读性） ==========
    private fun FileItem.isFolder(): Boolean {
        return folderType != null && folderType == FileFolderTypeEnum.FOLDER.value
    }
}