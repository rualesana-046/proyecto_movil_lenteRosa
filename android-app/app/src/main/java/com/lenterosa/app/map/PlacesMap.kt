package com.lenterosa.app.map

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.lenterosa.app.places.GeoPoint
import com.lenterosa.app.places.PlaceSummary
import com.lenterosa.app.ui.comfort.ComfortLevel
import com.lenterosa.app.ui.theme.ComfortPalette
import kotlin.math.roundToInt
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point

private const val SOURCE_ID = "places"
private const val LAYER_ID = "places-layer"

/**
 * Mapa de MapLibre con un marcador por lugar. Cada marcador lleva el ícono de su etiqueta del
 * índice además del color, para no depender solo del color (RNF-02). La atribución propia del
 * SDK se oculta porque la pantalla muestra siempre la del proveedor en una franja visible.
 */
@Composable
fun PlacesMap(
    places: List<PlaceSummary>,
    selectedId: String?,
    camera: CameraTarget,
    initialCamera: CameraTarget,
    onSelect: (String?) -> Unit,
    onCameraIdle: (center: GeoPoint, radiusMeters: Int, zoom: Double) -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = isSystemInDarkTheme(),
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentOnSelect by rememberUpdatedState(onSelect)
    val currentOnCameraIdle by rememberUpdatedState(onCameraIdle)
    val mapView = remember {
        MapConfig.initialize(context)
        MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var style by remember { mutableStateOf<Style?>(null) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(null)
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            mapView.onPause()
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    DisposableEffect(mapView) {
        mapView.getMapAsync { libreMap ->
            libreMap.uiSettings.isAttributionEnabled = false
            libreMap.uiSettings.isLogoEnabled = false
            libreMap.uiSettings.isRotateGesturesEnabled = false
            libreMap.uiSettings.isTiltGesturesEnabled = false
            libreMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(initialCamera.center.lat, initialCamera.center.lng))
                .zoom(initialCamera.zoom)
                .build()
            libreMap.addOnMapClickListener { point ->
                val screen = libreMap.projection.toScreenLocation(point)
                val id = libreMap.queryRenderedFeatures(screen, LAYER_ID).firstOrNull()?.getStringProperty("id")
                currentOnSelect(id)
                true
            }
            libreMap.addOnCameraIdleListener {
                val target = libreMap.cameraPosition.target ?: return@addOnCameraIdleListener
                val bounds = libreMap.projection.visibleRegion.latLngBounds
                val radius = MapViewModel.distanceMeters(
                    GeoPoint(target.latitude, target.longitude),
                    GeoPoint(bounds.latitudeNorth, bounds.longitudeEast),
                )
                currentOnCameraIdle(GeoPoint(target.latitude, target.longitude), radius, libreMap.cameraPosition.zoom)
            }
            map = libreMap
        }
        onDispose { }
    }

    // El estilo sigue al tema; al cambiarlo se vuelven a agregar los marcadores.
    val styleUrl = MapConfig.styleUrl(dark)
    LaunchedEffect(map, styleUrl) {
        val libreMap = map ?: return@LaunchedEffect
        style = null
        libreMap.setStyle(Style.Builder().fromUri(styleUrl)) { loaded ->
            addMarkerImages(context, loaded)
            loaded.addSource(GeoJsonSource(SOURCE_ID))
            loaded.addLayer(
                SymbolLayer(LAYER_ID, SOURCE_ID).withProperties(
                    PropertyFactory.iconImage(Expression.get("icon")),
                    PropertyFactory.iconSize(
                        Expression.switchCase(Expression.get("selected"), Expression.literal(1.25f), Expression.literal(1f)),
                    ),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                    PropertyFactory.symbolSortKey(
                        Expression.switchCase(Expression.get("selected"), Expression.literal(1f), Expression.literal(0f)),
                    ),
                ),
            )
            style = loaded
        }
    }

    LaunchedEffect(style, places, selectedId) {
        val source = style?.getSourceAs<GeoJsonSource>(SOURCE_ID) ?: return@LaunchedEffect
        source.setGeoJson(
            FeatureCollection.fromFeatures(
                places.map { place ->
                    Feature.fromGeometry(Point.fromLngLat(place.location.lng, place.location.lat)).apply {
                        addStringProperty("id", place.id)
                        addStringProperty("icon", markerImageId(ComfortLevel.fromCode(place.comfortLabel)))
                        addBooleanProperty("selected", place.id == selectedId)
                    }
                },
            ),
        )
    }

    // Solo las órdenes nuevas (por ejemplo "Cerca de mí") mueven la cámara.
    LaunchedEffect(map, camera.version) {
        val libreMap = map ?: return@LaunchedEffect
        if (camera.version > initialCamera.version) {
            libreMap.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(camera.center.lat, camera.center.lng), camera.zoom))
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun markerImageId(level: ComfortLevel) = "marker-${level.name.lowercase()}"

/** Un marcador por etiqueta: círculo del tono de la escala con su ícono y borde blanco. */
private fun addMarkerImages(context: Context, style: Style) {
    val density = context.resources.displayMetrics.density
    val size = (36 * density).roundToInt()
    val border = 2.5f * density
    ComfortLevel.entries.forEach { level ->
        val colors = ComfortPalette.light.getValue(level.tone)
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val radius = size / 2f
        paint.color = android.graphics.Color.WHITE
        canvas.drawCircle(radius, radius, radius, paint)
        paint.color = colors.content.toArgb()
        canvas.drawCircle(radius, radius, radius - border, paint)
        ContextCompat.getDrawable(context, level.icon)?.mutate()?.let { icon ->
            icon.setTint(colors.container.toArgb())
            val inset = (size * 0.24f).roundToInt()
            icon.setBounds(inset, inset, size - inset, size - inset)
            icon.draw(canvas)
        }
        style.addImage(markerImageId(level), bitmap)
    }
}
