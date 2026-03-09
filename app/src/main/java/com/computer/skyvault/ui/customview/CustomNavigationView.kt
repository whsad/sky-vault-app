package com.computer.skyvault.ui.customview

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.computer.skyvault.R
import com.computer.skyvault.adapter.RecycleItemCustomNavAdapter
import com.computer.skyvault.common.recycleitem.HeaderItem
import com.computer.skyvault.common.recycleitem.MenuItem
import com.computer.skyvault.common.recycleitem.NavItem
import com.computer.skyvault.databinding.CustomviewLinearlayoutNavigationViewBinding

class CustomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private lateinit var binding: CustomviewLinearlayoutNavigationViewBinding

    private lateinit var navHeaderLinearLayout: LinearLayout
    private lateinit var recyclerViewMenu: RecyclerView
    private lateinit var imageViewPortrait: AppCompatImageView
    private lateinit var textViewNickname: TextView
    private lateinit var textViewEmail: TextView
    private lateinit var uploadButton: Button

    private var menuItems: List<NavItem> = emptyList()
    private var adapter: RecycleItemCustomNavAdapter? = null
    private var selectedItemId: Int = -1
    private var onNavigationItemSelectedListener: ((Int) -> Unit)? = null
    private var onHeaderClickListener: (() -> Unit)? = null
    private var onUploadClickListener: (() -> Unit)? = null


    init {
        orientation = VERTICAL
        setBackgroundColor(Color.WHITE)
        setupView()
        initAttributes(attrs)
    }

    private fun setupView() {
        binding = CustomviewLinearlayoutNavigationViewBinding.inflate(LayoutInflater.from(context), this, true)

        // 初始化视图
        navHeaderLinearLayout = binding.navHeader
        recyclerViewMenu = binding.menuRecycleView
        imageViewPortrait = binding.portrait
        textViewNickname = binding.nickname
        textViewEmail = binding.email
        uploadButton = binding.btnUploadFiles

        // 设置 RecyclerView
        recyclerViewMenu.layoutManager = LinearLayoutManager(context)

        // 设置头部点击事件
        navHeaderLinearLayout.setOnClickListener {
            onHeaderClickListener?.invoke()
        }

        // 设置上传按钮点击事件
        uploadButton.setOnClickListener {
            onUploadClickListener?.invoke()
        }
    }

    private fun initAttributes(attrs: AttributeSet?) {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.CustomNavigationView) {
                // 设置背景
                val background = getDrawable(R.styleable.CustomNavigationView_android_background)
                background?.let { drawable ->
                    setBackground(drawable)
                }

                // 设置头部背景
                val headerBackground = getDrawable(R.styleable.CustomNavigationView_headerBackground)
                headerBackground?.let { drawable ->
                    navHeaderLinearLayout.background = drawable
                }
            }
        }
    }

    fun setMenuItems(items: List<NavItem>) {
        menuItems = items
        adapter = RecycleItemCustomNavAdapter(items) { itemId ->
            onNavigationItemSelectedListener?.invoke(itemId)
        }
        recyclerViewMenu.adapter = adapter
    }

    fun setSelectedItem(itemId: Int) {
        selectedItemId = itemId
        menuItems.forEachIndexed { index, item ->
            if (item is MenuItem) {
                item.isSelected = item.id == itemId
            }
        }
        adapter?.notifyDataSetChanged()
    }

    fun setHeader(header: HeaderItem) {
        textViewNickname.text = header.nickname
        textViewEmail.text = header.email
    }

    fun setHeaderImage(resourceId: Int) {
        imageViewPortrait.setImageResource(resourceId)
    }

    fun setOnNavigationItemSelectedListener(listener: (Int) -> Unit) {
        onNavigationItemSelectedListener = listener
    }

    fun setOnHeaderClickListener(listener: () -> Unit) {
        onHeaderClickListener = listener
    }

    fun setOnUploadClickListener(listener: () -> Unit) {
        onUploadClickListener = listener
    }

    fun getMenuItemById(itemId: Int): MenuItem? {
        return menuItems.find { it is MenuItem && it.id == itemId } as? MenuItem
    }

    fun updateBadge(itemId: Int, badgeText: String?) {
        menuItems.forEachIndexed { index, item ->
            if (item is MenuItem && item.id == itemId) {
                item.badge = badgeText
                adapter?.notifyItemChanged(index)
            }
        }
    }
}