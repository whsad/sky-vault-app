package com.computer.skyvault.common.vo

data class UserInfoVo(
    val id: String,
    val nick_name: String,
    val is_superuser: Boolean
)

// 登录信息数据类
data class LoginInfoVo(
    val user: UserInfoVo,
    val access_token: String,
    val refresh_token: String,
    val token_type: String,
    val expires_in: Int
)