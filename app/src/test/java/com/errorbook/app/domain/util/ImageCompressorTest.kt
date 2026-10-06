package com.errorbook.app.domain.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import java.io.File
import kotlin.math.min
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * 压缩必须真的把体积压到上限内。
 * 文档里的旧实现一旦 quality 递减到下限仍超标就会带着超标文件退出，
 * 所以这里专门用一张高熵（噪声）大图逼出兜底降采样分支。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ImageCompressorTest {

    private val tmpDir: File = createTempDir(prefix = "compress-test")

    @Test
    fun `压缩后的文件不超过设定上限`() = runBlocking {
        val source = writeNoiseJpeg(width = 2400, height = 2400)
        val output = File(tmpDir, "out.jpg")

        ImageCompressor.compress(source, output, maxSizeKB = 500)

        assertTrue(
            "压缩后仍有 ${output.length() / 1024}KB，超过 500KB",
            output.length() / 1024 <= 500,
        )
    }

    @Test
    fun `小图也会产出可解码的 jpeg`() = runBlocking {
        val source = writeNoiseJpeg(width = 200, height = 200)
        val output = File(tmpDir, "small.jpg")

        ImageCompressor.compress(source, output, maxSizeKB = 500)

        assertTrue(output.exists() && output.length() > 0)
        val decoded = android.graphics.BitmapFactory.decodeFile(output.absolutePath)
        assertTrue("压缩产物无法解码", decoded != null && decoded.width > 0)
    }

    @Test
    fun `多次压缩使用不同输出文件互不干扰`() = runBlocking {
        val source = writeNoiseJpeg(width = 1200, height = 1200)
        val a = File(tmpDir, "a.jpg")
        val b = File(tmpDir, "b.jpg")

        ImageCompressor.compress(source, a, maxSizeKB = 500)
        ImageCompressor.compress(source, b, maxSizeKB = 500)

        assertTrue(a.exists() && b.exists())
        assertTrue(a.length() > 0 && b.length() > 0)
    }

    /** 生成纯随机噪声图——JPEG 最难压缩的内容，逼出降采样兜底。 */
    private fun writeNoiseJpeg(width: Int, height: Int): File {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = java.util.Random(42)
        val pixels = IntArray(width * height) {
            Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
        }
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val file = File(tmpDir, "noise-${min(width, height)}-$random.jpg")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        return file
    }

    private fun createTempDir(prefix: String): File = java.nio.file.Files.createTempDirectory(prefix).toFile()
}
