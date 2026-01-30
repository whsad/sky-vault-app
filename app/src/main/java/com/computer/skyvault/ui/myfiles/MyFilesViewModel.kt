package com.computer.skyvault.ui.myfiles

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class MyFilesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is my files Fragment"
    }
    val text: LiveData<String> = _text
}