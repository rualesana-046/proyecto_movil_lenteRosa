package com.lenterosa.app.places

import com.lenterosa.app.navigation.BackStackCodec
import com.lenterosa.app.navigation.Destination
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaceFormatTest {

    @Test
    fun `la distancia se muestra en metros o en kilómetros con coma decimal`() {
        assertEquals("350 m", PlaceFormat.distance(350))
        assertEquals("1,2 km", PlaceFormat.distance(1_240))
    }

    @Test
    fun `un promedio nulo no se convierte en cero`() {
        assertEquals("4,2", PlaceFormat.average(4.24))
        assertNull(PlaceFormat.average(null))
    }

    @Test
    fun `la fecha se muestra en español y una fecha inválida no rompe la pantalla`() {
        assertEquals("1 nov 2026", PlaceFormat.date("2026-11-01T12:00:00Z", ZoneId.of("America/Bogota"))?.replace(".", ""))
        assertNull(PlaceFormat.date("ayer"))
        assertNull(PlaceFormat.date(null))
    }

    @Test
    fun `la espera se redondea hacia arriba a días, horas o minutos`() {
        assertEquals(WaitTime(5, WaitUnit.DAYS), PlaceFormat.waitTime(4 * 86_400L + 1))
        assertEquals(WaitTime(14, WaitUnit.HOURS), PlaceFormat.waitTime(13 * 3_600L + 60))
        assertEquals(WaitTime(1, WaitUnit.MINUTES), PlaceFormat.waitTime(5))
    }

    @Test
    fun `cada categoría tiene su texto y una desconocida se muestra como Otro`() {
        val known = listOf("PARK", "STREET", "TRANSPORT", "COMMERCE", "EDUCATION", "HEALTH", "NIGHTLIFE", "OTHER")
        assertEquals(known.size, known.map(PlaceFormat::category).toSet().size)
        assertEquals(PlaceFormat.category("OTHER"), PlaceFormat.category("MUSEO"))
    }

    @Test
    fun `la pila de pantallas se guarda y se restaura, incluso con nombres con separadores`() {
        val stack = listOf(Destination.Places, Destination.Privacy, Destination.PlaceDetail("p1"), Destination.Contribute("p1", "Parque · Norte | 2"))

        assertEquals(stack, BackStackCodec.decode(BackStackCodec.encode(stack)))
        assertEquals(listOf(Destination.Places), BackStackCodec.decode(listOf("desconocida")))
    }

    @Test
    fun `la lista busca alrededor del centro del piloto al abrirse`() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val api = FakePlacesApi()
            val viewModel = PlacesViewModel(repository(api))

            assertEquals(LoadState.Ready(api.places), viewModel.state.value.places)

            viewModel.onQueryChange("parque")
            viewModel.search()

            assertEquals(listOf(null, "parque"), api.nearbyQueries)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
