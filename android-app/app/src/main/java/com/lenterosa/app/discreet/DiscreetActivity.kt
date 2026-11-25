package com.lenterosa.app.discreet

import android.Manifest
import android.app.ActivityManager
import android.app.KeyguardManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.lenterosa.app.R
import com.lenterosa.app.ui.glass.ProvideVisualPreferences
import com.lenterosa.app.ui.glass.VisualPreferences
import com.lenterosa.app.ui.motion.Motion
import com.lenterosa.app.ui.theme.LenteRosaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Modo discreto (RF-08, ADR 0013). Actividad aparte con nombre neutro ("Atajo"): no muestra el
 * nombre ni el logo de Lente Rosa, no aparece en Recientes, no permite capturas y se puede abrir
 * sobre la pantalla bloqueada. Enviar la ubicación pide desbloquear antes de abrir los mensajes.
 */
@AndroidEntryPoint
class DiscreetActivity : ComponentActivity() {

    private val viewModel: DiscreetViewModel by viewModels()

    private val requestLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        // Con permiso preciso, aproximado o ninguno: se sigue igual y el estado dice qué se pudo hacer.
        sendHelpMessage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // Mientras está abierta, en Recientes se ve como "Atajo" y con la miniatura en negro.
        val neutral = getString(R.string.discreet_label)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setTaskDescription(ActivityManager.TaskDescription.Builder().setLabel(neutral).build())
        } else {
            @Suppress("DEPRECATION")
            setTaskDescription(ActivityManager.TaskDescription(neutral))
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        enableEdgeToEdge()
        val reducedMotion = Motion.systemReducedMotion(this)
        setContent {
            val dark = isSystemInDarkTheme()
            val colors = if (dark) DiscreetColors.Dark else DiscreetColors.Light
            LenteRosaTheme(darkTheme = dark) {
                // Nunca liquid glass en el Modo discreto (ADR 0010).
                ProvideVisualPreferences(VisualPreferences(reducedMotion = reducedMotion, discreetMode = true)) {
                    val state by viewModel.state.collectAsStateWithLifecycle()
                    var showSettings by rememberSaveable { mutableStateOf(false) }
                    BackHandler(enabled = showSettings) { showSettings = false }
                    when {
                        state.call is FakeCall.Ringing || state.call is FakeCall.InCall -> {
                            Ringer(state.call is FakeCall.Ringing)
                            FakeCallScreen(state.call, onAnswer = viewModel::answer, onHangUp = viewModel::hangUp)
                        }
                        showSettings -> DiscreetSettingsScreen(
                            settings = state.settings,
                            colors = colors,
                            onSaveFakeCall = { name, delay -> viewModel.saveFakeCall(name, delay) },
                            onAddContact = { name, phone -> viewModel.addContact(name, phone) },
                            onRemoveContact = viewModel::removeContact,
                            onBack = { showSettings = false },
                        )
                        else -> DiscreetScreen(
                            state = state,
                            colors = colors,
                            onSendLocation = ::onSendLocation,
                            onFakeCall = viewModel::startFakeCall,
                            onMark = { viewModel.markUncomfortable() },
                            onUndo = viewModel::undo,
                            onDismissStatus = viewModel::dismissSendStatus,
                            onOpenSettings = { showSettings = true },
                            onCallEmergency = ::dialEmergency,
                            onExit = ::exitToHome,
                        )
                    }
                }
            }
        }
    }

    /** Un toque: pide la ubicación solo la primera vez; luego abre directo los mensajes. */
    private fun onSendLocation() {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = granted(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!fine && !coarse && !askedForLocation) {
            askedForLocation = true
            requestLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            sendHelpMessage()
        }
    }

    private var askedForLocation = false

    private fun sendHelpMessage() {
        lifecycleScope.launch {
            val message = viewModel.prepareHelpMessage()
            withUnlockedDevice { openMessaging(message) }
        }
    }

    private fun openMessaging(message: HelpMessage) {
        val intent = message.contact?.let {
            Intent(Intent.ACTION_SENDTO, ("smsto:" + Uri.encode(it.phone)).toUri()).putExtra("sms_body", message.text)
        } ?: Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, message.text), null)
        try {
            startActivity(intent)
            viewModel.reportSendStatus(message.status)
        } catch (_: ActivityNotFoundException) {
            viewModel.reportSendStatus(SendStatus.NO_MESSAGING_APP)
        }
    }

    /** Sobre la pantalla bloqueada, pide desbloquear antes de abrir otra app. */
    private fun withUnlockedDevice(action: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (keyguard == null || !keyguard.isKeyguardLocked) {
            action()
            return
        }
        keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
            override fun onDismissSucceeded() = action()
        })
    }

    /** Abre el marcador con el 123 escrito; no llama sola. */
    private fun dialEmergency() {
        withUnlockedDevice {
            try {
                startActivity(Intent(Intent.ACTION_DIAL, "tel:123".toUri()))
            } catch (_: ActivityNotFoundException) {
                viewModel.reportSendStatus(SendStatus.NO_MESSAGING_APP)
            }
        }
    }

    /** Sale a la pantalla de inicio del teléfono sin dejar rastro en Recientes. */
    private fun exitToHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finishAndRemoveTask()
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    companion object {
        /** Intención que usan el atajo del ícono, el tile y el gesto dentro de la app. */
        fun intent(context: Context): Intent = Intent(context, DiscreetActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    }
}

/** Tono y vibración mientras suena la llamada simulada; se detienen al contestar o colgar. */
@androidx.compose.runtime.Composable
private fun Ringer(ringing: Boolean) {
    val context = androidx.compose.ui.platform.LocalContext.current
    DisposableEffect(ringing) {
        var ringtone: Ringtone? = null
        val vibrator = vibrator(context)
        if (ringing) {
            ringtone = RingtoneManager.getRingtone(context, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))?.apply {
                audioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build()
                runCatching { play() }
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 600), 0))
        }
        onDispose {
            ringtone?.stop()
            vibrator?.cancel()
        }
    }
}

private fun vibrator(context: Context): Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    context.getSystemService(VibratorManager::class.java)?.defaultVibrator
} else {
    @Suppress("DEPRECATION")
    context.getSystemService(Vibrator::class.java)
}
