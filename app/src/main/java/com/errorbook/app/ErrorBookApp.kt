package com.errorbook.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 单 Activity 应用入口。
 *
 * onCreate 中不做任何耗时操作（数据库懒加载、图片编解码、OCR 模型加载全部推迟到首次访问），
 * 以满足冷启动 ≤ 2 秒的非功能要求。
 */
@HiltAndroidApp
class ErrorBookApp : Application()