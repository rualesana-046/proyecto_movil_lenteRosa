# ADR 0012 — Lugares guardados y alertas de cambio (RF-05, RF-07)

- **Estado:** aceptado. El envío por FCM queda pendiente de la aprobación de AM.
- **PR:** #16
- **Decide:** AM (reglas de producto propuestas aquí: qué cambio avisa, límite de guardados, textos y retención); el agente (diseño técnico)

## Contexto

RF-05 pide que una usuaria autenticada guarde y quite lugares. RF-07 pide que, cuando una nueva opinión cambie de forma relevante un lugar guardado, quien lo guardó reciba un aviso por FCM, reaccionando a cada opinión, sin procesos programados. AM pidió avisos sutiles.

El envío por FCM requiere dos cosas que necesitan aprobación explícita de AM:

- credenciales de Firebase Admin en la API, que son un secreto;
- guardar el token de notificaciones de cada teléfono, lo que toca la privacidad.

## Decisión

### Lugares guardados (RF-05)

1. **Tabla `saved_places`** (V5): UID, lugar y fecha. Tiene llaves foráneas a `users` y `places` con `ON DELETE CASCADE`, así que al eliminar la cuenta o el lugar las filas se borran solas (Ley 1581 de 2012).
2. **Endpoints** `GET /api/v1/me/saved-places`, `PUT` y `DELETE /api/v1/me/saved-places/{placeId}`. Guardar y quitar son idempotentes. Requieren sesión, pero no correo verificado.
3. **Errores y límite:**
   - un lugar inexistente responde 404, y la API lo sabe por la llave foránea, sin leer la tabla de otro módulo;
   - **límite propuesto: 100 lugares por usuaria** (409, con un mensaje claro);
   - guardar registra la cuenta si hacía falta.

### Alertas (RF-07)

4. **Antes y después en el mismo momento:**
   - `ContributeReviewService` calcula el Índice de Confort justo antes y justo después de publicar la opinión, y publica el evento `ReviewContributed`;
   - así la diferencia se debe solo a la opinión nueva y no al paso del tiempo;
   - el evento sale por un puerto propio que la infraestructura conecta a Spring, de modo que la capa de aplicación no depende de Spring.
5. **El módulo `alerts` escucha cada evento** y avisa a quienes guardaron el lugar, menos a la autora de la opinión. No hay procesos programados. Si crear los avisos falla, la opinión igual queda publicada y en el registro no se escribe quién opinó.
6. **Regla de cambio relevante (propuesta):** cambia la etiqueta del índice, o el índice se mueve **10 puntos o más**. Los cambios pequeños no avisan.
7. **Avisos sutiles:**
   - un solo aviso sin leer por lugar. Si el lugar vuelve a cambiar antes de que se lea, el aviso se actualiza; si vuelve a como estaba, el aviso desaparece;
   - en la app, solo un punto en la pestaña Guardados, sin sonido ni vibración, que el lector de pantalla anuncia con texto («2 avisos nuevos»);
   - el texto es neutro: «Cambió cómo lo percibe la comunidad: ahora «X», antes «Y».».
8. **Tabla `alerts`** (V6): guarda etiquetas e índices de antes y después, nunca quién opinó. Los avisos leídos se borran a los **30 días**, en el momento en que llega un aviso nuevo para esa usuaria, sin procesos programados.
9. **Endpoints** `GET /api/v1/me/alerts` (no leídos y los 50 más recientes) y `PUT /api/v1/me/alerts/read`. La app los marca como leídos al abrir la pestaña.

### App

10. **Botón de guardar** (marcador) en la ficha y en la tarjeta del mapa. Su descripción dice la acción («Guardar lugar», «Quitar de guardados»).
11. **Nueva pestaña «Guardados»** con los avisos arriba («Nuevo» escrito, no solo de color) y los lugares debajo. Sin sesión, invita a iniciar sesión.
12. **El cambio se ve al instante** y se deshace si la API falla. Un almacén único (`SavedPlacesStore`) comparte el estado entre la ficha, el mapa y la pestaña.

### Pendiente: envío por FCM (requiere aprobación)

13. **Para enviar notificaciones push habrá que:**
    - crear una cuenta de servicio de Firebase Admin y guardarla como secreto de la API;
    - registrar el token de FCM del teléfono, en una tabla con borrado en cascada;
    - pedir en la app el permiso de notificaciones de Android 13 o superior;
    - enviar el aviso desde `alerts` con el mismo texto neutro.

    Hasta entonces, los avisos se ven al abrir la app.

## Consecuencias

- `alerts` depende de la API pública de `places` (el evento), de `users` (quién guardó) y de `comfortindex` (etiquetas).
- Dos opiniones recién publicadas dan una evidencia apenas menor que 2 (por el peso por antigüedad), así que un lugar nuevo necesita tres opiniones para dejar «Pocas opiniones». Es coherente con ADR 0003.
