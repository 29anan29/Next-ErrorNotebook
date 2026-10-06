package com.errorbook.app.data.repository

import android.net.Uri
import com.errorbook.app.data.model.ExportEntry
import com.errorbook.app.platform.file.FileSaveManager
import com.errorbook.app.platform.file.PdfExporter
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 导出编排：先生成 PDF 字节、再写进用户通过 SAF 选择的目录。
 * 字节都在内存里一次性产出（Top N 一般是几十个缩略图，远小于内存），
 * 因此不需要落临时文件。
 */
@Singleton
class ExportRepository @Inject constructor(
    private val fileSaveManager: FileSaveManager,
    private val pdfExporter: PdfExporter,
) {
    /** 生成 PDF 并写入 [dirUri]，返回文件名；失败回 Result.failure。 */
    suspend fun exportReasonList(dirUri: Uri, fileName: String, entries: List<ExportEntry>): Result<Uri> {
        return try {
            val buffer = ByteArrayOutputStream()
            pdfExporter.exportReasonList(entries, buffer)
            val bytes = buffer.toByteArray()
            fileSaveManager.writeFile(dirUri, fileName, "application/pdf", bytes)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** 仅生成 PDF 字节（不落盘/不写 SAF），供测试或分享预览复用。 */
    fun generatePdfBytes(entries: List<ExportEntry>): ByteArray {
        val buffer = ByteArrayOutputStream()
        pdfExporter.exportReasonList(entries, buffer)
        return buffer.toByteArray()
    }
}
