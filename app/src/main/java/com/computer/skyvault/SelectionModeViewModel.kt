package com.computer.skyvault

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SelectionModeViewModel {
    private val _isSelectionMode = MutableLiveData<Boolean>()
    val isSelectionMode: LiveData<Boolean> = _isSelectionMode

    private val _selectedCount = MutableLiveData<Int>()
    val selectedCount: LiveData<Int> = _selectedCount

    fun enterSelectionMode(count: Int) {
        _isSelectionMode.value = true
        _selectedCount.value = count
    }

    fun exitSelectionMode() {
        _isSelectionMode.value = false
        _selectedCount.value = 0
    }
}