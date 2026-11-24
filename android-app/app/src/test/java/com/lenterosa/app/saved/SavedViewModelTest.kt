package com.lenterosa.app.saved

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SavedViewModelTest {

    private val api = FakeSavedApi()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `al abrir la pestaña los avisos se muestran como nuevos y quedan leídos en la API`() {
        api.saved += "p1"
        api.alerts = AlertInbox(1, listOf(alert()))
        val viewModel = SavedViewModel(store(api))

        viewModel.load()

        val shown = (viewModel.state.value as SavedLoad.Ready).overview.inbox.items.single()
        assertFalse("En pantalla sigue marcado como nuevo esta vez", shown.read)
        assertEquals(1, api.markedRead)
    }

    @Test
    fun `quitar un lugar desde la pestaña lo saca de la lista`() {
        api.saved += "p1"
        val viewModel = SavedViewModel(store(api))
        viewModel.load()

        viewModel.remove("p1")

        assertEquals(emptyList<String>(), (viewModel.state.value as SavedLoad.Ready).overview.places.map { it.id })
    }
}
