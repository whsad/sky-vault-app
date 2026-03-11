package com.computer.skyvault.common.constant

/**
 * 文件相关常量
 */
object FileConstant {
    // 缓存有效期（1小时）
    const val CACHE_VALID_DURATION = 3600000L

    // 下载/上传缓冲区大小（8KB）
    const val BUFFER_SIZE = 8192

    // 网络超时时间（30秒）
    const val NETWORK_TIMEOUT_SECONDS = 30L

    // 文件Provider授权
    const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"

    // 分页默认参数
    const val DEFAULT_PAGE_NO = 1
    const val DEFAULT_PAGE_SIZE = 15
    const val MAX_PAGE_SIZE = 100

    // 日志TAG
    const val TAG_FILE_SERVICE = "FileInfoService"
    const val TAG_MY_FILES = "MyFilesFragment"

    // 文件名最大长度
    const val MAX_FILE_NAME_LENGTH = 255
}