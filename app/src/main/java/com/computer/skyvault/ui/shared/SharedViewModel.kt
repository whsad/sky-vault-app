package com.computer.skyvault.ui.shared

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is shared Fragment"
    }
    val text: LiveData<String> = _text
}