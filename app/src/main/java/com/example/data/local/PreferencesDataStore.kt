package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ThemeMode
import com.example.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manoj_sonali_prefs")

class PreferencesDataStore(
    private val appContext: Context,
    private val keystoreManager: KeystoreManager
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val APP_THEME_STYLE = stringPreferencesKey("app_theme_style")
        val ENCRYPTED_PHONE = stringPreferencesKey("encrypted_phone")
        val LAST_NOTIFIED_MSG_MS = longPreferencesKey("last_notified_msg_ms")
    }

    val themeModeFlow: Flow<ThemeMode> = appContext.dataStore.data.map { prefs ->
        ThemeMode.fromRaw(prefs[Keys.THEME_MODE])
    }

    val appThemeStyleFlow: Flow<AppThemeStyle> = appContext.dataStore.data.map { prefs ->
        AppThemeStyle.fromRaw(prefs[Keys.APP_THEME_STYLE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }

    suspend fun setAppThemeStyle(style: AppThemeStyle) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME_STYLE] = style.name
        }
    }

    suspend fun saveEncryptedPhone(phoneE164: String) {
        val encrypted = keystoreManager.encrypt(phoneE164)
        appContext.dataStore.edit { prefs ->
            prefs[Keys.ENCRYPTED_PHONE] = encrypted
        }
    }

    val decryptedPhoneFlow: Flow<String?> = appContext.dataStore.data.map { prefs ->
        prefs[Keys.ENCRYPTED_PHONE]?.let { keystoreManager.decrypt(it) }
    }

    suspend fun getLastNotifiedMessageMs(): Long {
        return appContext.dataStore.data.first()[Keys.LAST_NOTIFIED_MSG_MS] ?: 0L
    }

    suspend fun updateLastNotifiedMessageMs(timestampMs: Long) {
        appContext.dataStore.edit { prefs ->
            val current = prefs[Keys.LAST_NOTIFIED_MSG_MS] ?: 0L
            if (timestampMs > current) {
                prefs[Keys.LAST_NOTIFIED_MSG_MS] = timestampMs
            }
        }
    }

    suspend fun clearSensitivePrefs() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(Keys.ENCRYPTED_PHONE)
            prefs.remove(Keys.LAST_NOTIFIED_MSG_MS)
        }
    }
}
