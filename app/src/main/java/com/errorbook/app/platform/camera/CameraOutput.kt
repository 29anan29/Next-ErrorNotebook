package com.errorbook.app.platform.camera

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * 系统相机拍照需要把输出目标暴露成一个可写的 Uri。
 * 用 FileProvider 把沙箱内的临时文件授权出去——不需要任何存储权限。
 *
 * authority 用 context.packageName 拼装，这样 debug 的 applicationIdSuffix
 * 也能对上；AndroidManifest 里对应声明为 ${applicationId}.fileprovider。
 */
object CameraOutput {
    private const val DIR_CAPTURE = "capture"

    fun createUri(context: Context): Uri {
        val dir = File(context.cacheDir, DIR_CAPTURE).apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
