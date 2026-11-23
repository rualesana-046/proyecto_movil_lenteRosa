package com.lenterosa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}
