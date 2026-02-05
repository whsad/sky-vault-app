package com.computer.skyvault.common.recycleitem

sealed class NavItem(val type: Int) {
    companion object {
        const val TYPE_MENU = 0
        const val TYPE_DIVIDER = 1
        const val TYPE_HEADER = 2
    }
}

data class MenuItem(
    val id: Int,
    val title: String,
    val iconRes: Int? = null,
    var badge: String? = null,
    var isSelected: Boolean = false
) : NavItem(TYPE_MENU)

class DividerItem : NavItem(TYPE_DIVIDER)

data class HeaderItem(
    val nickname: String,
    val email: String
) : NavItem(TYPE_HEADER)