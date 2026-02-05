package com.computer.skyvault.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.computer.skyvault.common.vo.LoginInfoVo

class UserViewModel : ViewModel() {

    private val _loginInfo = MutableLiveData<LoginInfoVo?> ()
    val loginInfo: LiveData<LoginInfoVo?> = _loginInfo

    fun setLoginInfo(info: LoginInfoVo){
        _loginInfo.value = info
    }

    fun clearLoginInfo() {
        _loginInfo.value = null
    }
}