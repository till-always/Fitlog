package com.fitlog.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("fitlog_settings")

data class UserProfile(
    val name: String = "Till",
    val signature: String = "自律给我自由",
    val gender: String = "男",
    val heightCm: Int = 178,
    val weightKg: Float = 72.5f
)

class SettingsStore(private val context: Context) {
    private object K {
        val dark = booleanPreferencesKey("dark_mode")
        val rest = intPreferencesKey("default_rest")
        val vibrate = booleanPreferencesKey("vibrate")
        val sound = booleanPreferencesKey("sound")
        val name = stringPreferencesKey("profile_name")
        val sig = stringPreferencesKey("profile_sig")
        val gender = stringPreferencesKey("profile_gender")
        val height = intPreferencesKey("profile_height")
        val weight = floatPreferencesKey("profile_weight")
        val deletedDataset = stringSetPreferencesKey("deleted_dataset_exercises")
    }

    val darkMode: Flow<Boolean> = context.dataStore.data.map { it[K.dark] ?: false }
    val defaultRest: Flow<Int> = context.dataStore.data.map { it[K.rest] ?: 60 }
    val vibrate: Flow<Boolean> = context.dataStore.data.map { it[K.vibrate] ?: true }
    val sound: Flow<Boolean> = context.dataStore.data.map { it[K.sound] ?: true }
    val profile: Flow<UserProfile> = context.dataStore.data.map {
        UserProfile(
            name = it[K.name] ?: "Till",
            signature = it[K.sig] ?: "自律给我自由",
            gender = it[K.gender] ?: "男",
            heightCm = it[K.height] ?: 178,
            weightKg = it[K.weight] ?: 72.5f
        )
    }

    suspend fun setDarkMode(v: Boolean) = context.dataStore.edit { it[K.dark] = v }
    suspend fun setDefaultRest(v: Int) = context.dataStore.edit { it[K.rest] = v }
    suspend fun setVibrate(v: Boolean) = context.dataStore.edit { it[K.vibrate] = v }
    suspend fun setSound(v: Boolean) = context.dataStore.edit { it[K.sound] = v }
    suspend fun saveProfile(p: UserProfile) = context.dataStore.edit {
        it[K.name] = p.name
        it[K.sig] = p.signature
        it[K.gender] = p.gender
        it[K.height] = p.heightCm
        it[K.weight] = p.weightKg
    }

    /** 用户手动删除的数据集动作名：导入时跳过，避免删除后随数据集复活 */
    suspend fun deletedDatasetExercises(): Set<String> =
        context.dataStore.data.first()[K.deletedDataset] ?: emptySet()

    suspend fun addDeletedDatasetExercise(name: String) = context.dataStore.edit {
        it[K.deletedDataset] = (it[K.deletedDataset] ?: emptySet()) + name
    }
}
