package com.lenterosa.app

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.lenterosa.app.account.AccountRoute
import com.lenterosa.app.account.AccountViewModel
import com.lenterosa.app.account.GoogleIdTokenProvider
import com.lenterosa.app.ui.theme.LenteRosaTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Por ahora la app abre la cuenta (RF-02); la navegación completa llega con el onboarding (RF-11) y el mapa (RF-09).
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: AccountViewModel = hiltViewModel()
                    AccountRoute(
                        viewModel = viewModel,
                        requestGoogleIdToken = { googleIdTokens.requestIdToken(this@MainActivity) },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
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
