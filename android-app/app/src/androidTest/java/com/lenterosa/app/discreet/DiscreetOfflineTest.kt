package com.lenterosa.app.discreet

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.location.Criteria
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * El Modo discreto funciona sin conexión (RF-08, RF-10): con el modo avión activado de verdad, la
 * llamada simulada suena, "Marcar incómodo" guarda la marca, y "Enviar mi ubicación" obtiene la
 * posición del GPS y abre la app de mensajes con el enlace al mapa.
 *
 * El GPS es un proveedor de prueba con una posición fija; el proveedor combinado (fused) y el de red
 * quedan desactivados para que la posición solo pueda venir del GPS. Al terminar se deja el
 * teléfono como estaba. Se corre en el emulador: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DiscreetOfflineTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val locations = context.getSystemService(LocationManager::class.java)
    private val testProviders = mutableListOf<String>()
    private val gpsFeeder = Executors.newSingleThreadScheduledExecutor()
    private var scenario: ActivityScenario<DiscreetActivity>? = null

    @Before
    fun setUp() {
        val pkg = context.packageName
        shell("pm grant $pkg android.permission.ACCESS_FINE_LOCATION")
        shell("pm grant $pkg android.permission.ACCESS_COARSE_LOCATION")
        shell("appops set $pkg android:mock_location allow")
        shell("cmd location set-location-enabled true")
        shell("cmd connectivity airplane-mode enable")
        waitUntil("el teléfono sigue con internet") { !hasInternet() }

        // Solo el GPS da posición: el combinado y el de red quedan apagados.
        addTestProvider(LocationManager.GPS_PROVIDER, enabled = true)
        addTestProvider(LocationManager.NETWORK_PROVIDER, enabled = false)
        if (LocationManager.FUSED_PROVIDER in locations.allProviders) {
            addTestProvider(LocationManager.FUSED_PROVIDER, enabled = false)
        }
        gpsFeeder.scheduleWithFixedDelay({ pushGpsFix() }, 0, 300, TimeUnit.MILLISECONDS)
        Intents.init()
        // La app de mensajes no se abre de verdad: se registra la intención y se responde al instante.
        Intents.intending(not(isInternal())).respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun tearDown() {
        scenario?.close()
        Intents.release()
        gpsFeeder.shutdownNow()
        testProviders.forEach { runCatching { locations.removeTestProvider(it) } }
        shell("appops set ${context.packageName} android:mock_location default")
        shell("cmd connectivity airplane-mode disable")
    }

    @Test
    fun sinConexionLaUbicacionDeAyudaVieneDelGps() = runBlocking {
        val fix = AndroidHelpLocation(context).current(DiscreetViewModel.SEND_LOCATION_TIMEOUT_MS)

        assertTrue("Sin internet no se obtuvo la ubicación: $fix", fix is HelpFix.Found)
        fix as HelpFix.Found
        assertEquals(LAT, fix.lat, 0.00001)
        assertEquals(LNG, fix.lng, 0.00001)
        assertTrue(fix.precise)
    }

    @Test
    fun sinConexionEnviarMiUbicacionAbreLosMensajesConElEnlaceAlMapa() {
        open()

        compose.onNodeWithTag(DiscreetTags.SEND).performClick()

        waitUntil("no se abrió la app de mensajes") { Intents.getIntents().any { messageText(it) != null } }
        val text = Intents.getIntents().firstNotNullOf { messageText(it) }
        assertTrue(text, text.contains("https://www.openstreetmap.org/?mlat=1.21360&mlon=-77.28110"))
        assertFalse("La ubicación del GPS no debe salir como aproximada: $text", text.contains("aproximada"))
        assertFalse(hasInternet())
        // Hay una app de mensajes que puede recibir el texto aunque no haya datos móviles. Se consulta
        // por el shell: la app no declara <queries> porque no necesita ver qué otras apps hay.
        val messaging = shell("cmd package query-activities --brief -a android.intent.action.SENDTO -d smsto:3000000000")
        assertFalse("No hay app de mensajes en el teléfono", messaging.contains("No activities"))
    }

    @Test
    fun sinConexionLaLlamadaSimuladaSuena() {
        open()

        compose.onNodeWithTag(DiscreetTags.FAKE_CALL).performClick()

        val timeout = (DiscreetSettings.DEFAULT_DELAY_SECONDS + 5) * 1_000L
        compose.waitUntil(timeoutMillis = timeout) {
            compose.onAllNodesWithText(DiscreetSettings.DEFAULT_CALLER).fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(hasInternet())
    }

    @Test
    fun sinConexionMarcarIncomodoGuardaLaMarca() {
        open()

        compose.onNodeWithTag(DiscreetTags.MARK).performClick()

        // "Deshacer" solo aparece cuando la marca ya quedó guardada en el teléfono.
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(DiscreetTags.UNDO).fetchSemanticsNodes().isNotEmpty()
        }
        assertFalse(hasInternet())
    }

    private fun open() {
        scenario = ActivityScenario.launch(DiscreetActivity.intent(context))
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(DiscreetTags.SEND).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** Texto del mensaje de ayuda, venga por el selector para compartir o directo a un contacto. */
    private fun messageText(intent: Intent): String? = when (intent.action) {
        Intent.ACTION_CHOOSER -> @Suppress("DEPRECATION") (intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT))
            ?.getStringExtra(Intent.EXTRA_TEXT)
        Intent.ACTION_SENDTO -> intent.getStringExtra("sms_body")
        else -> null
    }

    @Suppress("DEPRECATION") // La firma con Criteria funciona en todas las versiones que admite la app.
    private fun addTestProvider(name: String, enabled: Boolean) {
        runCatching { locations.removeTestProvider(name) }
        locations.addTestProvider(
            name, false, name == LocationManager.GPS_PROVIDER, false, false, true, true, true,
            Criteria.POWER_LOW, Criteria.ACCURACY_FINE,
        )
        locations.setTestProviderEnabled(name, enabled)
        testProviders += name
    }

    private fun pushGpsFix() {
        val fix = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = LAT
            longitude = LNG
            accuracy = 5f
            time = System.currentTimeMillis()
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        runCatching { locations.setTestProviderLocation(LocationManager.GPS_PROVIDER, fix) }
    }

    private fun hasInternet(): Boolean {
        val manager = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun waitUntil(message: String, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + 10_000
        while (!condition()) {
            if (SystemClock.elapsedRealtime() > deadline) throw AssertionError(message)
            SystemClock.sleep(200)
        }
    }

    private fun shell(command: String): String {
        val output = instrumentation.uiAutomation.executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(output).use { String(it.readBytes()) }
    }

    private companion object {
        // Centro de Pasto (dato ficticio para la prueba).
        const val LAT = 1.2136
        const val LNG = -77.2811
    }
}
