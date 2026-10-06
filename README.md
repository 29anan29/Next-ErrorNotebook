# 错因本 V3

以**错因管理**为核心的学习工具：拍照 OCR 录题，不要求输入正解，只记录「我为什么错」；考前按出错次数从高到低复习错因，而不是重新看答案。

- 产品需求：[docs/PRD.md](docs/PRD.md)
- 技术设计：[docs/技术文档.md](docs/技术文档.md)
- 设计令牌来源：[DESIGN-notion.md](DESIGN-notion.md)

## 运行前提

| 项 | 版本 | 说明 |
|---|---|---|
| JDK | 17 | AGP 8.7 要求 17+ |
| Gradle | 8.11.1 | 由 `./gradlew` 自动获取 |
| Android SDK | platform 35 + build-tools 35.0.0 | `local.properties` 里配 `sdk.dir` |
| 目标设备 | HarmonyOS 4.x（AOSP 兼容层 = Android 12 / API 31） | NEXT / 5.0+ 已移除 AOSP 层，装不了 APK |

```bash
./gradlew :app:assembleDebug        # 调试包
./gradlew :app:assembleRelease      # 发布包（需配置签名，见下）
./gradlew :app:testDebugUnitTest    # JVM 单元测试（含 Robolectric）
./gradlew :app:lintDebug            # Android Lint
./gradlew :app:spotlessCheck        # ktlint 格式检查
```

## 签名

`app/build.gradle.kts` 从 Gradle 属性或同名环境变量读取签名信息，**任一项缺失时跳过 release 签名配置**（而不是让配置阶段直接失败）：

```bash
./gradlew :app:assembleRelease \
  -PERRORBOOK_KEYSTORE=keystore/errorbook.jks \
  -PERRORBOOK_KEYSTORE_PASSWORD=... \
  -PERRORBOOK_KEY_ALIAS=... \
  -PERRORBOOK_KEY_PASSWORD=...
```

## 关键实现说明

这些都是设计/技术文档里写错或写漏、本项目实际做了修正的地方，改动前请先读这一节。

### 1. `compileSdk` / `targetSdk` 是 35，不是文档里的 31

Compose BOM 2025.01、Room 2.7、CameraX 1.4、Lifecycle 2.8.7 全部要求 `compileSdk ≥ 34~35`，文档 §11.1 的 `compileSdk = 31` 直接编译失败。
`targetSdk` 跟随 `compileSdk`：HarmonyOS 4.x 设备是 API 31，装一个 targetSdk 35 的 APK 完全合法，且被 targetSdk gate 的行为变更在该设备上不会触发。

Kotlin 2.0 下 Compose 编译器改用 `org.jetbrains.kotlin.plugin.compose` 插件，删掉了文档里的 `composeOptions`/`kotlinCompilerExtensionVersion`。

### 2. OCR 用内置模型，且 APK 里没有网络权限

文档 §2.1/§5.1 写的 `com.google.android.gms:play-services-mlkit-text-recognition` 是**动态下载模型的瘦客户端**，首次识别要联网——与「不联网、数据不出设备」直接冲突。

改用内置模型：

```kotlin
implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
```

同时在 `AndroidManifest.xml` 里显式 `tools:node="remove"` 剔掉 `INTERNET` / `ACCESS_NETWORK_STATE` / `ACCESS_WIFI_STATE`。这不是装饰——ML Kit 会拖入 `datatransport`、`firebase-components` 等自带网络权限的依赖，剔权后可用 `aapt2 dump permissions` 断言产物只有 `CAMERA`。代价是包体：内置中文模型约 2.5MB，release 包 44MB。

### 3. 错因频次是**两个**指标，不是一个

PRD §5.3.1 定义「每关联一次错因，计数 +1」，但文档 §4.2 的排行榜 SQL 用 `COUNT(DISTINCT qr.questionId)`。复习时点「又错」只会再插一行相同 `(questionId, reasonId)` 的关联记录，DISTINCT 会把它吃掉，**计数永远不涨**——两处规格互相矛盾。

`ReasonWithCount` 因此同时承载两个数字，正好也满足 PRD §5.4.1「显示出错次数、关联题目数」：

| 字段 | SQL | 含义 |
|---|---|---|
| `wrongCount` | `COUNT(qr.id)` | 出错次数，「又错」会 +1 |
| `questionCount` | `COUNT(DISTINCT qr.questionId)` | 关联的不同题目数 |

