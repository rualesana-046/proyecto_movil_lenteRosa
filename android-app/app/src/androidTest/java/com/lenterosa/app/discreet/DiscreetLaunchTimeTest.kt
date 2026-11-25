package com.lenterosa.app.discreet

import android.app.PendingIntent
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Métrica de la Fase 1: activar el Modo discreto en 5 segundos o menos (RF-08, ADR 0013).
 *
 * Mide la parte que depende de la app: desde que el sistema recibe la intención del tile (el mismo
 * PendingIntent que usa DiscreetTileService) o del atajo del ícono, hasta que las tres acciones
 * están en pantalla y se pueden tocar. El resto de los 5 segundos queda para el gesto de la usuaria
 * (bajar los ajustes rápidos o mantener presionado el ícono, y tocar). Se corre en el emulador o un
 * teléfono: ./gradlew connectedDebugAndroidTest
 */
@RunWith(AndroidJUnit4::class)
class DiscreetLaunchTimeTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun desdeElTileLasAccionesEstanListasDentroDelPresupuesto() {
        val elapsed = measure {
            PendingIntent.getActivity(context, 0, DiscreetActivity.intent(context), PendingIntent.FLAG_IMMUTABLE).send()
        }
        Log.i(TAG, "Tile: $elapsed ms")
        assertTrue("Desde el tile tardó $elapsed ms (presupuesto de la app: $APP_BUDGET_MS ms)", elapsed <= APP_BUDGET_MS)
    }

    @Test
    fun desdeElAtajoDelIconoLasAccionesEstanListasDentroDelPresupuesto() {
        // La misma intención que declara res/xml/shortcuts.xml.
        val shortcut = Intent(Intent.ACTION_VIEW)
            .setClassName(context.packageName, DiscreetActivity::class.java.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val elapsed = measure { context.startActivity(shortcut) }
        Log.i(TAG, "Atajo: $elapsed ms")
        assertTrue("Desde el atajo tardó $elapsed ms (presupuesto de la app: $APP_BUDGET_MS ms)", elapsed <= APP_BUDGET_MS)
    }

    private fun measure(launch: () -> Unit): Long {
        val start = SystemClock.elapsedRealtime()
        launch()
        compose.waitUntil(timeoutMillis = 5_000) {
            compose.onAllNodesWithTag(DiscreetTags.FAKE_CALL).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(DiscreetTags.SEND).fetchSemanticsNodes().isNotEmpty() &&
                compose.onAllNodesWithTag(DiscreetTags.MARK).fetchSemanticsNodes().isNotEmpty()
        }
        return SystemClock.elapsedRealtime() - start
    }

    private companion object {
        const val TAG = "DiscreetLaunch"

        /** De los 5 s de la métrica, 2 s son para la app; 3 s quedan para el gesto y el toque. */
        const val APP_BUDGET_MS = 2_000L
    }
}
