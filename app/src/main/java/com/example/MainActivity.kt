package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.domain.model.ThemeMode
import com.example.navigation.AppNavigation
import com.example.ui.theme.ManojSonaliTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as ManojSonaliApp).container
        container.notificationMonitor.onNotificationIntent(intent)

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    container.notificationMonitor.setAppForeground(true)
                    lifecycleScope.launch { container.authAndUserRepository.updatePresence(true) }
                }
                Lifecycle.Event.ON_STOP -> {
                    container.notificationMonitor.setAppForeground(false)
                    lifecycleScope.launch { container.authAndUserRepository.updatePresence(false) }
                }
                else -> Unit
            }
        })

        setContent {
            val themeMode by container.preferencesDataStore.themeModeFlow
                .collectAsStateWithLifecycle(initialValue = ThemeMode.SYSTEM)
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            ManojSonaliTheme(darkTheme = isDark) {
                AppNavigation(container = container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val container = (application as ManojSonaliApp).container
        container.notificationMonitor.onNotificationIntent(intent)
    }
}
