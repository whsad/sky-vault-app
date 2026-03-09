package com.computer.skyvault.ui.transfer

import android.app.Dialog
import android.os.Bundle
import android.view.*
import android.view.animation.AnimationUtils
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.computer.skyvault.R
import com.computer.skyvault.databinding.TransferDialogFragmentBinding
import com.google.android.material.tabs.TabLayoutMediator

class TransferDialogFragment : DialogFragment() {
    private var _binding: TransferDialogFragmentBinding? = null
    private val binding get() = _binding!!

    companion object {
        fun newInstance() = TransferDialogFragment()
        const val TAG = "TransferDialogFragment"
    }

    // 关键修改1：设置全屏样式 + 侧滑动画
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.apply {
            // 关键修改2：设置全屏 + 右侧滑入
            setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.RIGHT) // 从右侧滑入
            // 取消背景灰度（全屏不需要）
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        }
        dialog.setCanceledOnTouchOutside(true) // 点击左侧空白可关闭
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TransferDialogFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupViewPager()
        setupBottomBar()
    }

    // 顶部栏初始化（关闭按钮改为返回箭头）
    private fun setupToolbar() {
        // 关闭按钮（返回箭头）
        binding.ivClose.setOnClickListener {
            dismiss() // 关闭时触发滑出动画
        }

        // 设置按钮
        binding.ivSettings.setOnClickListener {
            // 打开传输设置
        }

        // 选择按钮
        binding.ivSelect.setOnClickListener {
            // 进入多选模式
        }

        // 删除按钮
        binding.ivDelete.setOnClickListener {
            // 批量删除任务
        }
    }

    // ViewPager2 + TabLayout 初始化（逻辑不变）
    private fun setupViewPager() {
        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount() = 4
            override fun createFragment(position: Int) = when (position) {
                0 -> DownloadFragment()
                1 -> UploadFragment()
                2 -> TransferSaveFragment()
                3 -> CloudAdditionFragment()
                else -> DownloadFragment()
            }
        }

        // Tab绑定
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "下载"
                1 -> "上传"
                2 -> "转存"
                3 -> "云添加"
                else -> ""
            }
        }.attach()

        // Tab切换监听（控制右上角按钮显示）
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> { // 下载页
                        binding.ivSelect.visibility = View.GONE
                        binding.ivDelete.visibility = View.GONE
                        binding.ivSettings.visibility = View.VISIBLE
                    }
                    1, 2 -> { // 上传/转存页
                        binding.ivSelect.visibility = View.VISIBLE
                        binding.ivDelete.visibility = View.GONE
                        binding.ivSettings.visibility = View.GONE
                    }
                    3 -> { // 云添加页
                        binding.ivSelect.visibility = View.GONE
                        binding.ivDelete.visibility = View.VISIBLE
                        binding.ivSettings.visibility = View.GONE
                    }
                }
            }
        })
    }

    // 底部空间提示栏（逻辑不变）
    private fun setupBottomBar() {
        binding.viewPager.registerOnPageChangeCallback(object : androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                when (position) {
                    0 -> { // 下载页（手机空间）
                        binding.tvSpaceInfo.text = "手机剩余空间：129.9GB/461.3GB"
                        binding.tvSpaceAction.text = "点击管理"
                    }
                    else -> { // 其他页（网盘空间）
                        binding.tvSpaceInfo.text = "网盘剩余空间：4.1GB/305.0GB"
                        binding.tvSpaceAction.text = "查看详情"
                    }
                }
            }
        })

        binding.tvSpaceAction.setOnClickListener {
            // 跳转到空间管理页
        }
    }

    // 关键修改3：关闭时触发滑出动画
    override fun dismiss() {
        binding.root.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slide_out_to_right))
        binding.root.postDelayed({
            super.dismiss()
        }, 300) // 动画时长匹配
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}