`question_reasons` 刻意**不加** `(questionId, reasonId)` 唯一约束，否则「又错」插不进第二行。`RecordReviewUseCase` 在一个事务里写 review_log 并按需补插关联行。

科目与时间筛选用 `:subjectId IS NULL OR ...` / `:since IS NULL OR ...` 两个可空参数合并进同一条查询——文档示例里 `flatMapLatest` 的时间分支会把 `subjectId` 筛选丢掉，两个筛选无法组合。

### 4. 修正的文档缺陷

| 位置 | 问题 | 修法 |
|---|---|---|
| `PdfExporter` | `PageInfo` 页码恒为 1，多页 PDF 结构损坏；标题画完才判断是否溢出页底 | 页码自增；绘制前先 `ensure(space)` |
| `ImageCompressor` | 质量递减到下限仍超标就带着超标文件退出，从不降采样兜底 | 质量阶梯 + 继续降采样的二级循环 |
| 拍照 | `BitmapFactory` 忽略 EXIF orientation，竖版题图躺倒 | `ExifInterface` 读方向后旋转 |
| 预置错因 | 文档 §4.3 列 8 条，PRD 附录 14 条；Hilt 示例里 `onCreate` 是空注释 | 取 14 条，用 `execSQL` 批量插入 |
| `review_logs` | 文档没给外键和索引 | 补 `CASCADE` 外键 + `questionReasonId`/`reviewedAt` 索引 |
| 签名 | `System.getenv` 为 null 时 `signingConfigs.create("release")` 直接让配置阶段失败 | 存在性判断，缺失则跳过签名配置 |
| `mastered` 字段 | 文档没说谁写它 | 保留但暂不驱动，由 `review_logs` 承担状态 |

### 5. 依赖补全

文档遗漏但必需的：`androidx.hilt:hilt-navigation-compose`（`hiltViewModel()`）、`lifecycle-runtime-compose`（`collectAsStateWithLifecycle()`）、`activity-compose`、Coil 3、DataStore、`buildConfig = true`、`room.schemaLocation`（否则 `exportSchema = true` 构建报错）。注解处理器统一用 KSP（Room 2.7 + Hilt 2.56 均支持），不用 kapt。

## 网络与镜像

`services.gradle.org` 与 `repo1.maven.org` 在部分网络环境直连超时，`settings.gradle.kts` 与 `gradle-wrapper.properties` 里改指腾讯镜像。换到境外网络时把 `distributionUrl` 改回 `services.gradle.org/distributions/gradle-8.11.1-bin.zip` 即可。

## 架构

单模块三层（Presentation / Domain / Data）+ Platform Services，包结构见 `docs/技术文档.md` §3.2。

```
data/local/{entity,dao}   5 张表 + 4 个 DAO + 预置错因种子
data/repository           Question / Reason / Subject / Export
domain/usecase            GetReasonRanking / SaveQuestion / RecognizeText /
                          RecordReview / ExportReasonList
domain/util               ImageCompressor（EXIF+二级降负）/ FileNameGenerator
platform/{ocr,camera,file} OcrService / ImageStore / FileSaveManager / PdfExporter
ui/{theme,navigation,components,home,capture,reasondetail,...}
di                        DatabaseModule
```

UI 令牌全部来自 `DESIGN-notion.md`，翻译成 `ui/theme/{Color,Type,Shape,Spacing,Elevation}.kt`：
暖白画布 `#f6f5f4`、近黑正文、单一结构性蓝 `#0075de` 只用于动作，贴纸色板只做分类圆点。
字阶按移动端重新缩放（文档里的 64px Display 在手机上没有意义）但保留原负字距比例。

## 测试

26 个用例，`./gradlew :app:testDebugUnitTest` 全绿：

- `ReasonDaoRankingTest` — 锁死上面第 3 条的核心语义（「又错」计数递增、双指标独立、排序、归档过滤、科目×时间组合筛选、级联删除、合并迁移）
- `ReviewFeedbackTest` — 三种反馈各自的计数影响与 review_log 落库
- `PresetAndReviewTest` — 预置 14 条错因真的落库、分类与颜色齐全
- `ReviewPaneUiTest` — PRD §13 验收项「复习时默认不显示错因，点击显示后才展示」
- `ImageCompressorTest` — 用高熵噪声图逼出降采样兜底分支，断言产物 ≤ 500KB

HarmonyOS 4.x 没有模拟器，相机 / OCR / SAF 三条链路本地只能保证编译与单测覆盖，端到端需真机验证。
