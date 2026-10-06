package com.errorbook.app

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Instrumented test 的 runner：让被测进程使用 [HiltTestApplication] 而非真实的
 * [ErrorBookApp]，否则 Hilt 组件在测试进程中无法被替换。
 */
class HiltTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application = super.newApplication(cl, HiltTestApplication::class.java.name, context)
}
