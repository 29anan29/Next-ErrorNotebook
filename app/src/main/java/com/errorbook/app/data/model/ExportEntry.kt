package com.errorbook.app.data.model

/** 导出排行榜的一个错因条目，含其前几道代表题的缩略图路径。 */
data class ExportEntry(
    val reasonName: String,
    val category: String?,
    val wrongCount: Int,
    val questionCount: Int,
    val thumbnailPaths: List<String>,
)
