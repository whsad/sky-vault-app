package com.computer.skyvault.ui.myfiles

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.computer.skyvault.MainActivity
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemMyFilesFileFolderListAdapter
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.common.dto.LoginRequest
import com.computer.skyvault.databinding.ModuleFragmentMyFilesBinding
import com.computer.skyvault.service.AccountService
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.ui.login.UserViewModel
import com.computer.skyvault.utils.showToast

private const val TAG = "MyFilesFragment"

class MyFilesFragment : Fragment() {

    private var _binding: ModuleFragmentMyFilesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecycleItemMyFilesFileFolderListAdapter
    private lateinit var userViewModel: UserViewModel

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
        // 初始化 ViewModel
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

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

        // todo 等登录页面写好后转移，登录用户，同步login viewmodel数据
        performLogin()

        // 监听登录信息变化, 并获取文件列
        userViewModel.loginInfo.observe(viewLifecycleOwner) { loginInfo ->
            loginInfo?.let {
                // 获取文件列表
                val request = LoadFileListRequest(
                    pageNo = 1,
                    pageSize = 15,
                    fileNameFuzzy = "",
                    category = "all",
                    filePid = "0"
                )

                loadFileList(request, loginInfo.access_token)

                binding.btnNewFolder.setOnClickListener {
                    showCreateFolderDialog()
                }
            } ?: run {
                // todo 如果没有登录信息
//                Log.w(TAG, "No login info available, attempting to login...")
//                performLogin()
            }
        }
    }

    private fun showCreateFolderDialog() {
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
//            createFolder(folderName)
        }

        dialog.setNegativeButton("返回") { dialog, _ ->
            dialog.dismiss()
        }

        val alertDialog = dialog.create()
        // 设置对话框宽度为 80% 屏幕宽（更符合截图样式）
        alertDialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.8).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        alertDialog.show()
    }

    private fun createFolder(req: LoadFileListRequest, token: String) {

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

    private fun performLogin() {
        val loginRequest = LoginRequest(
            email = "example@example.com",
            password = "123123123",
            verificationCode = "test"
        )
        AccountService.login(
            loginRequest,
            onSuccess = { result ->
                if (result.code == 200) {
                    result.data?.let { loginInfo ->
                        userViewModel.setLoginInfo(loginInfo)
                        Log.d(TAG, "Login successful")
                    }
                } else {
                    result.message.showToast(requireContext())
                }
            },
            onFailure = {
                "Network error. Please try again.".showToast(requireContext())
                Log.e(TAG, "Login exception: $it")
            }
        )
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