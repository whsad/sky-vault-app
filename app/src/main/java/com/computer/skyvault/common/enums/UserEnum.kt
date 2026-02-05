package com.computer.skyvault.common.enums


/**
 * 用户相关枚举类
 */
enum class UserStatusEnum(val value: Int) {
    DISABLE(0),
    ENABLED(1);

    companion object {
        /**
         * 根据状态值获取枚举项
         */
        fun getByStatus(status: Int): UserStatusEnum? {
            return UserStatusEnum.entries.find { it.value == status }
        }
    }
}