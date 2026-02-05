package com.computer.skyvault.common.dto

import androidx.annotation.Size

// 注册请求数据类
data class RegisterRequest(
    val email: String,
    @field:Size(min = 2, max = 20)
    val nickName: String,
    @field:Size(min = 8, max = 32)
    val password: String,
    val verificationCode: String
)

// 登录请求数据类
data class LoginRequest(
    val email: String,
    @field:Size(min = 8, max = 32)
    val password: String,
    val verificationCode: String
)

// 重置密码请求数据类
data class ResetPwdRequest(
    val email: String,
    @field:Size(min = 8, max = 32)
    val newPassword: String,
    val verificationCode: String
)

// 更新密码请求数据类
data class UpdatePwdRequest(
    @field:Size(min = 8, max = 32)
    val oldPassword: String,
    @field:Size(min = 8, max = 32)
    val newPassword: String,
    val verificationCode: String
)

// 刷新令牌请求数据类
data class RefreshTokenRequest(
    val refreshToken: String
)

// 更新用户资料请求数据类
data class UpdateProfileRequest(
    @field:Size(min = 1, max = 50)
    val nickName: String
)

// 用户空间信息数据类
data class UserSpaceDto(
    val useSpace: Long,
    val totalSpace: Long
)