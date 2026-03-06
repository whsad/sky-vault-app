package com.computer.skyvault.ui.myfiles

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.computer.skyvault.MainActivity
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.NewFolderRequest
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.ModuleFragmentMyFilesBinding
import com.computer.skyvault.manager.LoginManager
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.service.FileShareService
import com.computer.skyvault.ui.login.LoginActivity
import com.computer.skyvault.utils.showToast

private const val TAG = "MyFilesFragment"

class MyFilesFragment : Fragment() {

    private var _binding: ModuleFragmentMyFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecycleItemMyFilesFileFolderListAdapter
    private lateinit var loginManager: LoginManager
    private lateinit var fileShareService: FileShareService
    private lateinit var fileInfoService: FileInfoService

    // 文件选择器
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            fileInfoService.showUploadDialog(uris)
        }
    }

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
            adapter = adapter
        )

        // 设置文件选择回调
        fileInfoService.onFileSelectRequested = {
            openFilePicker()
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

        // check login info
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

    private fun showCreateFolderDialog(token: String) {
        val dialog = AlertDialog.Builder(requireContext())
        val inflater = layoutInflater
        val view = inflater.inflate(R.layout.module_dialog_create_folder, null)
        dialog.setView(view)

        val etFolderName =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etFolderName)
        val tilFolderName =
            view.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilFolderName)

        dialog.setPositiveButton("完成") { _, _ ->
            val folderName = etFolderName.text.toString().trim()
            if (folderName.isEmpty()) {
                tilFolderName.error = "文件夹名称不能为空"
                return@setPositiveButton
            }

            fileInfoService.createFolder(
                NewFolderRequest("0", folderName),
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
//        fileInfoService.loadFileList(req, token)
    }

    private fun refreshFileList() {
        val token = loginManager.getLoginInfo()?.access_token ?: return
        loadFileList(
            LoadFileListRequest(1, 15, "", "all", "0"),
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

    private fun showDeleteConfirmationDialog(selectedItems: List<FileItem>) {
        AlertDialog.Builder(requireContext())
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
