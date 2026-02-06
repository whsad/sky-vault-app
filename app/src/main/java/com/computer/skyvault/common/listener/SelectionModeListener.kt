package com.computer.skyvault.common.listener

interface SelectionModeListener {
    fun onSelectionModeChanged(isInSelectionMode: Boolean, selectedCount: Int)
    fun onExitSelectionMode()
    fun onSelectAll()
}