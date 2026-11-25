package com.lenterosa.app.map

import android.content.Context
import com.lenterosa.app.BuildConfig
import okhttp3.OkHttpClient
import org.maplibre.android.MapLibre
import org.maplibre.android.module.http.HttpRequestUtil

/**
 * Proveedor del mapa (RF-09, ADR 0009). Los valores salen de BuildConfig para poder cambiar de
 * proveedor (OpenFreeMap por defecto, Stadia Maps como alternativa) sin tocar el código.
 */
object MapConfig {
    val styleUrl: String = BuildConfig.MAP_STYLE_URL
    val darkStyleUrl: String = BuildConfig.MAP_STYLE_URL_DARK

    /** Estilo según el tema: oscuro de noche para no deslumbrar ni llamar la atención. */
    fun styleUrl(dark: Boolean): String = if (dark) darkStyleUrl else styleUrl
    val providerName: String = BuildConfig.MAP_PROVIDER_NAME
    val attribution: String = BuildConfig.MAP_ATTRIBUTION
    val attributionUrl: String = BuildConfig.MAP_ATTRIBUTION_URL

    /**
     * User-Agent propio que identifica a la app ante el proveedor, como piden los términos de
     * OpenFreeMap. No incluye ningún dato de la usuaria ni del dispositivo.
     */
    val userAgent: String = "LenteRosa/${BuildConfig.VERSION_NAME} (Android; +https://github.com/rualesana-046/proyecto_movil_lenteRosa)"

    @Volatile
    private var initialized = false

    /**
     * Inicializa MapLibre una sola vez, con el User-Agent propio en todas sus descargas. Se llama
     * al mostrar el mapa por primera vez, no al abrir la app: así el Modo discreto (RF-08), que se
     * abre desde el tile o el atajo, no paga el arranque del mapa.
     */
    @Synchronized
    fun initialize(context: Context) {
        if (initialized) return
        initialized = true
        MapLibre.getInstance(context.applicationContext)
        HttpRequestUtil.setOkHttpClient(
            OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
                }
                .build(),
        )
        // Sin registro de las URL de las teselas: revelarían qué zonas se miran.
        HttpRequestUtil.setLogEnabled(false)
    }
}
