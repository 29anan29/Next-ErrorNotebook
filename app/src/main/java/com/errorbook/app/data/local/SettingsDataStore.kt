package com.errorbook.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 应用级设置：默认导出目录、是否看过 HarmonyOS 安装引导、是否需要拍照 OCR 引导。 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private companion object {
        private val Context.dataStore: DataStore<androidx.datastore.preferences.core.Preferences> by preferencesDataStore("settings")

        val KEY_DEFAULT_EXPORT_DIR = stringPreferencesKey("default_export_dir_uri")
        val KEY_SEEN_HARMONYOS_GUIDE = booleanPreferencesKey("seen_harmonyos_guide")
    }

    val defaultExportDir: Flow<String?> = context.dataStore.data.map { it[KEY_DEFAULT_EXPORT_DIR] }

    val seenHarmonyOSGuide: Flow<Boolean> = context.dataStore.data.map { it[KEY_SEEN_HARMONYOS_GUIDE] ?: false }

    suspend fun setDefaultExportDir(uri: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) prefs.remove(KEY_DEFAULT_EXPORT_DIR) else prefs[KEY_DEFAULT_EXPORT_DIR] = uri
        }
    }

    suspend fun setHarmonyOSGuideSeen(seen: Boolean = true) {
        context.dataStore.edit { prefs -> prefs[KEY_SEEN_HARMONYOS_GUIDE] = seen }
    }
}