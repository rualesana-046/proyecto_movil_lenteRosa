package com.lenterosa.app.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.testing.TestNavHostController
import androidx.navigation.toRoute
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Rutas tipadas y la salida de la bienvenida (RF-11, ADR 0011). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = android.app.Application::class)
class AppNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var navController: TestNavHostController

    private fun show(start: Any) {
        compose.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            NavHost(navController, startDestination = start) {
                composable<Routes.Onboarding> { Text("bienvenida") }
                composable<Routes.Home> { Text("inicio") }
                composable<Routes.PlaceDetail> { Text("ficha") }
            }
        }
    }

    @Test
    fun `la primera vez se abre la bienvenida y después el inicio`() {
        assertEquals(Routes.Onboarding, startDestination(onboardingCompleted = false))
        assertEquals(Routes.Home, startDestination(onboardingCompleted = true))
    }

    @Test
    fun `al terminar la bienvenida se va al inicio y atrás no vuelve a ella`() {
        show(startDestination(onboardingCompleted = false))

        compose.runOnUiThread { navController.finishOnboarding() }

        assertTrue(navController.currentDestination!!.hasRoute(Routes.Home::class))
        val inStack = navController.currentBackStack.value.mapNotNull { it.destination.route }
        assertFalse("La bienvenida quedó en la pila: $inStack", inStack.any { it.contains("Onboarding") })
        // Lo único debajo del inicio es el grafo raíz: no hay otra pantalla a la que volver.
        assertEquals(null, navController.previousBackStackEntry?.destination?.route?.takeIf { "Onboarding" in it || "Home" in it })
    }

    @Test
    fun `la ficha recibe el identificador del lugar por la ruta tipada`() {
        show(Routes.Home)

        compose.runOnUiThread { navController.navigate(Routes.PlaceDetail("p-42")) }

        assertEquals("p-42", navController.currentBackStackEntry!!.toRoute<Routes.PlaceDetail>().id)
    }
}
