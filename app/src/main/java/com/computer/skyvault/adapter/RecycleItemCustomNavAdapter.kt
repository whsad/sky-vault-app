package com.computer.skyvault.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.R
import com.computer.skyvault.common.recycleitem.MenuItem
import com.computer.skyvault.common.recycleitem.NavItem
import com.computer.skyvault.databinding.ModuleRecycleItemCustomNavDividerBinding
import com.computer.skyvault.databinding.ModuleRecycleItemCustomNavMenuBinding

class RecycleItemCustomNavAdapter(
    private var items: List<NavItem>,
    private val onItemClickListener: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    inner class MenuViewHolder(binding: ModuleRecycleItemCustomNavMenuBinding) : RecyclerView.ViewHolder(binding.root) {
        private val itemLayout: LinearLayout = binding.itemLayout
        private val iconView: AppCompatImageView = binding.icon
        private val titleView: TextView = binding.title
        private val badgeView: TextView = binding.textViewBadge
//        private val selectedView: View = View(itemView.context).apply {
//            setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.md_theme_onPrimary))
//        }

        fun bind(menuItem: MenuItem) {
            // 设置图标
            menuItem.iconRes?.let {
                iconView.setImageResource(it)
                iconView.visibility = View.VISIBLE
            } ?: run {
                iconView.visibility = View.GONE
            }

            // 设置标题
            titleView.text = menuItem.title

            // 设置徽章
            menuItem.badge?.let {
                badgeView.text = it
                badgeView.visibility = View.VISIBLE
            } ?: run {
                badgeView.visibility = View.GONE
            }

            // 设置选中状态
            if (menuItem.isSelected) {
//                itemLayout.setBackgroundColor(R.color.md_theme_secondaryContainer)
                itemLayout.setBackgroundResource(R.drawable.nav_item_background)
                iconView.setColorFilter(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onSecondaryContainer)
                )
                titleView.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onSecondaryContainer)
                )
            } else {
                itemLayout.setBackgroundResource(R.color.md_theme_onError)
                iconView.clearColorFilter()
                titleView.setTextColor(
                    ContextCompat.getColor(itemView.context, android.R.color.black)
                )
            }

            // 点击事件
            itemLayout.setOnClickListener {
                onItemClickListener(menuItem.id)
            }
        }
    }

    inner class DividerViewHolder(binding: ModuleRecycleItemCustomNavDividerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun getItemViewType(position: Int): Int = items[position].type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            NavItem.TYPE_MENU -> {
                val binding = ModuleRecycleItemCustomNavMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                MenuViewHolder(binding)
            }

            NavItem.TYPE_DIVIDER -> {
                val binding = ModuleRecycleItemCustomNavDividerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                DividerViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unknown view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MenuViewHolder -> {
                val item = items[position]
                if (item is MenuItem) {
                    holder.bind(item)
                }
            }
            // DividerViewHolder 不需要绑定数据
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateItems(newItems: List<NavItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}