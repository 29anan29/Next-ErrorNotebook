package com.errorbook.app.domain.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 图片压缩：先采样限制最长边，再按质量递减；若仍超标，继续降采样。
 * 修复文档版本的问题：旧实现质量递减到 30 仍超标就带着超标文件退出，
 * 且从不降采样兜底。
 */
object ImageCompressor {
    private const val MAX_DIMENSION = 2048
    private const val MIN_QUALITY = 40

    suspend fun compress(inputFile: File, outputFile: File, maxSizeKB: Int = 500): File =
        withContext(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(inputFile.absolutePath, bounds)

            var sample = 1
            while (bounds.outWidth / sample > MAX_DIMENSION || bounds.outHeight / sample > MAX_DIMENSION) {
                sample *= 2
            }

            var currentSample = sample
            while (true) {
                val bitmap = BitmapFactory.decodeFile(
                    inputFile.absolutePath,
                    BitmapFactory.Options().apply { inSampleSize = currentSample },
                ) ?: run {
                    // 采样解码失败只能放弃，把原始文件原样复制，避免阻塞录题主流程
                    inputFile.copyTo(outputFile, overwrite = true)
                    return@withContext outputFile
                }

                val rotated = applyExifRotation(bitmap, inputFile)
                var quality = 90
                var ok = false
                while (quality >= MIN_QUALITY) {
                    FileOutputStream(outputFile).use { out ->
                        rotated.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    }
                    if (outputFile.length() / 1024 <= maxSizeKB) {
                        ok = true
                        break
                    }
                    quality -= 15
                }
                if (ok || currentSample >= sample * 8) {
                    bitmap.recycle()
                    if (rotated !== bitmap) rotated.recycle()
                    break
                }
                // 继续降采样再试
                currentSample *= 2
                bitmap.recycle()
                if (rotated !== bitmap) rotated.recycle()
            }
            outputFile
        }

    /** 读 EXIF 方向并旋转，保证拍摄出来的竖版题图不会躺倒。 */
    private fun applyExifRotation(bitmap: Bitmap, file: File): Bitmap {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val rotation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
            val degrees = when (rotation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> return bitmap
            }
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: Exception) {
            bitmap
        }
    }
}