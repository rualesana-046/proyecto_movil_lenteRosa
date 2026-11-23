# ADR 0006 — Cuentas y autenticación (RF-02)

- **Estado:** aceptado
- **PR:** #9
- **Decide:** AM (afecta a la seguridad y la privacidad de las usuarias)

## Contexto

RF-02 pide registro e inicio de sesión con correo o Google (Firebase Authentication); guardar, valorar y recibir alertas exigen cuenta, y la consulta como invitada sigue siendo pública. La API no debe tener secretos de Firebase y las opiniones deben seguir siendo anónimas.

## Decisión

### API

1. **Validación de ID tokens sin credenciales.** Spring Security actúa como servidor de recursos OAuth2 y verifica cada ID token de Firebase con las llaves públicas de Google (`securetoken@system.gserviceaccount.com`). Reglas: firma, `exp` e `iat`, emisor `https://securetoken.google.com/<projectId>`, audiencia `<projectId>`, `sub` (UID) no vacío de hasta 128 caracteres y `auth_time` en el pasado. No se usa Admin SDK ni cuenta de servicio: el único dato de configuración es el Project ID (`FIREBASE_PROJECT_ID`), que no es secreto.
2. **Falla cerrada.** Sin `FIREBASE_PROJECT_ID`, la API rechaza todos los tokens.
3. **Rutas.** `GET` de lugares, opiniones y salud son públicos; todo lo demás exige `Authorization: Bearer`. Sin sesiones ni cookies, por lo que no se usa CSRF. Una ruta protegida sin token responde 401 con ProblemDetail ("Necesitas iniciar sesión para hacer esto."), sin explicar por qué falló el token. Como consecuencia, las rutas inexistentes también responden 401 a quien no tiene sesión (por ejemplo `/actuator/health`).
4. **Datos mínimos.** La tabla `users` (V4) solo guarda el UID y la fecha de alta. No se guardan correo, nombre ni apodo. `email_verified` se lee del token en cada petición.
5. **Endpoints.** `PUT /api/v1/me` registra la cuenta (idempotente), `GET /api/v1/me` la consulta y `DELETE /api/v1/me` la elimina.
6. **Eliminar la cuenta** deja anónimas sus opiniones (`author_id = NULL`, sin borrarlas) y borra el registro, en una sola transacción (Ley 1581 de 2012; resuelve la parte de TD-05 que no depende de RF-04). Es idempotente. `AccountService` usa `@Transactional` porque la operación abarca dos módulos; es la excepción a los casos de uso sin anotaciones de Spring.

### App (pendiente de implementar en este mismo PR)

7. Firebase Authentication con correo y contraseña, y con Google mediante Credential Manager.
8. **Apodo:** se guarda solo en Firebase como `displayName`, nunca en la API; las opiniones siguen siendo anónimas.
9. **Correo verificado obligatorio para opinar (RF-04).** La API lo exigirá con el claim `email_verified` al implementar RF-04.
10. **Eliminar la cuenta pide reautenticación** en la app antes de borrar en Firebase y llamar a `DELETE /api/v1/me`.
11. `google-services.json` nunca se versiona; el CI genera uno de relleno con valores falsos solo para compilar.

## Consecuencias

- Rotar llaves de Google no requiere cambios: el decodificador lee el conjunto de llaves públicas.
- Las pruebas firman tokens con una llave RSA generada para las pruebas y aplican las mismas reglas de validación que producción.
- La consulta como invitada no cambia.
