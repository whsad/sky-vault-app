package com.computer.skyvault.utils

import android.view.View

/**
 * 设置View点击事件
 */
fun setViewClickListener(view: View, onClick: () -> Unit) {
    view.setOnClickListener { onClick.invoke() }
}

/**
 * 设置防重复点击的View点击事件
 * @param interval 防重复点击间隔（默认500ms）
 */
fun setSingleClickListener(view: View, interval: Long = 500, onClick: () -> Unit) {
    view.setOnClickListener(object : View.OnClickListener {
        private var lastClickTime = 0L
        override fun onClick(v: View?) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime >= interval) {
                lastClickTime = currentTime
                onClick.invoke()
            }
        }
    })
}