# ADR 0013 — Modo discreto (RF-08)

- **Estado:** aceptado
- **PR:** #17
- **Decide:** AM (las 8 decisiones de producto y los ajustes de la Fase 1); el agente (diseño técnico)

## Contexto

RF-08 pide un modo discreto de ayuda rápida: se abre con un atajo del ícono y con un tile de ajustes rápidos, en una pantalla gris con tres acciones que informan su estado real. AM aprobó las 8 recomendaciones de producto con ajustes de la Fase 1, entre ellos la métrica de activarlo en 5 segundos o menos.

## Decisión

### Entradas

1. **Tres formas de entrar,** que abren la misma actividad (`DiscreetActivity.intent`):
   - **Atajo del ícono:** al mantener presionado el ícono aparece «Atajo» (`res/xml/shortcuts.xml`).
   - **Tile de ajustes rápidos:** nombre «Atajo» e ícono neutro (un círculo con un punto), sin el nombre ni el logo de Lente Rosa.
   - **Gesto dentro de la app:** mantener presionado el título «Lente Rosa» del inicio. Tocarlo no hace nada, así que no choca con la navegación. TalkBack lo ofrece como acción con nombre («Abrir el Modo discreto»).

### Pantalla

2. **Actividad aparte, con nombre neutro «Atajo»:**
   - fuera de Recientes una vez que se sale, y con el nombre «Atajo» y la miniatura en negro mientras está abierta;
   - sin capturas de pantalla (`FLAG_SECURE`);
   - en su propia tarea y visible sobre la pantalla bloqueada.
3. **Pantalla gris neutra:** sin el rosa de la marca, **sin liquid glass ni animaciones** (`VisualPreferences(discreetMode = true)`, ADR 0010). Los grises cumplen AA en claro y oscuro, y una prueba lo verifica.
4. **Partes fijas y desplazables:** las acciones se desplazan si falta espacio, y la parte de abajo (estado, «Deshacer» y el acceso al 123) queda **siempre visible**. «Salir» lleva a la pantalla de inicio del teléfono y quita la tarea.
5. **Funciona sin cuenta.** Los contactos de confianza (hasta 3), el nombre y el tiempo de la llamada y las marcas se guardan **solo en el teléfono** (DataStore). Nada de esto va a la API.

6. **Sin copia de seguridad ni transferencia (TD-03):**
   - `data_extraction_rules.xml` (Android 12 o superior) excluye todos los dominios de la app, tanto en la copia en la nube como en la transferencia entre dispositivos;
   - `backup_rules.xml` hace lo mismo en Android 11 o inferior;
   - así nunca salen del teléfono los contactos de confianza, las marcas, los ajustes, la sesión y los tokens de Firebase, ni la caché del mapa;
   - `allowBackup="false"` no basta, porque desde Android 12 no impide la transferencia;
   - `BackupRulesTest` lo verifica.

### Acciones de un solo toque

7. **Enviar mi ubicación:**
   - pide la ubicación **precisa** solo la primera vez; si se niega, usa la aproximada y avisa que es menos exacta;
   - abre directo la app de mensajes con el contacto de confianza, si hay uno guardado, o la hoja de compartir si no;
   - el texto incluye un enlace a OpenStreetMap;
   - sobre la pantalla bloqueada, pide desbloquear antes de abrir los mensajes;
   - estado real: «Tu mensaje está listo… Tú decides si lo envías». La app no afirma que se envió.
8. **Llamada simulada:**
   - usa el nombre y el tiempo configurados antes (ahora mismo, 10 s o 30 s);
   - muestra un aviso con «**Deshacer» durante 5 segundos** y la cuenta regresiva;
   - suena con el tono de llamada y vibra;
   - se puede contestar (con contador) o colgar, tocando el círculo o su texto. No hay ninguna llamada real.
9. **Marcar incómodo:**
   - guarda en el teléfono la hora y, si ya hay permiso, la ubicación aproximada con 3 decimales (unos 100 m), sin pedir nada;
   - muestra un aviso con «**Deshacer» durante 5 segundos**;
   - no se envía a la API ni afecta el índice.
10. **Acceso al 123:** el texto «Si estás en peligro, llama al 123» abre el marcador con el número escrito, sin llamar automáticamente.

### Métrica: activar en 5 segundos o menos

11. **Prueba instrumentada `DiscreetLaunchTimeTest`:**
    - mide desde la intención del tile (el mismo `PendingIntent`) y desde la del atajo hasta que las tres acciones están en pantalla;
    - el presupuesto de la app es de **2 s**, y los otros 3 s quedan para el gesto de la usuaria;
    - se corre en un dispositivo con `connectedDebugAndroidTest`; el CI no tiene emulador.
12. **Arranque más liviano:** MapLibre ya no se inicializa al abrir la app, sino al mostrar el mapa por primera vez. Así el tile y el atajo no pagan el arranque del mapa.
13. **Resultados en el emulador de desarrollo:**
    - **Con gráficos por software** (SwiftShader o lavapipe, 1 núcleo): entre 3,9 y 7,5 s, por encima del presupuesto. El propio sistema perdía de 30 a 58 cuadros.
    - **Con gráficos por hardware** (GPU del Mac por Metal, 4 núcleos; AM autorizó el cambio): **dentro del presupuesto** en todas las mediciones, con la versión de depuración sin precompilar:

      | Medición | Tiempo |
      |---|---|
      | Tile, primero en su ejecución | 0,84 – 1,12 s |
      | Atajo, primero en su ejecución | 0,59 – 1,09 s |
      | Tile, después del atajo | 0,14 – 0,16 s |

      Con unos 3 s para el gesto, la activación completa queda por debajo de 5 s.
    - **Pendiente:** confirmarlo en un teléfono real con la versión de publicación (TD-11).

## Consecuencias

- Hay dos permisos nuevos: ubicación precisa (solo al tocar «Enviar mi ubicación») y vibración.
- Las pruebas instrumentadas usan Espresso 3.7.0, porque la versión que trae Compose (3.5) falla en Android 17.
