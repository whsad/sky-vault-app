package com.computer.skyvault.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.computer.skyvault.R
import com.computer.skyvault.common.enums.FileTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.MyfilesRecycleItemFileFolderListBinding
import com.computer.skyvault.service.client.FileInfoServiceClient
import com.computer.skyvault.utils.DataUtils
import com.computer.skyvault.utils.DateUtils

private const val TAG = "RecycleItemMyFilesFileF"

class RecycleItemMyFilesFileFolderListAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit
) : RecyclerView.Adapter<RecycleItemMyFilesFileFolderListAdapter.ViewHolder>() {

    private var items: List<FileItem> = emptyList()
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<String>() // 使用Set提高查找效率

    // 监听选择状态变化
    var onSelectionChanged: ((List<FileItem>) -> Unit)? = null
    var onSelectionModeChanged: ((Boolean, Int) -> Unit)? = null

    inner class ViewHolder(binding: MyfilesRecycleItemFileFolderListBinding) : RecyclerView.ViewHolder(binding.root) {
        val rootLayout = binding.rootLayout // item layout
        private val ivFileCover = binding.ivFileCover // 文件封面
        val tvFileName = binding.tvFileName // 文件名称
        val ivStarred = binding.ivStarred // 收藏图标
        val tvUpdateTime = binding.tvUpdateTime  // 上传时间
        val tvFileSize = binding.tvFileSize // 文件大小
        val ibOperate = binding.ibOperate // 文件操作

        init {
            // 设置操作图标点击事件
            ibOperate.setOnClickListener {
                val position = bindingAdapterPosition

                if (position != RecyclerView.NO_POSITION) {
                    val item = items[position]
                    val fileId = item.fileId
                    if (!isSelectionMode) {
                        // 如果不在 selection mode, enter selection mode 并选中当前项
                        enterSelectionMode()
                        selectedItems.add(fileId)
                        notifySelectionChanged()
                        // 更新操作图标
                        ibOperate.setImageResource(R.drawable.ic_checked)
                    } else {
                        // 自动切换选中状态
                        toggleSelection(fileId)
                    }
                }
            }
        }

        fun bindItem(item: FileItem) {
            // 根据文件类型设置封面图
            val fileType = FileTypeEnum.getByType(item.fileType)
            DataUtils.setFileCoverByType(fileType, item, ivFileCover)
            // 设置文件名称
            tvFileName.text = item.fileName
            // 设置是否收藏
            ivStarred.visibility = if (item.isStarred) View.VISIBLE else View.GONE
            // 设置修改时间
            tvUpdateTime.text = DateUtils.formatIsoDateTime(item.lastUpdateTime)
            // 设置文件大小
            if (item.status == 0) {
                tvFileSize.text = "转码中"
            } else if (item.status == 1) {
                tvFileSize.text = "转码失败"
            }else {
                tvFileSize.text = if (fileType == null) "" else DataUtils.formatFileSize(item.fileSize)
            }
            // 动态更新操作图标
            val ibOperateIcon = if (isSelectionMode && item.fileId.let { selectedItems.contains(it) }) {
                R.drawable.ic_checked  // 选中时的对勾图标
            } else {
                R.drawable.ic_pointer   // 未选中时的空心圆图标
            }
            ibOperate.setImageResource(ibOperateIcon)
        }
    }



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = MyfilesRecycleItemFileFolderListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = ViewHolder(binding)

        // 设置 item layout 点击事件监听
        holder.rootLayout.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = items[position]
                if (isSelectionMode) {
                    // 选择模式下，点击切换选中状态
                    toggleSelection(item.fileId)
                } else {
                    // 普通状态下，点击打开文件/文件夹
                    // todo 回调函数
                    onItemClick(item)
                }
            }
        }

        holder.rootLayout.setOnLongClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = items[position]
                if (!isSelectionMode) {
                    // 进入选择状态
                    enterSelectionMode()
                    // 选中当前项
                    selectedItems.add(item.fileId)
                    notifySelectionChanged()
                    // todo 回调函数
                    onItemLongClick(item)
                    return@setOnLongClickListener true
                } else {
                    // 已经在选择状态下，长按切换选中状态
                    toggleSelection(item.fileId)
                    return@setOnLongClickListener true
                }
            }
            false
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.bindItem(item)
    }

    override fun getItemCount(): Int = items.size

    private fun notifyItemChangedByFileId(fileId: String) {
        val position = items.indexOfFirst { it.fileId == fileId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    private fun notifySelectionChanged() {
        val selectedCount = selectedItems.size
        val selectedItemsList = getSelectedItems()
        onSelectionChanged?.invoke(selectedItemsList)
        onSelectionModeChanged?.invoke(isSelectionMode, selectedCount)
    }

    fun toggleSelection(fileId: String) {
        if (selectedItems.contains(fileId)) {
            selectedItems.remove(fileId)
        } else {
            selectedItems.add(fileId)
        }
        notifySelectionChanged()
        notifyItemChangedByFileId(fileId)

        // 如果没有选中项，退出选择模式
        if (selectedItems.isEmpty()) {
            exitSelectionMode()
        }
    }

    fun enterSelectionMode() {
        isSelectionMode = true
        onSelectionModeChanged?.invoke(true, selectedItems.size)
        notifyItemRangeChanged(0, itemCount)
    }

    fun exitSelectionMode() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedItems.clear()
            onSelectionModeChanged?.invoke(false, 0)
            notifyItemRangeChanged(0, itemCount)
        }
    }

    fun checkAll(isCheckAll: Boolean) {
        if (isCheckAll) {
            // 如果已经是全选状态了，就要取消全选
            selectedItems.clear()
        } else {
            // 如果不是全选状态，就全选所有文件
            selectedItems.clear()
            selectedItems.addAll(items.map { it.fileId })
        }
        notifySelectionChanged()
        notifyItemRangeChanged(0, itemCount)
    }

    fun isInSelectionMode() = isSelectionMode

    fun getSelectedItems(): List<FileItem> = items.filter { selectedItems.contains(it.fileId) }

    fun getSelectedCount() = selectedItems.size

    fun getCurrentList(): List<FileItem> = items

    fun submitList(newItems: List<FileItem>) {
        items = newItems
        Log.d(TAG, "submitList: ${selectedItems}")
//        notifyItemRangeChanged(0, itemCount)
        notifyDataSetChanged()
    }
}