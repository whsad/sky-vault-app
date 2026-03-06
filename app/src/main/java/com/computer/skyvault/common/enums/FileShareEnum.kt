package com.computer.skyvault.common.enums


/**
 * 文件分享相关枚举类
 */
enum class ShareValidTypeEnum(
    val typeValue: Int,
    val days: Int,
    val description: String
) {
    SEVEN_DAYS(0, 7, "7 天"),
    FOURTEEN_DAYS(1, 14, "14 天"),
    THIRTY_DAYS(2, 30, "30 天"),
    ONE_YEAR(3, 365, "365 天"),
    FOREVER(4, -1, "永久有效");

    companion object {
        /**
         * 根据类型值获取枚举
         */
        fun getByType(typeValue: Int): ShareValidTypeEnum {
            return ShareValidTypeEnum.entries.find { it.typeValue == typeValue } ?: FOREVER
        }
    }
}