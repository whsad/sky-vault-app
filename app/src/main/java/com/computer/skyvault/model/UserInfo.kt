package com.computer.skyvault.model

import java.time.LocalDateTime

/**
 * 用户信息实体类
 */
data class UserInfo(
    val id: String,
    val email: String,
    val hashedPassword: String,
    val isActive: Boolean,
    val isSuperuser: Boolean,
    val isVerified: Boolean,
    val nickName: String,
    val joinTime: LocalDateTime,
    val lastLoginTime: LocalDateTime,
    val useSpace: Long,
    val totalSpace: Long
)