# Requisitos — Lente Rosa

App Android que muestra cómo percibe la comunidad un lugar o una ruta, sin rastreo continuo, más un modo discreto de ayuda rápida. El índice refleja percepción comunitaria: **nunca garantiza seguridad ni sustituye a los servicios de emergencia**.

El alcance obligatorio no se recorta. Si el plazo se acorta, se usa el margen de los días 13 y 14.

## Requisitos funcionales

| ID | Requisito | Criterio de aceptación |
|---|---|---|
| RF-01 | Consulta como invitada | Una invitada busca y ve lugares con su índice sin iniciar sesión. |
| RF-02 | Cuentas | Registro e inicio de sesión con correo o Google (Firebase Auth). Guardar, valorar y recibir alertas exige cuenta. |
| RF-03 | Índice de Confort 0–100 | Promedio bayesiano con peso por recencia. Muestra etiqueta, cantidad de opiniones, antigüedad e incertidumbre. La fórmula vive solo en la API y es verificable. |
| RF-04 | Aportar opinión | Una usuaria autenticada valora un lugar; la opinión se muestra anónima a otras usuarias. Hay un límite contra abuso. |
| RF-05 | Lugares guardados | Una usuaria autenticada guarda y quita lugares. |
| RF-06 | Cápsula de IA | Gemini Flash, llamado solo desde la API, resume comentarios reales y separa hechos, opiniones e incertidumbre en lenguaje cotidiano. Si la IA falla o se agota la cuota, se usan plantillas de respaldo. |
| RF-07 | Alertas de cambio | Cuando una nueva opinión cambia de forma relevante un lugar guardado, quien lo guardó recibe un aviso por FCM. Reacciona a cada opinión, sin procesos programados. |
| RF-08 | Modo discreto | Se abre con un atajo del ícono y con un tile de ajustes rápidos. Pantalla gris con tres acciones: enviar ubicación, llamada simulada y marcar ruta incómoda. Cada acción informa su estado real. |
| RF-09 | Mapa | MapLibre con mosaicos OpenStreetMap de un proveedor que lo permita (no el servidor oficial de OSM), con atribución visible. |
| RF-10 | Sin conexión | Caché local (Room) que muestra la información guardada con su antigüedad visible y errores de red claros. |
| RF-11 | Onboarding | Tres pasos al primer uso. |

## Requisitos no funcionales

| ID | Requisito |
|---|---|
| RNF-01 | Privacidad: sin rastreo continuo; la ubicación se pide solo bajo demanda. |
| RNF-02 | Accesibilidad: WCAG AA, contraste verificado, nunca solo color, respeta movimiento reducido. |
| RNF-03 | Diseño: Material 3, temas claro y oscuro, Plus Jakarta Sans, liquid glass fuera del modo discreto. |
| RNF-04 | Seguridad: ningún secreto en el repositorio público; la clave de IA nunca entra a la app; tokens de Firebase validados en la API. |
| RNF-05 | Costo: USD 0 al mes; Cloud Run con topes (mínimo 0, máximo 2 instancias, alerta en USD 1). |
| RNF-06 | Calidad: pruebas de cálculo, autenticación, permisos, errores de red y funciones principales pasan en CI. |
| RNF-07 | Compatibilidad: Android 8.0 (API 26) o superior. |
| RNF-08 | Honestidad: ninguna integración simulada se presenta como terminada. |

## Terminado significa

- [ ] Una invitada consulta lugares sin iniciar sesión.
- [ ] Una usuaria autenticada guarda, valora y recibe alertas.
- [ ] El índice usa una fórmula verificable y muestra cantidad y antigüedad de los datos.
- [ ] La IA resume comentarios reales y comunica incertidumbre.
- [ ] El modo discreto ejecuta sus tres acciones e informa el estado real de cada una.
- [ ] No hay rastreo continuo de ubicación.
- [ ] La caché muestra información guardada con su antigüedad.
- [ ] Temas claro y oscuro con contraste AA y sin depender solo del color.
- [ ] Pruebas de cálculo, autenticación, permisos, errores de red y funciones principales pasan.
- [ ] La API responde de verdad en Cloud Run.
- [ ] Ninguna integración simulada se presenta como terminada.
- [ ] El README permite a otra persona instalar, ejecutar y probar el proyecto.
