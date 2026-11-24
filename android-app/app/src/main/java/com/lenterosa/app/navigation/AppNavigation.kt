package com.lenterosa.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.lenterosa.app.account.AccountRoute
import com.lenterosa.app.account.AccountViewModel
import com.lenterosa.app.home.HomeScreen
import com.lenterosa.app.home.HomeTab
import com.lenterosa.app.map.MapViewModel
import com.lenterosa.app.onboarding.OnboardingRoute
import com.lenterosa.app.onboarding.OnboardingViewModel
import com.lenterosa.app.places.BackButton
import com.lenterosa.app.places.ContributeScreen
import com.lenterosa.app.places.ContributeViewModel
import com.lenterosa.app.places.PlaceDetailScreen
import com.lenterosa.app.places.PlaceDetailViewModel
import com.lenterosa.app.places.PlacesViewModel
import com.lenterosa.app.privacy.PrivacyScreen
import com.lenterosa.app.ui.glass.GlassTopBar
import com.lenterosa.app.ui.glass.currentMotion
import com.lenterosa.app.ui.motion.Motion
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.serialization.Serializable

/** Rutas tipadas de la app (Navigation Compose, ADR 0011). */
object Routes {
    /** Bienvenida de tres pasos, solo al primer uso (RF-11). */
    @Serializable
    data object Onboarding

    /** Inicio: mapa (RF-09) y lista (RF-01). */
    @Serializable
    data object Home

    @Serializable
    data object Account

    @Serializable
    data object Privacy

    @Serializable
    data class PlaceDetail(val id: String)

    @Serializable
    data class Contribute(val id: String, val name: String)
}

/** La primera pantalla: la bienvenida si nunca se completó; si no, el inicio. */
fun startDestination(onboardingCompleted: Boolean): Any = if (onboardingCompleted) Routes.Home else Routes.Onboarding

/** Al terminar o saltar la bienvenida se va al inicio y la bienvenida sale de la pila: "atrás" no vuelve a ella. */
fun NavController.finishOnboarding() = navigate(Routes.Home) {
    popUpTo(Routes.Onboarding) { inclusive = true }
    launchSingleTop = true
}

/** Clave del resultado que pide a la ficha recargarse después de publicar una opinión. */
const val REVIEW_PUBLISHED_KEY = "review_published"

@Composable
fun AppNavigation(
    startDestination: Any,
    requestGoogleIdToken: suspend () -> String,
    navController: NavHostController = rememberNavController(),
) {
    val motion = currentMotion()
    // La pestaña del inicio se recuerda al ir a otras pantallas y volver.
    var homeTab by rememberSaveable { mutableStateOf(HomeTab.MAP) }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = motion.enter(forward = true),
        exitTransition = motion.exit(forward = true),
        popEnterTransition = motion.enter(forward = false),
        popExitTransition = motion.exit(forward = false),
    ) {
        composable<Routes.Onboarding> {
            OnboardingRoute(
                viewModel = hiltViewModel<OnboardingViewModel>(),
                onFinished = { navController.finishOnboarding() },
            )
        }
        composable<Routes.Home> {
            HomeScreen(
                tab = homeTab,
                onTabSelected = { homeTab = it },
                mapViewModel = hiltViewModel<MapViewModel>(),
                placesViewModel = hiltViewModel<PlacesViewModel>(),
                onOpenPlace = { navController.navigate(Routes.PlaceDetail(it)) },
                onContribute = { id, name -> navController.navigate(Routes.Contribute(id, name)) },
                onOpenAccount = { navController.navigate(Routes.Account) },
                onOpenPrivacy = { navController.navigate(Routes.Privacy) },
            )
        }
        composable<Routes.Privacy> { PrivacyScreen(onBack = { navController.popBackStack() }) }
        composable<Routes.Account> {
            val hazeState = rememberHazeState()
            // Sin título en la barra: la pantalla de cuenta ya muestra el suyo.
            Scaffold(topBar = { GlassTopBar(hazeState, navigationIcon = { BackButton { navController.popBackStack() } }) }) { padding ->
                AccountRoute(
                    viewModel = hiltViewModel<AccountViewModel>(),
                    requestGoogleIdToken = requestGoogleIdToken,
                    modifier = Modifier.hazeSource(hazeState).padding(padding),
                )
            }
        }
        composable<Routes.PlaceDetail> { entry ->
            val route = entry.toRoute<Routes.PlaceDetail>()
            val published by entry.reviewPublished()
            PlaceDetailScreen(
                viewModel = hiltViewModel<PlaceDetailViewModel>(),
                placeId = route.id,
                refreshKey = published,
                onBack = { navController.popBackStack() },
                onContribute = { id, name -> navController.navigate(Routes.Contribute(id, name)) },
                onOpenAccount = { navController.navigate(Routes.Account) },
            )
        }
        composable<Routes.Contribute> { entry ->
            val route = entry.toRoute<Routes.Contribute>()
            ContributeScreen(
                viewModel = hiltViewModel<ContributeViewModel>(),
                placeId = route.id,
                placeName = route.name,
                onBack = { navController.popBackStack() },
                onPublished = {
                    // La pantalla anterior (la ficha o el inicio) se entera de que hay una opinión nueva.
                    navController.previousBackStackEntry?.savedStateHandle?.set(REVIEW_PUBLISHED_KEY, System.currentTimeMillis())
                    navController.popBackStack()
                },
                onOpenAccount = { navController.navigate(Routes.Account) },
            )
        }
    }
}

@Composable
private fun NavBackStackEntry.reviewPublished() =
    savedStateHandle.getStateFlow(REVIEW_PUBLISHED_KEY, 0L).collectAsStateWithLifecycle()

/** Transiciones entre pantallas con los tokens de movimiento; sin animación con "Quitar animaciones". */
private fun Motion.enter(forward: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    screenTransition<NavBackStackEntry>(forward)(this).targetContentEnter
}

private fun Motion.exit(forward: Boolean): AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    screenTransition<NavBackStackEntry>(forward)(this).initialContentExit
}
