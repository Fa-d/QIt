package dev.sadakat.qit

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.sadakat.qit.core.domain.model.ThemeMode
import dev.sadakat.qit.presentation.AppViewModel
import dev.sadakat.qit.presentation.QuranApp
import dev.sadakat.qit.ui.theme.QItAppTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // Notifications show playback controls and download progress; the app works without them.
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val app by appViewModel.uiState.collectAsStateWithLifecycle()
            val darkTheme = when (app.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT, ThemeMode.SEPIA -> false
                ThemeMode.DARK -> true
            }
            // The in-app theme can differ from the system's, so the system bar icons follow it.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            // Until the settings are read the window's page color shows, so there's no theme flash.
            if (app.isReady) {
                QItAppTheme(
                    darkTheme = darkTheme,
                    dynamicColor = app.dynamicColor,
                    arabicScale = app.arabicTextSize.scale,
                ) {
                    QuranApp()
                }
            }
        }
    }
}
