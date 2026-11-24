package com.lenterosa.app.ui.glass

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lenterosa.app.ui.motion.Motion
import dev.chrisbanes.haze.HazeState

private val TopBarShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
private val BottomBarShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
val GlassCardShape = RoundedCornerShape(20.dp)

/** Barra superior de vidrio: el contenido de la pantalla se desplaza por detrás. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlassTopBar(
    hazeState: HazeState?,
    title: @Composable () -> Unit = {},
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent, scrolledContainerColor = Color.Transparent),
        modifier = Modifier.glassSurface(hazeState, TopBarShape),
    )
}

/** Barra de navegación inferior de vidrio. */
@Composable
fun GlassNavigationBar(hazeState: HazeState?, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        modifier = modifier.glassSurface(hazeState, BottomBarShape),
        content = content,
    )
}

/** Movimiento actual según "Quitar animaciones" del sistema. */
@Composable
fun currentMotion(): Motion = Motion(LocalVisualPreferences.current.reducedMotion)
