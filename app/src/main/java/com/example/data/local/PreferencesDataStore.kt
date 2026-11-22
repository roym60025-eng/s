package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.ThemeMode
import com.example.security.KeystoreManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "manoj_sonali_prefs")

class PreferencesDataStore(
    private val appContext: Context,
    private val keystoreManager: KeystoreManager
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ENCRYPTED_PHONE = stringPreferencesKey("encrypted_phone")
    }

    val themeModeFlow: Flow<ThemeMode> = appContext.dataStore.data.map { prefs ->
        ThemeMode.fromRaw(prefs[Keys.THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        appContext.dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
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

    suspend fun clearSensitivePrefs() {
        appContext.dataStore.edit { prefs ->
            prefs.remove(Keys.ENCRYPTED_PHONE)
        }
    }
}
