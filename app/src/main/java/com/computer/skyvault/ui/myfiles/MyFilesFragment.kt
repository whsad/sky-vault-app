package com.computer.skyvault.ui.myfiles

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
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

    // todo 后期需要转移到loginview中
    private lateinit var userViewModel: UserViewModel


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = ModuleFragmentMyFilesBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // 初始化 ViewModel
        userViewModel = ViewModelProvider(this)[UserViewModel::class.java]

        // 初始化适配器
        adapter = RecycleItemMyFilesFileFolderListAdapter(
            onItemClick = { fileItem ->
                // 处理点击事件
                println("Clicked on: ${fileItem.fileName}")
            },
            onItemLongClick = { fileItem ->
                // 处理长按事件
                println("Long clicked on: ${fileItem.fileName}")
            }
        )

        // 设置 RecyclerView
        binding.fileFolderList.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@MyFilesFragment.adapter
        }

        // 监听选择状态变化
        adapter.onSelectionModeChanged = { isInSelectionMode, selectedCount ->
            if (isInSelectionMode) {
                showBottomActionBar()
//                updateTopSelectionBar(selectedCount)
            } else {
                hideBottomActionBar()
//                hideTopSelectionBar()
            }
        }
        // 添加测试数据
        addTestData()

        return root
    }

//    private fun showBottomActionBar() {
//        binding.bottomActionBar.root.visibility = View.VISIBLE
//        val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
//        binding.bottomActionBar.root.startAnimation(slideUp)
//
//        // 调整 RecyclerView 的底部边距
//        val params = binding.fileFolderList.layoutParams as ConstraintLayout.LayoutParams
//        params.bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_action_bar_height)
//        binding.fileFolderList.layoutParams = params
//
//        // 设置按钮点击事件
//        binding.bottomActionBar.btnDelete.setOnClickListener {
//            deleteSelectedFiles()
//        }
//        binding.bottomActionBar.btnShare.setOnClickListener {
//            shareSelectedFiles()
//        }
//        binding.bottomActionBar.btnMove.setOnClickListener {
//            moveSelectedFiles()
//        }
//    }

    //    private fun hideBottomActionBar() {
//        val slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
//        slideDown.setAnimationListener(object : Animation.AnimationListener {
//            override fun onAnimationStart(animation: Animation?) {}
//            override fun onAnimationRepeat(animation: Animation?) {}
//
//            override fun onAnimationEnd(animation: Animation?) {
//                binding.bottomActionBar.root.visibility = View.GONE
//
//                // 恢复 RecyclerView 的底部边距
//                val params = binding.fileFolderList.layoutParams as ConstraintLayout.LayoutParams
//                params.bottomMargin = 0
//                binding.fileFolderList.layoutParams = params
//            }
//        })
//        binding.bottomActionBar.root.startAnimation(slideDown)
//    }

    private fun showBottomActionBar() {
        // ✅ 仅当当前不可见时才执行动画和显示
        if (binding.bottomActionBar.root.visibility == View.GONE) {
            binding.bottomActionBar.root.visibility = View.VISIBLE
            val slideUp = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up)
            binding.bottomActionBar.root.startAnimation(slideUp)

            // 调整 RecyclerView 的底部边距
//            val params = binding.fileFolderList.layoutParams as ConstraintLayout.LayoutParams
//            params.bottomMargin = resources.getDimensionPixelSize(R.dimen.bottom_action_bar_height)
//            binding.fileFolderList.layoutParams = params
        }

        // 设置按钮点击事件（每次都要设，避免复用旧监听器）
        binding.bottomActionBar.btnDelete.setOnClickListener { deleteSelectedFiles() }
        binding.bottomActionBar.btnShare.setOnClickListener { shareSelectedFiles() }
        binding.bottomActionBar.btnMove.setOnClickListener { moveSelectedFiles() }
    }

    private fun hideBottomActionBar() {
        // ✅ 仅当当前可见时才执行动画和隐藏
        if (binding.bottomActionBar.root.visibility == View.VISIBLE) {
            val slideDown = AnimationUtils.loadAnimation(requireContext(), R.anim.slide_down)
            slideDown.setAnimationListener(object : Animation.AnimationListener {
                override fun onAnimationStart(animation: Animation?) {}
                override fun onAnimationRepeat(animation: Animation?) {}

                override fun onAnimationEnd(animation: Animation?) {
                    binding.bottomActionBar.root.visibility = View.GONE

                    // 恢复 RecyclerView 的底部边距
                    val params = binding.fileFolderList.layoutParams as ConstraintLayout.LayoutParams
                    params.bottomMargin = 0
                    binding.fileFolderList.layoutParams = params
                }
            })
            binding.bottomActionBar.root.startAnimation(slideDown)
        }
    }

    private fun deleteSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        // 执行删除逻辑
        println("Deleting files: ${selectedItems.map { it.fileName }}")
    }

    private fun shareSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        // 执行分享逻辑
        println("Sharing files: ${selectedItems.map { it.fileName }}")
    }

    private fun moveSelectedFiles() {
        val selectedItems = adapter.getSelectedItems()
        // 执行移动逻辑
        println("Moving files: ${selectedItems.map { it.fileName }}")
    }

    private fun addTestData() {
        val email = "example@example.com"
        val password = "123123123"
        val verificationCode = "test"

        AccountService.login(
            LoginRequest(email, password, verificationCode),
            onSuccess = { result ->
                // 处理登录成功逻辑
                Log.d(TAG, "addTestData: Login success -> $result")
                if (result.code == 200) {
                    result.data?.let {
                        userViewModel.setLoginInfo(it)
                    }
                }
            },
            onFailure = { error ->
                // 处理登录失败逻辑
                "Login failed. Incorrect username or password.".showToast(requireContext())
                Log.w(TAG, "addTestData: Login Fail -> $error")
            }
        )

        userViewModel.loginInfo.observe(viewLifecycleOwner) { loginInfo ->
            loginInfo?.let {
                val pageNo = 1
                val pageSize = 15
                val fileNameFuzzy = ""
                val category = "all"
                val filePid = "0"

                FileInfoService.loadFileList(
                    LoadFileListRequest(
                        pageNo,
                        pageSize,
                        fileNameFuzzy,
                        category,
                        filePid
                    ),
                    addHeaders = {
                        it.addHeader("Authorization", "Bearer ${userViewModel.loginInfo.value?.access_token}")
                    },
                    onSuccess = { result ->
                        // 处理登录成功逻辑
                        Log.d(TAG, "addTestData: success -> $result")
                        if (result.code == 200) {
                            result.data?.list?.let {
                                adapter.submitList(it)
                            }
                        }
                    },
                    onFailure = { error ->
                        // 处理登录失败逻辑
                        "Server Error".showToast(requireContext())
                        Log.w(TAG, "addTestData: Server Error -> $error")

                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}