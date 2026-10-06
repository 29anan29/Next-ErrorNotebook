package com.errorbook.app.platform.file

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * SAF 封装：目录选择、uri 权限持久化、向用户选定目录写文件。
 * 注意所有 URI 权限都必须在主线程外做持久化（takePersistableUriPermission 可在 IO 线程）。
 */
@Singleton
class FileSaveManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createDirectoryPickerIntent(): Intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
    }

    fun persistPermission(treeUri: Uri) {
        try {
            context.contentResolver.takePersistableUriPermission(
                treeUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // 同一个目录短时间内重复 persist 会抛 IllegalArgumentException/SecurityException，
            // 视为已持久化，忽略即可。
        }
    }

    fun hasPersistedPermission(treeUri: Uri): Boolean = context.contentResolver.persistedUriPermissions.any { it.uri == treeUri }

    suspend fun writeFile(treeUri: Uri, fileName: String, mimeType: String, bytes: ByteArray): Result<Uri> = runCatching {
        val dir = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("无法访问所选目录")
        // 先删掉已存在的同名文件，避免重名追加
        dir.findFile(fileName)?.delete()
        val file = dir.createFile(mimeType, fileName) ?: error("无法在所选目录创建文件")
        context.contentResolver.openOutputStream(file.uri)?.use { it.write(bytes) }
            ?: error("无法写入所选目录")
        file.uri
    }
}
