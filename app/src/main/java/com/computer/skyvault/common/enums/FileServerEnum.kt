package com.computer.skyvault.common.enums

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
    val suffix: List<String>
) {
    // 图片 1:图片
    IMAGE(FileCategoryEnum.IMAGE, 1,
        listOf(".jpeg", ".jfif", ".jpg", ".png", ".gif", ".bmp", ".dds", ".psd", ".pdt", ".webp", ".xmp", ".svg", ".tiff")),
    // 视频 2:视频
    VIDEO(FileCategoryEnum.VIDEO, 2,
        listOf(".mp4", ".avi", ".rmvb", ".mkv", ".mov")),
    // 音频 3:音频
    AUDIO(FileCategoryEnum.AUDIO, 3,
        listOf(".mp3", ".m4a", ".wav", ".wma", ".mp2", ".flac", ".midi", ".ra", ".ape", ".aac", ".cda")),
    // PDF 4:pdf
    PDF(FileCategoryEnum.DOCUMENT, 4,
        listOf(".pdf")),
    // WORD 5:word
    WORD(FileCategoryEnum.DOCUMENT, 5,
        listOf(".docx")),
    // EXCEL 6:excel
    EXCEL(FileCategoryEnum.DOCUMENT, 6,
        listOf(".xlsx", ".csv")),
    // TXT 7:txt
    TXT(FileCategoryEnum.DOCUMENT, 7,
        listOf(".txt")),
    // CODE 8:code
    CODE(FileCategoryEnum.OTHER, 8,
        listOf(
            ".h", ".c", ".hpp", ".hxx", ".cpp", ".cc", ".c++", ".cxx", ".m", ".o", ".s", ".dll", ".cs",
            ".java", ".class", ".js", ".ts", ".css", ".scss", ".vue", ".jsx", ".sql", ".md", ".json", ".html", ".xml"
        )),
    // ZIP 9:zip
    ZIP(FileCategoryEnum.OTHER, 9,
        listOf("rar", ".zip", ".7z", ".cab", ".arj", ".lzh", ".tar", ".gz", ".ace", ".uue", ".bz", ".jar", ".iso", ".mpq")),
    // APP 10:App
    APP(FileCategoryEnum.APP, 10,
        listOf(".exe", ".msi", ".dmg", ".deb", ".rpm", ".apk", ".ipa", ".app", ".bat", ".sh", ".cmd")),
    // BT_SEEDS 11:种子文件
    BT_SEEDS(FileCategoryEnum.BT_SEEDS, 11,
        listOf(".torrent")),
    // OTHERS 12:其他
    OTHERS(FileCategoryEnum.OTHER, 12,
        emptyList());

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
        fun getByType(typeCode: Int): FileTypeEnum? {
            return FileTypeEnum.entries.find { it.typeCode == typeCode }
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
}

enum class UploadStatusEnum(val status: String) {
    INSTANT_UPLOAD("Instant upload"),
    UPLOADING("Uploading"),
    UPLOAD_FINISH("Upload finish")
}

enum class FileStatusEnum(val value: Int) {
    TRANSCODING(0),
    TRANSCODE_FAILED(1),
    TRANSCODE_COMPLETED(2)
}

enum class DelFlagEnum(val value: Int) {
    DELETED(0),
    RECYCLED(1),
    NORMAL(2),
    ARCHIVED(3)
}