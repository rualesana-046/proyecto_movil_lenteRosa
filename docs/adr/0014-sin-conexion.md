# ADR 0014 — Sin conexión (RF-10)

- **Estado:** aceptado
- **PR:** pendiente
- **Decide:** AM (privacidad: qué se guarda, cuánto tiempo y cómo se borra; Aportar sin conexión); el agente (diseño técnico)

## Contexto

RF-10 pide que la app muestre la información guardada cuando no hay conexión, con su antigüedad visible y errores de red claros. Guardar datos en el teléfono tiene un costo de privacidad: una lista de zonas del mapa o de fichas abiertas revela por dónde se movió la usuaria. AM aprobó las recomendaciones de privacidad con dos agregados: Aportar queda desactivado sin conexión, sin guardar opiniones para después, y pruebas que verifiquen que el Modo discreto funciona sin internet.

## Decisión

### Qué se guarda

1. **Caché en Room** (`cache_sin_conexion.db`, Room 2.8.5): una tabla clave–JSON con la hora en que se guardó cada entrada.
   - **Listas de lugares:** el centro de Pasto y cada zona del mapa que se buscó, con las coordenadas redondeadas a 3 decimales (unos 100 m) en la clave.
   - **Fichas de lugares** con sus opiniones.
   - **Guardados y avisos** de la cuenta con sesión, con la clave `saved:<uid>`: nunca se muestran a otra cuenta.
2. **«Cerca de mí» nunca se guarda en el disco** (`cacheable = false`): sus resultados solo viven en memoria, porque guardarlos dejaría rastro de dónde estaba la usuaria. Sin conexión, «Cerca de mí» muestra el error de red y no resultados de otra búsqueda.
3. **Retención de 7 días.** Al leer, lo que tiene más de 7 días no se devuelve y se borra. Cada escritura también borra lo vencido. No hay procesos programados.
4. **Un 404 borra la entrada:** un lugar que ya no existe no reaparece sin conexión.

### Cómo se muestra

5. **Aviso visible** sobre la lista, la ficha, el mapa y Guardados: «Datos guardados hace 3 horas · Sin conexión», o «· No pudimos actualizar» si hay internet pero la API falla. Lleva el botón «Intentar de nuevo». Nunca se muestran datos guardados como si fueran actuales.
6. **Sin conexión, Aportar queda desactivado** en la ficha, la tarjeta del mapa y el formulario, con el texto «Necesitas conexión para enviar tu opinión.». Las opiniones no se guardan para enviarlas después: una opinión enviada tarde tendría una fecha que no corresponde a la visita.
7. **Conectividad** con `ConnectivityManager.registerDefaultNetworkCallback`: hay conexión si la red tiene internet y está validada.

### Mapa

8. **Caché de teselas de MapLibre limitada a 50 MB** (`setMaximumAmbientCacheSize`). Las teselas que se vieron sirven sin conexión. No hay descarga deliberada de zonas en RF-10.

### Borrado

9. **Al cerrar sesión o eliminar la cuenta** se borran los guardados y avisos de la cuenta (`clearSaved`). Las listas y fichas no son de ninguna cuenta y se conservan.
10. **«Borrar datos sin conexión»** en «Tu privacidad» borra toda la caché de Room y las teselas del mapa.
11. **Sin copia de seguridad ni transferencia:** la base está en `databases/`, que las reglas de TD-03 (ADR 0013) ya excluyen.

### Modo discreto sin conexión

12. El Modo discreto no usa la red: la llamada simulada y las marcas viven en el teléfono, y «Enviar mi ubicación» usa el GPS y abre la app de mensajes. `DiscreetOfflineTest` lo verifica en el emulador con el modo avión activado. En esa prueba el GPS es un proveedor de prueba y los proveedores combinado y de red quedan apagados.

## Consecuencias

- Dependencias nuevas: Room 2.8.5 (runtime, ktx y compilador con KSP) y espresso-intents 3.7.0 para las pruebas instrumentadas.
- Room y KSP suman tiempo de compilación; la caché es una sola tabla sin migraciones (versión 1).
- Las fichas guardadas pueden tener opiniones que ya cambiaron: el aviso con la antigüedad lo deja claro.
- La prueba sin conexión del Modo discreto necesita un emulador o un teléfono (`connectedDebugAndroidTest`) y no corre en el CI.
