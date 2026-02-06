package com.computer.skyvault.ui.myfiles

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.computer.skyvault.common.dto.LoadFileListRequest
import com.computer.skyvault.service.FileInfoService
import com.computer.skyvault.utils.showToast

class MyFilesViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "This is my files Fragment"
    }
    val text: LiveData<String> = _text
}