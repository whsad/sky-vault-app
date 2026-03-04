package com.computer.skyvault.adapter

import android.icu.text.DecimalFormat
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.common.dto.SelectedFile
import com.computer.skyvault.databinding.ModuleRecycleItemSelectedFileBinding

class RecycleItemSelectedFileAdapter(
    private val files: List<SelectedFile>,
    private val onItemClick: (SelectedFile) -> Unit
) : RecyclerView.Adapter<RecycleItemSelectedFileAdapter.ViewHolder>() {

    private val formatter = DecimalFormat("#,### KB")

    inner class ViewHolder(private val binding: ModuleRecycleItemSelectedFileBinding): RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(files[position])
                }
            }
        }

        fun bind(file: SelectedFile) {
            binding.tvFileName.text = file.name
            binding.tvFileSize.text = formatFileSize(file.size)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ModuleRecycleItemSelectedFileBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(files[position])
    }

    override fun getItemCount(): Int = files.size

    private fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${formatter.format(size / 1024.0)}"
            else -> "${DecimalFormat("#,###.## MB").format(size / (1024.0 * 1024.0))}"
        }
    }
}