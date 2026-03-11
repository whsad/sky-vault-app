package com.computer.skyvault.common.enums

import com.computer.skyvault.R

/**
 * 文件服务器相关枚举类
 */

enum class FileFolderTypeEnum(val value: Int) {
    FILE(0),
    FOLDER(1)
}

enum class FileCategoryEnum(val code: Int, val category: String) {
    IMAGE(1, "Image"),
    VIDEO(2, "video"),
    AUDIO(3, "Audio"),
    DOCUMENT(4, "Document"),
    APP(5, "App"),
    BT_SEEDS(6, "BT seeds"),
    OTHER(7, "Other");

    companion object {
        fun getByCategory(category: String): FileCategoryEnum? {
            return FileCategoryEnum.entries.firstOrNull { it.category == category }
        }
    }
}

enum class FileTypeEnum(
    val category: FileCategoryEnum,
    val typeCode: Int,
    val suffix: List<String>,
    val storageSubdir: String
) {
    // 图片 1:图片
    IMAGE(FileCategoryEnum.IMAGE, 1,
        listOf(".jpeg", ".jfif", ".jpg", ".png", ".gif", ".bmp", ".dds", ".psd", ".pdt", ".webp", ".xmp", ".svg", ".tiff"),
        "images"),
    // 视频 2:视频
    VIDEO(FileCategoryEnum.VIDEO, 2,
        listOf(".mp4", ".avi", ".rmvb", ".mkv", ".mov"),
        "videos"),
    // 音频 3:音频
    AUDIO(FileCategoryEnum.AUDIO, 3,
        listOf(".mp3", ".m4a", ".wav", ".wma", ".mp2", ".flac", ".midi", ".ra", ".ape", ".aac", ".cda"),
        "audios"),
    // PDF 4:pdf
    PDF(FileCategoryEnum.DOCUMENT, 4,
        listOf(".pdf"),
        "documents"),
    // WORD 5:word
    WORD(FileCategoryEnum.DOCUMENT, 5,
        listOf(".docx"),
        "documents"),
    // EXCEL 6:excel
    EXCEL(FileCategoryEnum.DOCUMENT, 6,
        listOf(".xlsx", ".csv"),
        "documents"),
    // TXT 7:txt
    TXT(FileCategoryEnum.DOCUMENT, 7,
        listOf(".txt"),
        "documents"),
    // CODE 8:code
    CODE(FileCategoryEnum.OTHER, 8,
        listOf(
            ".h", ".c", ".hpp", ".hxx", ".cpp", ".cc", ".c++", ".cxx", ".m", ".o", ".s", ".dll", ".cs",
            ".java", ".class", ".js", ".ts", ".css", ".scss", ".vue", ".jsx", ".sql", ".md", ".json", ".html", ".xml"
        ),"codes"),
    // ZIP 9:zip
    ZIP(FileCategoryEnum.OTHER, 9,
        listOf("rar", ".zip", ".7z", ".cab", ".arj", ".lzh", ".tar", ".gz", ".ace", ".uue", ".bz", ".jar", ".iso", ".mpq"),
        "archives"),
    // APP 10:App
    APP(FileCategoryEnum.APP, 10,
        listOf(".exe", ".msi", ".dmg", ".deb", ".rpm", ".apk", ".ipa", ".app", ".bat", ".sh", ".cmd"),
        "applications"),
    // BT_SEEDS 11:种子文件
    BT_SEEDS(FileCategoryEnum.BT_SEEDS, 11,
        listOf(".torrent"),
        "bt_seeds"),
    // OTHERS 12:其他
    OTHERS(FileCategoryEnum.OTHER, 12,
        emptyList(), "others");

    /**
     * 获取对应的 MIME Type
     */
    fun getMimeType(): String {
        return when (this) {
            IMAGE -> "image/*"
            VIDEO -> "video/*"
            AUDIO -> "audio/*"
            PDF -> "application/pdf"
            WORD -> "application/msword"
            EXCEL -> "application/vnd.ms-excel"
            TXT -> "text/plain"
            CODE -> "text/plain"
            ZIP -> "application/zip"
            APP -> "application/vnd.android.package-archive"
            BT_SEEDS -> "application/x-bittorrent"
            OTHERS -> "*/*"
        }
    }

    /**
     * 根据文件后缀获取文件类型
     */
    companion object {
        fun getFileTypeBySuffix(suffix: String): FileTypeEnum {
            val lowerSuffix = suffix.lowercase()
            return FileTypeEnum.entries.find { lowerSuffix in it.suffix } ?: OTHERS
        }

        /**
         * 根据类型码获取文件类型
         */
        fun getByType(typeCode: Int?): FileTypeEnum? {
            return FileTypeEnum.entries.find { it.typeCode == typeCode }
        }

        /**
         * 根据文件后缀获取 MIME Type
         */
        fun getMimeTypeBySuffix(suffix: String): String {
            val fileType = getFileTypeBySuffix(suffix)
            return fileType.getMimeType()
        }

        /**
         * 根据类型码获取 MIME Type
         */
        fun getMimeTypeByType(typeCode: Int?): String {
            val fileType = getByType(typeCode)
            return fileType?.getMimeType() ?: "*/*"
        }
    }

    /**
     * 获取分类码
     */
    val categoryCode: Int get() = category.code

    /**
     * 获取类型码
     */
    val type: Int get() = typeCode

    /**
     * 获取存储子目录名称
     */
    val storageDirectory: String get() = storageSubdir
}

enum class UploadStatusEnum(val status: Int) {
    INSTANT_UPLOAD(0),
    UPLOADING(1),
    UPLOAD_FINISH(2);
}

enum class FileStatusEnum(val status: Int) {
    TRANSCODING(0),
    TRANSCODE_FAILED(1),
    TRANSCODE_COMPLETED(2);

    companion object {
        /**
         * 根据类型码获取文件类型
         */
        fun getByType(statusCode: Int?): FileStatusEnum? {
            return FileStatusEnum.entries.find { it.status == statusCode }
        }
    }
}

enum class DelFlagEnum(val value: Int) {
    DELETED(0),
    RECYCLED(1),
    NORMAL(2),
    ARCHIVED(3)
}