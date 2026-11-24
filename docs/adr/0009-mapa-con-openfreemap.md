# ADR 0009 — Mapa con MapLibre y OpenFreeMap (RF-09)

- **Estado:** aceptado
- **PR:** #12
- **Decide:** AM (proveedor, alternativa, atribución siempre visible y aviso de privacidad); el agente (diseño técnico)

## Contexto

RF-09 pide un mapa con MapLibre y mosaicos de OpenStreetMap, sin usar el servidor oficial de OSM. Se compararon OpenFreeMap, Stadia Maps y MapTiler: límites del plan gratuito, tarjeta y clave, política de uso, atribución, uso sin conexión y privacidad. Además, la ubicación solo se pide bajo demanda (RNF-01).

## Decisión

### Proveedor

1. **OpenFreeMap** con el estilo `liberty` (y `dark` con el tema oscuro, ADR 0010): sin clave, sin registro, sin límite de vistas y con uso comercial permitido. Sin clave no hay ningún secreto dentro del APK, que es público.
2. **Stadia Maps como alternativa configurable.** La URL del estilo, el nombre del proveedor y el texto y enlace de la atribución están en `BuildConfig` y se cambian con propiedades de Gradle (`lenteRosa.mapStyleUrl`, `lenteRosa.mapProviderName`, `lenteRosa.mapAttribution`, `lenteRosa.mapAttributionUrl`), sin tocar el código. Stadia exige cuenta y clave, que es un secreto: activarla requiere la aprobación de AM.
3. **Riesgo:** OpenFreeMap no ofrece SLA y puede cambiar sin aviso. La alternativa anterior es la mitigación.
4. **MapLibre Android 13.7.1** (versión estable). Las versiones con sufijo `-globe` son experimentales y no se usan.

### Condiciones de uso de OpenFreeMap

5. **User-Agent propio:** todas las descargas del mapa llevan `LenteRosa/<versión> (Android; +<repositorio>)`. No incluye datos de la usuaria ni del dispositivo.
6. **Descarga sin conexión (para RF-10):** cada descarga debe iniciarla la usuaria, con un máximo de 3.000 teselas y 50 MB, y 4 conexiones en paralelo. Las zonas grandes se dividen en varias descargas. Fuera de eso, está prohibido descargar de forma automatizada. La caché normal de MapLibre de lo que ya se vio no es una descarga masiva.
7. **Atribución:** "© OpenFreeMap © OpenMapTiles · Datos © colaboradores de OpenStreetMap", enlazada a la página de copyright de OSM.

### Atribución siempre visible

8. La atribución del SDK (botón "i") y su logo se ocultan. En su lugar, una franja con el texto completo va **fuera de la zona del mapa**, entre el mapa y la barra inferior. Así, ni la tarjeta del lugar, ni el botón Aportar, ni los controles del mapa, ni la barra inferior pueden taparla.
9. `MapAttributionLayoutTest` (Robolectric, se ejecuta en el CI) lo comprueba en tres tamaños: teléfono común, pantalla pequeña y texto al doble de tamaño. Con la tarjeta del lugar abierta, "Buscar en esta zona" y un aviso a la vez, verifica que la atribución se vea completa dentro de la pantalla, que nada se superponga y que el mapa termine encima de ella. Si el liquid glass hace que el contenido pase por detrás de las barras, esta prueba falla y habrá que mantener la franja fuera de la zona translúcida.

### Ubicación bajo demanda

10. La app abre en Pasto (zona del piloto) sin pedir permisos. La ubicación se pide solo al tocar **"Cerca de mí"**, después de un diálogo que explica para qué se usa.
11. Solo se pide **ubicación aproximada** (`ACCESS_COARSE_LOCATION`), una sola lectura (`getCurrentLocation`), sin seguimiento continuo. Si no llega una lectura nueva en 15 s, se usa la última del teléfono solo si tiene menos de 5 minutos.
12. La ubicación se usa para centrar el mapa y buscar en la API, que no la guarda ni la registra (RNF-01). La app tampoco la guarda.
13. Al mover el mapa se ofrece "Buscar en esta zona", con un radio de hasta 5 km. No se busca automáticamente en cada movimiento.

### Aviso de privacidad

14. Hay una nueva pantalla "Tu privacidad", que se abre con el ícono de escudo y explica en lenguaje cotidiano tres puntos:
    - El proveedor del mapa puede ver qué zona se mira y la IP técnica de la conexión, pero no quién es la usuaria: la app no le envía nombre, correo ni cuenta.
    - Lente Rosa no guarda la ubicación.
    - Las opiniones son anónimas.
15. MapLibre no escribe en el registro las URL de las teselas, que revelarían qué zonas se miran.

### Pantallas

16. El inicio tiene dos pestañas: **Mapa** (por defecto) y **Lista**. Cada marcador lleva el tono de la escala de confort y el ícono de su etiqueta, nunca solo el color. Al tocar uno se abre una tarjeta con nombre, distancia, índice, "Ver ficha" y "Aportar". "Aportar" lleva a la ficha si falta la sesión o la verificación del correo.
17. El mapa no es accesible para lectores de pantalla. La pestaña Lista, con los mismos lugares, es la alternativa accesible.

## Consecuencias

- El APK de depuración pesa unos 67 MB por las bibliotecas nativas de MapLibre para todas las arquitecturas (TD-10).
- Cambiar de proveedor no requiere tocar el código, pero sí revisar sus términos y su atribución.
