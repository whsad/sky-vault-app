package com.computer.skyvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.R
import com.computer.skyvault.common.enums.FileTypeEnum
import com.computer.skyvault.common.recycleitem.FileItem
import com.computer.skyvault.databinding.ModuleRecycleItemMyFilesFileFolderListBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecycleItemMyFilesFileFolderListAdapter(
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit
) : RecyclerView.Adapter<RecycleItemMyFilesFileFolderListAdapter.ViewHolder>() {

    companion object {
        private val INPUT_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        private val OUTPUT_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    }

    private var items: List<FileItem> = emptyList()
    private var isSelectionMode = false
    private val selectedItems = mutableSetOf<String>() // 使用Set提高查找效率

    // 监听选择状态变化
    var onSelectionChanged: ((List<FileItem>) -> Unit)? = null
    var onSelectionModeChanged: ((Boolean, Int) -> Unit)? = null

    inner class ViewHolder(binding: ModuleRecycleItemMyFilesFileFolderListBinding) : RecyclerView.ViewHolder(binding.root) {
        val ivFileCover = binding.ivFileCover
        val tvFileName = binding.tvFileName
        val ivStarred = binding.ivStarred
        val tvUploadTime = binding.tvUploadTime
        val tvFileSize = binding.tvFileSize
        val ibOperate = binding.ibOperate
        val rootLayout = binding.rootLayout

        // 保存当前绑定的itemId
        private var currentItemId: String? = null

        init {
            // 初始化时设置为默认图标（空心圆）
            ibOperate.setImageResource(R.drawable.ic_pointer)
            ibOperate.visibility = View.VISIBLE

            // 设置图标点击事件
            ibOperate.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = items.getOrNull(position) ?: return@setOnClickListener
                    handleIconClick(item)
                }
            }
        }

        fun bindItem(item: FileItem) {
            currentItemId = item.fileId

            // 设置文本
            tvFileName.text = item.fileName ?: "未知文件"
            tvFileSize.text = item.fileSize?.toString() ?: "未知大小"
            ivStarred.visibility = if (item.isStarred) View.VISIBLE else View.GONE

            // 设置上传时间
            tvUploadTime.text = formatUploadTime(item.lastUpdateTime)

            // 设置文件图标
            setFileIcon(ivFileCover, item.fileType)

            // 更新操作按钮图标
            updateOperationIcon(item)
        }

        private fun updateOperationIcon(item: FileItem) {
            val iconRes = if (isSelectionMode && item.fileId?.let { selectedItems.contains(it) } == true) {
                R.drawable.ic_checked  // 选中时的对勾图标
            } else {
                R.drawable.ic_pointer   // 未选中时的空心圆图标
            }
            ibOperate.setImageResource(iconRes)
        }

        private fun handleIconClick(item: FileItem) {
            if (!isSelectionMode) {
                // 如果不在选择模式，进入选择模式并选中当前项
                enterSelectionMode()
                item.fileId?.let { fileId ->
                    selectedItems.add(fileId)
                    notifySelectionChanged()
                    // 更新当前项的图标
                    ibOperate.setImageResource(R.drawable.ic_checked)
                }
            } else {
                // 已经在选择模式，切换选中状态
                item.fileId?.let { fileId ->
                    toggleSelection(fileId)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ModuleRecycleItemMyFilesFileFolderListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        val holder = ViewHolder(binding)

        // 设置整个item的点击事件监听器
        holder.rootLayout.setOnClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = items.getOrNull(position) ?: return@setOnClickListener
                if (isSelectionMode) {
                    // 选择模式下，点击切换选中状态
                    item.fileId?.let { fileId ->
                        toggleSelection(fileId)
                    }
                } else {
                    // 普通模式下，点击打开文件/文件夹
                    onItemClick(item)
                }
            }
        }

        holder.rootLayout.setOnLongClickListener {
            val position = holder.bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                val item = items.getOrNull(position) ?: return@setOnLongClickListener false
                if (!isSelectionMode) {
                    // 进入选择模式
                    enterSelectionMode()
                    // 选中当前项
                    item.fileId?.let { fileId ->
                        selectedItems.add(fileId)
                        notifySelectionChanged()
                    }
                    onItemLongClick(item)
                    return@setOnLongClickListener true
                } else {
                    // 已经在选择模式下，长按切换选中状态
                    item.fileId?.let { fileId ->
                        toggleSelection(fileId)
                    }
                    return@setOnLongClickListener true
                }
            }
            false
        }

        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.bindItem(item)
    }

    override fun getItemCount(): Int = items.size

    private fun formatUploadTime(timeString: String?): String {
        return try {
            timeString?.let {
                val date = INPUT_DATE_FORMAT.parse(it)
                OUTPUT_DATE_FORMAT.format(date)
            } ?: "未知时间"
        } catch (e: Exception) {
            "未知时间"
        }
    }

    private fun setFileIcon(imageView: AppCompatImageView, fileType: Int?) {
        val iconRes = when (fileType) {
            FileTypeEnum.IMAGE.typeCode -> R.drawable.image1
            FileTypeEnum.VIDEO.typeCode -> R.drawable.ic_video_file
            FileTypeEnum.AUDIO.typeCode -> R.drawable.ic_file_type_audio
            FileTypeEnum.PDF.typeCode -> R.drawable.ic_file_type_pdf
            FileTypeEnum.WORD.typeCode -> R.drawable.ic_file_type_word
            FileTypeEnum.EXCEL.typeCode -> R.drawable.ic_file_type_excel
            FileTypeEnum.TXT.typeCode -> R.drawable.ic_file_type_txt
            FileTypeEnum.CODE.typeCode -> R.drawable.ic_file_type_code
            FileTypeEnum.ZIP.typeCode -> R.drawable.ic_file_type_zip
            FileTypeEnum.APP.typeCode -> R.drawable.ic_file_type_app
            FileTypeEnum.BT_SEEDS.typeCode -> R.drawable.ic_file_type_bt
            FileTypeEnum.OTHERS.typeCode -> R.drawable.ic_file_type_other
            else -> R.drawable.ic_file_type_folder  // 文件夹图标
        }
        imageView.setImageResource(iconRes)
    }

    private fun toggleSelection(fileId: String) {
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

    private fun notifyItemChangedByFileId(fileId: String) {
        val position = items.indexOfFirst { it.fileId == fileId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun enterSelectionMode() {
        if (!isSelectionMode) {
            isSelectionMode = true
            onSelectionModeChanged?.invoke(true, selectedItems.size)
            notifyDataSetChanged() // 通知所有项更新图标
        }
    }

    fun exitSelectionMode() {
        if (isSelectionMode) {
            isSelectionMode = false
            selectedItems.clear()
            onSelectionModeChanged?.invoke(false, 0)
            notifyDataSetChanged() // 通知所有项更新图标
        }
    }

    fun isInSelectionMode() = isSelectionMode

    fun getSelectedItems(): List<FileItem> {
        return items.filter { it.fileId != null && selectedItems.contains(it.fileId) }
    }

    fun getSelectedCount() = selectedItems.size

    fun selectAll() {
        if (!isSelectionMode) {
            enterSelectionMode()
        }
        selectedItems.clear()
        items.forEach { item ->
            if (item.isSelectable) {
                item.fileId?.let { fileId ->
                    selectedItems.add(fileId)
                }
            }
        }
        notifySelectionChanged()
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedItems.clear()
        notifySelectionChanged()
        if (isSelectionMode) {
            exitSelectionMode()
        }
    }

    private fun notifySelectionChanged() {
        val selectedCount = selectedItems.size
        val selectedItemsList = getSelectedItems()
        onSelectionChanged?.invoke(selectedItemsList)
        onSelectionModeChanged?.invoke(isSelectionMode, selectedCount)
    }

    fun submitList(newItems: List<FileItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    // 为外部提供更新选中数量的方法
    fun updateSelectedCountDisplay() {
        val selectedCount = selectedItems.size
        onSelectionModeChanged?.invoke(isSelectionMode, selectedCount)
    }
}