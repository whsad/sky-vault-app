package com.computer.skyvault.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.computer.skyvault.common.vo.LoginInfoVo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class LoginManager private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: LoginManager? = null

        fun getInstance(context: Context): LoginManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LoginManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }

        private const val PREF_NAME = "login_prefs"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_NICKNAME = "nickname"
        private const val KEY_EMAIL = "email"
        private const val KEY_IS_SUPERUSER = "is_superuser"
        private const val KEY_TOKEN_TYPE = "token_type"
        private const val KEY_EXPIRES_IN = "expires_in"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _loginInfo = MutableStateFlow<LoginInfoVo?>(null)
    val loginInfo: StateFlow<LoginInfoVo?> = _loginInfo

    init {
        // 在初始化时恢复登录状态
        _loginInfo.value = getLoginInfo()
    }

    fun saveLoginInfo(info: LoginInfoVo) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, info.access_token)
            putString(KEY_REFRESH_TOKEN, info.refresh_token)
            putString(KEY_USER_ID, info.user.id)
            putString(KEY_NICKNAME, info.user.nick_name)
            putString(KEY_EMAIL, info.user.email)
            putBoolean(KEY_IS_SUPERUSER, info.user.is_superuser)
            putString(KEY_TOKEN_TYPE, info.token_type)
            putInt(KEY_EXPIRES_IN, info.expires_in)
        }
        _loginInfo.value = info
    }

    fun getLoginInfo(): LoginInfoVo? {
        val accessToken = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null) ?: ""
        val userId = prefs.getString(KEY_USER_ID, "") ?: ""
        val nickname = prefs.getString(KEY_NICKNAME, "") ?: ""
        val email = prefs.getString(KEY_EMAIL, "") ?: ""
        val isSuperuser = prefs.getBoolean(KEY_IS_SUPERUSER, false)
        val tokenType = prefs.getString(KEY_TOKEN_TYPE, "") ?: ""
        val expiresIn = prefs.getInt(KEY_EXPIRES_IN, 0)

        return LoginInfoVo(
            user = com.computer.skyvault.common.vo.UserInfoVo(
                id = userId,
                nick_name = nickname,
                email = email,
                is_superuser = isSuperuser
            ),
            access_token = accessToken,
            refresh_token = refreshToken,
            token_type = tokenType,
            expires_in = expiresIn
        )
    }

    fun clearLoginInfo() {
        prefs.edit { clear() }
        _loginInfo.value = null
    }

    fun isLoggedIn(): Boolean = getLoginInfo() != null

    fun getAccessToken(): String? = getLoginInfo()?.access_token
}