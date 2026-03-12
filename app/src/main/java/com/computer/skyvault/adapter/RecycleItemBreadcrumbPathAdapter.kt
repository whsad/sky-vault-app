package com.computer.skyvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.databinding.RecycleItemBreadcrumbPathBinding

class RecycleItemBreadcrumbPathAdapter(
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RecycleItemBreadcrumbPathAdapter.ViewHolder>() {

    private var items: List<String> = emptyList()
    val currentList: List<String> get() = items

    inner class ViewHolder(
        private val binding: RecycleItemBreadcrumbPathBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.tvFolderPath.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(position)
                }
            }
        }

        fun bind(item: String, isLast: Boolean) {
            binding.tvFolderPath.text = item
            binding.tvSeparator.visibility = if (isLast) View.GONE else View.VISIBLE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = RecycleItemBreadcrumbPathBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val isLast = position == itemCount - 1
        holder.bind(items[position], isLast)
    }

    fun submitList(newItems: List<String>) {
        items = newItems
        notifyDataSetChanged()
    }


}