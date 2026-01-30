package com.computer.skyvault.ui.starred

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class StarredViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is starred Fragment"
    }
    val text: LiveData<String> = _text
}