package com.lenterosa.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.autoSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenterosa.app.account.GoogleIdTokenProvider
import com.lenterosa.app.navigation.AppNavigation
import com.lenterosa.app.navigation.startDestination
import com.lenterosa.app.onboarding.LaunchViewModel
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.motion.Motion
import com.lenterosa.app.ui.theme.LenteRosaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Abre la bienvenida al primer uso (RF-11) y después el inicio con el mapa (RF-09) y la lista (RF-01).
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var googleIdTokens: GoogleIdTokenProvider

    private var visualPreferences by mutableStateOf(VisualPreferences())

    private val requestLocalNetwork = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkAccessIfNeeded()
        enableEdgeToEdge()
        setContent {
            LenteRosaTheme {
                ProvideVisualPreferences(visualPreferences) {
                    val launch: LaunchViewModel = hiltViewModel()
                    val onboardingCompleted by launch.onboardingCompleted.collectAsStateWithLifecycle()
                    // La primera pantalla se decide una sola vez: al terminar la bienvenida, la navegación
                    // ya sabe ir al inicio y el grafo no debe rehacerse.
                    var start by rememberSaveable(stateSaver = autoSaver()) { mutableStateOf<Boolean?>(null) }
                    if (start == null) start = onboardingCompleted
                    // Mientras se lee la marca guardada (un instante) se muestra solo el fondo del tema.
                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                        start?.let { completed ->
                            AppNavigation(
                                startDestination = startDestination(completed),
                                requestGoogleIdToken = { googleIdTokens.requestIdToken(this@MainActivity) },
                            )
                        }
                    }
                }
            }
        }
    }

    // Se vuelven a leer al volver a la app, por si la usuaria cambió los ajustes de accesibilidad.
    override fun onResume() {
        super.onResume()
        visualPreferences = VisualPreferences(
            reducedMotion = Motion.systemReducedMotion(this),
            highContrast = Motion.systemHighContrastText(this),
        )
    }

    // Solo en depuración: la API local corre en el equipo, que para Android 17 (API 37) es red local y
    // exige ACCESS_LOCAL_NETWORK. La versión publicada usa la API pública por HTTPS y no lo declara.
    private fun requestLocalNetworkAccessIfNeeded() {
        if (!BuildConfig.DEBUG || Build.VERSION.SDK_INT < LOCAL_NETWORK_PERMISSION_SDK) {
            return
        }
        if (checkSelfPermission(LOCAL_NETWORK_PERMISSION) != PackageManager.PERMISSION_GRANTED) {
            requestLocalNetwork.launch(LOCAL_NETWORK_PERMISSION)
        }
    }

    private companion object {
        const val LOCAL_NETWORK_PERMISSION = "android.permission.ACCESS_LOCAL_NETWORK"
        const val LOCAL_NETWORK_PERMISSION_SDK = 37
    }
}
