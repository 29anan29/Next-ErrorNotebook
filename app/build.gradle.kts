plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.spotless)
}

/**
 * release 签名所需的三个环境变量。未配置时不做 release 签名配置，
 * 而不是像直接读取 System.getenv 那样让配置阶段直接失败。
 */
val keystorePath: String? = providers.gradleProperty("ERRORBOOK_KEYSTORE").orNull
    ?: System.getenv("ERRORBOOK_KEYSTORE")
val keystorePassword: String? = providers.gradleProperty("ERRORBOOK_KEYSTORE_PASSWORD").orNull
    ?: System.getenv("ERRORBOOK_KEYSTORE_PASSWORD")
val keyAlias: String? = providers.gradleProperty("ERRORBOOK_KEY_ALIAS").orNull
    ?: System.getenv("ERRORBOOK_KEY_ALIAS")
val keyPassword: String? = providers.gradleProperty("ERRORBOOK_KEY_PASSWORD").orNull
    ?: System.getenv("ERRORBOOK_KEY_PASSWORD")

val hasReleaseSigning = listOf(keystorePath, keystorePassword, keyAlias, keyPassword)
    .all { !it.isNullOrBlank() }

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
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "com.errorbook.app.HiltTestRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystorePath!!)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
                enableV1Signing = true
                enableV2Signing = true
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
