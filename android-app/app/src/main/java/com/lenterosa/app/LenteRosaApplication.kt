package com.lenterosa.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

// MapLibre se inicializa al mostrar el mapa por primera vez (MapConfig), no aquí: el arranque de la
// app queda más liviano, sobre todo al abrir el Modo discreto desde el tile o el atajo.
@HiltAndroidApp
class LenteRosaApplication : Application()
