package com.lenterosa.app.discreet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DiscreetViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val store = FakeDiscreetStore()
    private var fix: HelpFix = HelpFix.Found(1.21364, -77.28112, precise = true)

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): DiscreetViewModel = DiscreetViewModel(store) { fix }.also { runCurrent() }

    @Test
    fun `la llamada simulada usa el nombre y el tiempo configurados y suena al terminar la cuenta`() = runTest(dispatcher) {
        store.settingsFlow.value = DiscreetSettings(fakeCallName = "Laura", fakeCallDelaySeconds = 10)
        val vm = viewModel()

        vm.startFakeCall()
        runCurrent()
        assertEquals(FakeCall.Scheduled(10), vm.state.value.call)
        assertEquals(UndoOffer.Call(10), vm.state.value.undo)

        advanceTimeBy(10_001)
        assertEquals(FakeCall.Ringing("Laura"), vm.state.value.call)

        vm.answer()
        runCurrent()
        assertEquals(FakeCall.InCall("Laura", 0), vm.state.value.call)
        vm.hangUp()
        assertEquals(FakeCall.Idle, vm.state.value.call)
    }

    @Test
    fun `Deshacer dentro de 5 segundos cancela la llamada, y el aviso desaparece a los 5 segundos`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.startFakeCall()
        advanceTimeBy(3_000)
        vm.undo()
        advanceTimeBy(20_000)
        assertEquals(FakeCall.Idle, vm.state.value.call)
        assertNull(vm.state.value.undo)

        vm.startFakeCall()
        advanceTimeBy(DiscreetViewModel.UNDO_WINDOW_MS + 1)
        assertNull("El aviso con Deshacer dura 5 segundos", vm.state.value.undo)
    }

    @Test
    fun `marcar incómodo guarda en el teléfono con la ubicación redondeada, y Deshacer la borra`() = runTest(dispatcher) {
        val vm = viewModel()

        vm.markUncomfortable(nowMillis = 1_000L)
        runCurrent()

        val mark = store.marksFlow.value.single()
        assertEquals(1.214, mark.lat!!, 0.0)
        assertEquals(-77.281, mark.lng!!, 0.0)
        assertEquals(UndoOffer.Mark(mark.id, withLocation = true), vm.state.value.undo)

        vm.undo()
        runCurrent()
        assertTrue(store.marksFlow.value.isEmpty())
    }

    @Test
    fun `sin ubicación la marca se guarda igual, solo con la hora`() = runTest(dispatcher) {
        fix = HelpFix.Unavailable
        val vm = viewModel()

        vm.markUncomfortable()
        runCurrent()

        assertNull(store.marksFlow.value.single().lat)
        assertEquals(false, (vm.state.value.undo as UndoOffer.Mark).withLocation)
    }

    @Test
    fun `el mensaje de ayuda abre directo con el contacto de confianza y dice si la ubicación es aproximada`() = runTest(dispatcher) {
        store.settingsFlow.value = DiscreetSettings(contacts = listOf(TrustedContact("Sofía", "+573001234567")))
        val vm = viewModel()

        val precise = vm.prepareHelpMessage()
        assertEquals("Sofía", precise.contact?.name)
        assertEquals(SendStatus.MESSAGE_READY, precise.status)
        assertTrue(precise.text.contains("https://www.openstreetmap.org/?mlat=1.21364&mlon=-77.28112"))

        fix = HelpFix.Found(1.2, -77.3, precise = false)
        val approximate = vm.prepareHelpMessage()
        assertEquals(SendStatus.MESSAGE_READY_APPROXIMATE, approximate.status)
        assertTrue(approximate.text.contains("ubicación aproximada"))

        fix = HelpFix.Unavailable
        assertEquals(SendStatus.MESSAGE_READY_WITHOUT_LOCATION, vm.prepareHelpMessage().status)
    }
}

internal class FakeDiscreetStore : DiscreetStore {
    val settingsFlow = MutableStateFlow(DiscreetSettings())
    val marksFlow = MutableStateFlow<List<UncomfortableMark>>(emptyList())
    override val settings: Flow<DiscreetSettings> = settingsFlow
    override val marks: Flow<List<UncomfortableMark>> = marksFlow

    override suspend fun addContact(contact: TrustedContact): Boolean {
        settingsFlow.update { it.copy(contacts = it.contacts + contact) }
        return true
    }

    override suspend fun removeContact(contact: TrustedContact) = settingsFlow.update { it.copy(contacts = it.contacts - contact) }

    override suspend fun setFakeCall(name: String, delaySeconds: Int) =
        settingsFlow.update { it.copy(fakeCallName = name, fakeCallDelaySeconds = delaySeconds) }

    override suspend fun addMark(mark: UncomfortableMark) = marksFlow.update { listOf(mark) + it }

    override suspend fun removeMark(id: String) = marksFlow.update { marks -> marks.filterNot { it.id == id } }
}
