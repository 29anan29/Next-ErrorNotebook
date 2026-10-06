package com.errorbook.app.platform.file

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.errorbook.app.data.model.ExportEntry
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用 Android 原生 PdfDocument 生成错因清单 PDF。
 *
 * 注意：
 *  - PageInfo 的页码必须逐页自增，否则导出的 PDF 页码重复/结构损坏；
 *  - 缩略图必须 inSampleSize 采样解码，否则全尺寸解码会 OOM；
 *  - 绘制标题前先判断本页剩余空间，避免内容溢出页底。
 */
@Singleton
class PdfExporter @Inject constructor() {
    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 40f
    private val contentMaxY = pageHeight - 50f

    fun exportReasonList(entries: List<ExportEntry>, output: OutputStream) {
        val pdf = PdfDocument()
        var pageNumber = 0
        var page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
        var canvas = page.canvas
        var y = margin + 10f

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = 16f
            isFakeBoldText = true
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        }

        fun newPage() {
            pdf.finishPage(page)
            page = pdf.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, ++pageNumber).create())
            canvas = page.canvas
            y = margin + 10f
        }

        fun ensure(space: Float) {
            if (y + space > contentMaxY) newPage()
        }

        entries.forEach { entry ->
            ensure(30f)
            canvas.drawText(
                "${entry.reasonName}（${entry.wrongCount} 次 · ${entry.questionCount} 题）",
                margin,
                y,
                titlePaint,
            )
            y += 24f

            entry.thumbnailPaths.take(5).forEach { path ->
                ensure(180f)
                val bitmap = decodeThumbnail(path)
                if (bitmap != null) {
                    val thumb = Bitmap.createScaledBitmap(bitmap, 200, 150, true)
                    canvas.drawBitmap(thumb, margin, y, null)
                    bitmap.recycle()
                    thumb.recycle()
                    y += 160f
                }
                y += 16f
            }
            y += 12f
        }

        pdf.finishPage(page)
        pdf.writeTo(output)
        pdf.close()
    }

    /** 采样解码缩略图：最长边压到 400px 以内，避免全尺寸 OOM。 */
    private fun decodeThumbnail(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 400 || bounds.outHeight / sample > 300) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
    }
}
