package com.errorbook.app.platform.camera

import android.content.Context
import android.net.Uri
import com.errorbook.app.domain.util.FileNameGenerator
import com.errorbook.app.domain.util.ImageCompressor
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 把用户选中的图片（拍照或相册）落到应用沙箱并压缩，返回最终文件路径。
 * 沙箱位于 filesDir/images/，用户无法通过文件管理器直接访问。
 */
@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** 相册选图 → 复制到沙箱并压缩。 */
    suspend fun importFromUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = newTarget()
        val temp = tempFor(target)
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取所选图片")
        ImageCompressor.compress(temp, target)
        temp.delete()
        target.absolutePath
    }

    /** 相机拍照产物（FileProvider 授权的 content:// uri）→ 压缩进沙箱后删除临时文件。 */
    suspend fun importCapture(uri: Uri): String = withContext(Dispatchers.IO) {
        val target = newTarget()
        val temp = tempFor(target)
        context.contentResolver.openInputStream(uri)?.use { input ->
            temp.outputStream().use { output -> input.copyTo(output) }
        } ?: error("无法读取拍照结果")
        ImageCompressor.compress(temp, target)
        temp.delete()
        deleteCaptureTemp()
        target.absolutePath
    }

    private fun newTarget(): File = File(FileNameGenerator.imageDir(context.filesDir), FileNameGenerator.imageName())

    private fun tempFor(target: File): File = File(target.parentFile, "${target.name}.tmp")

    /** 清理相机临时目录，避免用户反复重拍留下垃圾。 */
    private fun deleteCaptureTemp() {
        File(context.cacheDir, "capture").listFiles()?.forEach { it.delete() }
    }
}
