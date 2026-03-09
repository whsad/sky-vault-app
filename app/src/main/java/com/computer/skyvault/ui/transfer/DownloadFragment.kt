package com.computer.skyvault.ui.transfer

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.computer.skyvault.R
import com.computer.skyvault.adapter.DownloadTaskAdapter
import com.computer.skyvault.databinding.TransferFragmentDownloadBinding
import com.computer.skyvault.model.DownloadTask
import com.computer.skyvault.model.TransferStatus

class DownloadFragment : Fragment() {
    private var _binding: TransferFragmentDownloadBinding? = null
    private val binding get() = _binding!!
    private val downloadAdapter = DownloadTaskAdapter()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = TransferFragmentDownloadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        loadMockData()
    }

    private fun setupViews() {
        // 列表初始化
        binding.rvDownloads.adapter = downloadAdapter
        binding.rvDownloads.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL).apply {
                setDrawable(resources.getDrawable(R.drawable.divider_transfer_item))
            }
        )

        // 筛选标签 - 使用点击监听代替选中状态监听
        binding.chipAll.setOnClickListener { onChipClicked(R.id.chip_all) }
        binding.chipCompleted.setOnClickListener { onChipClicked(R.id.chip_completed) }
        binding.chipRunning.setOnClickListener { onChipClicked(R.id.chip_running) }
        binding.chipFailed.setOnClickListener { onChipClicked(R.id.chip_failed) }

        // 默认选中第一个
        updateChipSelection(R.id.chip_all)

//        // 筛选标签
//        binding.chipGroup.setOnCheckedStateChangeListener { _, checkedIds ->
//            val checkedId = checkedIds.firstOrNull() ?: R.id.chip_all
//            // 筛选逻辑（根据状态过滤）
//            when (checkedId) {
//                R.id.chip_all -> downloadAdapter.filter(emptyList())
//                R.id.chip_completed -> downloadAdapter.filter(listOf(TransferStatus.COMPLETED))
//                R.id.chip_running -> downloadAdapter.filter(listOf(TransferStatus.RUNNING))
//                R.id.chip_failed -> downloadAdapter.filter(listOf(TransferStatus.FAILED))
//            }
//        }

        // 同时下载数
        binding.layoutConcurrentCount.setOnClickListener { view ->
            // 选择同时下载数
            showConcurrentCountSelector(view)
        }

        // 全部清除
        binding.tvClearAll.setOnClickListener {
            // 清除已完成任务
        }
    }

    private var currentSelectedChipId: Int = R.id.chip_all

    private fun onChipClicked(chipId: Int) {
        if (currentSelectedChipId != chipId) {
            updateChipSelection(chipId)
            // 筛选逻辑
            when (chipId) {
                R.id.chip_all -> downloadAdapter.filter(emptyList())
                R.id.chip_completed -> downloadAdapter.filter(listOf(TransferStatus.COMPLETED))
                R.id.chip_running -> downloadAdapter.filter(listOf(TransferStatus.RUNNING))
                R.id.chip_failed -> downloadAdapter.filter(listOf(TransferStatus.FAILED))
            }
        }
    }

    private fun updateChipSelection(selectedId: Int) {
        currentSelectedChipId = selectedId
        binding.chipAll.isChecked = selectedId == R.id.chip_all
        binding.chipCompleted.isChecked = selectedId == R.id.chip_completed
        binding.chipRunning.isChecked = selectedId == R.id.chip_running
        binding.chipFailed.isChecked = selectedId == R.id.chip_failed
    }

    // 模拟数据
    private fun loadMockData() {
        val mockData = listOf(
            DownloadTask(
                id = "1",
                name = "FlClash-0.8.91-android-x86_64.apk",
                size = "45.0MB",
                progress = 100,
                speed = "",
                status = TransferStatus.COMPLETED
            ),
            DownloadTask(
                id = "2",
                name = "微信安装包.apk",
                size = "256MB",
                progress = 50,
                speed = "1.2MB/s",
                status = TransferStatus.RUNNING
            ),
            DownloadTask(
                id = "3",
                name = "视频文件.mp4",
                size = "1.2GB",
                progress = 0,
                speed = "",
                status = TransferStatus.FAILED
            )
        )
        downloadAdapter.submitList(mockData)

        // 空态判断
        if (mockData.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.rvDownloads.visibility = View.GONE
        } else {
            binding.emptyView.visibility = View.GONE
            binding.rvDownloads.visibility = View.VISIBLE
        }
    }

    /**
     * 显示同时下载数选择器
     */
    private fun showConcurrentCountSelector(view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.menu_concurrent_download_count, popup.menu)

        // 设置当前选中项
        val currentCount = getCurrentConcurrentCount()
        for (i in 0 until popup.menu.size()) {
            val menuItem = popup.menu.getItem(i)
            menuItem.isChecked = (i + 1) == currentCount
        }

        // 设置单选模式
        popup.setForceShowIcon(true)

        // 监听选择
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menu_1 -> updateConcurrentCount(1)
                R.id.menu_2 -> updateConcurrentCount(2)
                R.id.menu_3 -> updateConcurrentCount(3)
                R.id.menu_4 -> updateConcurrentCount(4)
                R.id.menu_5 -> updateConcurrentCount(5)
            }
            true
        }

        popup.show()
    }

    /**
     * 更新同时下载数
     */
    private fun updateConcurrentCount(count: Int) {
        // 更新 UI
        binding.tvConcurrentCount.text = count.toString()

        // TODO: 保存到 SharedPreferences 或传递给下载管理器
        saveConcurrentCount(count)

        // 提示用户
        Toast.makeText(requireContext(), "同时下载数已设置为 $count", Toast.LENGTH_SHORT).show()
    }

    /**
     * 获取当前同时下载数
     */
    private fun getCurrentConcurrentCount(): Int {
        // 从 SharedPreferences 读取，默认 2
        return requireContext()
            .getSharedPreferences("download_prefs", Context.MODE_PRIVATE)
            .getInt("concurrent_count", 2)
    }

    /**
     * 保存同时下载数
     */
    private fun saveConcurrentCount(count: Int) {
        requireContext()
            .getSharedPreferences("download_prefs", Context.MODE_PRIVATE)
            .edit()
            .putInt("concurrent_count", count)
            .apply()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}