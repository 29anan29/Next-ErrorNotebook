plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.spotless)
}

/**
 * release 签名配置。凭据来自 Gradle 属性（-P...）或同名环境变量，
 * 两者都不存在时**不创建** release 签名配置——这保证没有密钥也能本地
 * assembleDebug，甚至产出 unsigned release，而不会在配置阶段直接失败。
 *
 * CI 传入（见 .github/workflows/release.yml）：
 *   ERRORBOOK_KEYSTORE            keystore 在工作区内的路径
 *   ERRORBOOK_KEYSTORE_PASSWORD   keystore 口令
 *   ERRORBOOK_KEY_ALIAS           key alias
 *   ERRORBOOK_KEY_PASSWORD        key 口令
 */
// 变量名刻意加了 release 前缀：在 signingConfigs.create("release") { ... } 作用域内，
// keyPassword / keyAlias / storeFile 等名字会被 SigningConfig 自身的属性遮蔽，
// 直接用同名外层变量会读到 SigningConfig 的空值，导致 packageRelease 报
// "missing required property keyPassword"。
val releaseStoreFile: String? = providers.gradleProperty("ERRORBOOK_KEYSTORE").orNull
    ?: System.getenv("ERRORBOOK_KEYSTORE")
val releaseStorePassword: String? = providers.gradleProperty("ERRORBOOK_KEYSTORE_PASSWORD").orNull
    ?: System.getenv("ERRORBOOK_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? = providers.gradleProperty("ERRORBOOK_KEY_ALIAS").orNull
    ?: System.getenv("ERRORBOOK_KEY_ALIAS")
val releaseKeyPassword: String? = providers.gradleProperty("ERRORBOOK_KEY_PASSWORD").orNull
    ?: System.getenv("ERRORBOOK_KEY_PASSWORD")

val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

// 版本号默认写在构建脚本里，CI 发布时可用 -PVERSION_NAME / -PVERSION_CODE 覆盖，
// 保证 tag、APK 内 BuildConfig、Release 说明三者始终一致。
val resolvedVersionName: String = providers.gradleProperty("VERSION_NAME").getOrElse("0.0.1")
val resolvedVersionCode: Int = providers.gradleProperty("VERSION_CODE").getOrElse("1").toInt()

android {
    namespace = "com.errorbook.app"
    // compileSdk 必须跟随依赖要求：Compose BOM 2025.01 / Room 2.7 / CameraX 1.4 均要求 35。
    compileSdk = 35

    defaultConfig {
        applicationId = "com.errorbook.app"
        // HarmonyOS 4.x 的 AOSP 兼容层为 API 31；targetSdk 高于设备 API 依然可正常安装运行，
        // 被 targetSdk gate 的行为变更在该设备上不会触发，因此这里跟随 compileSdk。
        minSdk = 26
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "com.errorbook.app.HiltTestRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // 不显式指定 v1/v2：minSdk 26 下 AGP 会自动选用 v2 + v3 签名方案，
                // v1（JAR 签名）从 Android 7.0 起已无必要。
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/INDEX.LIST",
            )
        }
    }

    lint {
        warningsAsErrors = false
        abortOnError = true
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "ObsoleteLintCustomCheck")
    }
}

ksp {
    // Room schema 导出目录，必须提交进版本库以便后续写 Migration。
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
    arg("dagger.fastInit", "enabled")
}

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.kts")
        targetExclude("**/build/**")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.exifinterface)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.coil.compose)
    // 内置（bundled）中文识别模型：首次使用即离线，不依赖 Play 服务下载模型。
    implementation(libs.mlkit.text.recognition.chinese)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
}
