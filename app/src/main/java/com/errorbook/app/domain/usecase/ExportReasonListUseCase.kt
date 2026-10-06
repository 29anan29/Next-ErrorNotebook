package com.errorbook.app.domain.usecase

import android.net.Uri
import com.errorbook.app.data.model.ExportEntry
import com.errorbook.app.data.repository.ExportRepository
import javax.inject.Inject

class ExportReasonListUseCase @Inject constructor(
    private val exportRepository: ExportRepository,
) {
    /** 生成 PDF 并写入 [dirUri]，返回文件 uri。 */
    suspend operator fun invoke(dirUri: Uri, fileName: String, entries: List<ExportEntry>): Result<Uri> = exportRepository.exportReasonList(dirUri, fileName, entries)

    /** 只生成字节（不写盘），供导出预览的分享。 */
    fun generateBytes(entries: List<ExportEntry>): ByteArray = exportRepository.generatePdfBytes(entries)
}
