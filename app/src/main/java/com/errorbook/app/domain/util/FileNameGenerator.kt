package com.errorbook.app.domain.util

import java.io.File
import java.util.UUID

object FileNameGenerator {
    /** 生成 `日期/uuid.jpg` 形式的唯一图片文件名。 */
    fun imageName(): String = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"

    /** 错因清单导出文件名：`错因清单_yyyyMMdd_HHmm.pdf`。 */
    fun exportPdfName(): String {
        val fmt = java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
        return "错因清单_${fmt.format(System.currentTimeMillis())}.pdf"
    }

    fun imageDir(contextFilesDir: File): File {
        val dir = File(contextFilesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }
}
