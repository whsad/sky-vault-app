package com.computer.skyvault.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.R
import com.computer.skyvault.databinding.ModuleRecycleItemDownloadTaskBinding
import com.computer.skyvault.model.DownloadTask
import com.computer.skyvault.model.TransferStatus

class DownloadTaskAdapter : ListAdapter<DownloadTask, DownloadTaskAdapter.ViewHolder>(DiffCallback()) {
    private var filteredList = listOf<DownloadTask>()

    class ViewHolder(private val binding: ModuleRecycleItemDownloadTaskBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DownloadTask) {
            binding.tvFileName.text = item.name
            binding.tvFileSize.text = item.size

            when (item.status) {
                TransferStatus.RUNNING -> {
                    binding.progressBar.progress = item.progress
                    binding.tvProgress.text = "${item.progress}% ${item.speed}"
                    binding.tvStatus.text = ""
                    binding.ivAction.setImageResource(android.R.drawable.ic_media_pause)
                }
                TransferStatus.COMPLETED -> {
                    binding.progressBar.progress = 100
                    binding.tvProgress.text = ""
                    binding.tvStatus.text = "已完成"
                    binding.tvStatus.setTextColor(itemView.context.getColor(R.color.green))
                    binding.ivAction.setImageResource(android.R.drawable.ic_menu_delete)
                }
                TransferStatus.FAILED -> {
                    binding.progressBar.progress = 0
                    binding.tvProgress.text = ""
                    binding.tvStatus.text = "失败"
                    binding.tvStatus.setTextColor(itemView.context.getColor(R.color.red))
//                    binding.ivAction.setImageResource(android.R.drawable.ic_menu_retry)
                    binding.ivAction.setImageResource(android.R.drawable.ic_menu_agenda)
                }
                else -> {}
            }

            // 操作按钮点击
            binding.ivAction.setOnClickListener {
                when (item.status) {
                    TransferStatus.RUNNING -> { /* 暂停 */ }
                    TransferStatus.COMPLETED -> { /* 删除 */ }
                    TransferStatus.FAILED -> { /* 重试 */ }
                    else -> {}
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ModuleRecycleItemDownloadTaskBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(filteredList[position])
    }

    override fun getItemCount() = filteredList.size

    // 筛选逻辑
    fun filter(statuses: List<TransferStatus>) {
        filteredList = if (statuses.isEmpty()) currentList else currentList.filter { it.status in statuses }
        notifyDataSetChanged()
    }

    // 提交数据时同步筛选列表
    override fun submitList(list: List<DownloadTask>?) {
        super.submitList(list)
        filteredList = list ?: emptyList()
    }

    // DiffCallback（优化列表刷新）
    class DiffCallback : DiffUtil.ItemCallback<DownloadTask>() {
        override fun areItemsTheSame(oldItem: DownloadTask, newItem: DownloadTask) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: DownloadTask, newItem: DownloadTask) = oldItem == newItem
    }
}