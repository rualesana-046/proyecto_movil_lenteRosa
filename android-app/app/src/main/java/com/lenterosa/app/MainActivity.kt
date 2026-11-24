package com.lenterosa.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.lenterosa.app.account.GoogleIdTokenProvider
import com.lenterosa.app.navigation.AppNavigation
import com.lenterosa.app.ui.theme.LenteRosaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Abre la lista de lugares del piloto (RF-01); el mapa (RF-09) y el onboarding (RF-11) llegan después.
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var googleIdTokens: GoogleIdTokenProvider

    private val requestLocalNetwork = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestLocalNetworkAccessIfNeeded()
        enableEdgeToEdge()
        setContent {
            LenteRosaTheme {
                AppNavigation(requestGoogleIdToken = { googleIdTokens.requestIdToken(this@MainActivity) })
            }
        }
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
