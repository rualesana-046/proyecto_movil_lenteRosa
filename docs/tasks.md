# Hoja de ruta

Seguimiento por funcionalidad. Los requisitos y sus criterios de aceptación están en [`requirements.md`](requirements.md).

Estados: **pendiente**, **en curso**, **bloqueada**, **en revisión**, **completada**.

## Base técnica

| Componente | Alcance | Estado | PR |
|---|---|---|---|
| Monorepo y guías | Estructura, `AGENTS.md`, README, ADR 0001 | completada | #1, #2 |
| API mínima | Spring Boot, `/api/v1/health`, Flyway con PostGIS, pruebas con Testcontainers | completada | #1 |
| App mínima | Kotlin, Compose, Material 3, minSdk 26 | completada | #1 |
| CI | Compilación y pruebas de API y app en GitHub Actions | completada | #1 |
| Despliegue | API en Cloud Run con Neon y secretos; prueba de humo (RNF-05) | pendiente | — |
| Despliegue automático | Publicación de la API desde GitHub Actions con Workload Identity Federation | pendiente | — |

## Requisitos funcionales

| RF | Funcionalidad | Módulos y piezas | Estado | PR |
|---|---|---|---|---|
| RF-01 | Consulta como invitada | API: `places`, `reviews` (búsqueda por distancia, detalle con promedios, opiniones anónimas). App: explorar y ficha del lugar | en curso: API completada, con el Índice de Confort; falta la app | #3, #5 |
| RF-02 | Cuentas | `users`, Firebase Auth en la app, validación de tokens en la API (ADR 0006) | en revisión: API y app (registro, Google, verificación, apodo, eliminar cuenta) | #9 |
| RF-03 | Índice de Confort 0–100 | `comfortindex`: promedio bayesiano, recencia e incertidumbre, con pruebas de casos límite (ADR 0003). App: mostrar índice, etiqueta e incertidumbre | en curso: API completada; falta la app | #5 |
| RF-04 | Aportar opinión | `reviews` (creación con autora, límite de abuso), pantalla Aportar | pendiente | — |
| RF-05 | Lugares guardados | `users`, `places` | pendiente | — |
| RF-06 | Cápsula de IA | `insights`: Gemini Flash, validación de salida, plantillas de respaldo | pendiente | — |
| RF-07 | Alertas de cambio | `alerts`: detección de cambios y envío con FCM a quien guardó el lugar | pendiente | — |
| RF-08 | Modo discreto | App: atajo del ícono, tile de ajustes rápidos, tres acciones con estados reales | pendiente | — |
| RF-09 | Mapa | App: MapLibre con mosaicos OpenStreetMap y atribución | pendiente | — |
| RF-10 | Sin conexión | App: caché Room, antigüedad visible, errores de red claros | pendiente | — |
| RF-11 | Onboarding | App: tres pasos al primer uso | pendiente | — |

## Requisitos no funcionales

| RNF | Requisito | Cómo se cubre | Estado |
|---|---|---|---|
| RNF-01 | Privacidad | Ubicación solo bajo demanda; las coordenadas de búsqueda no se guardan ni se registran (prueba en RF-01) | en curso |
| RNF-02 | Accesibilidad | WCAG AA, contraste verificado, nunca solo color, movimiento reducido | pendiente |
| RNF-03 | Diseño | Sistema de diseño en temas claro y oscuro, Plus Jakarta Sans, liquid glass y animaciones fuera del modo discreto | pendiente |
| RNF-04 | Seguridad | Sin secretos en el repositorio; clave de IA solo en la API; tokens validados en la API | en curso |
| RNF-05 | Costo | Cloud Run con topes y alerta de presupuesto | pendiente |
| RNF-06 | Calidad | Regresión completa en CI, APK firmado, instalación limpia y README completo | en curso |
| RNF-07 | Compatibilidad | Android 8.0 (API 26) o superior | completada |
| RNF-08 | Honestidad | Ninguna integración simulada se presenta como terminada | en curso |

## Notas por funcionalidad

- **RF-04, pantalla Aportar:** Iluminación, Afluencia de mujeres y Visibilidad comercial son obligatorias. Experiencia nocturna es obligatoria si eligió "Fui de noche"; si eligió "Fui de día", esa pregunta ofrece la opción "No fui de noche" y se envía sin valor (`nightExperience` nulo).
- **RF-01, ficha del lugar:** si el promedio de una dimensión es nulo (sin opiniones, o sin visitas nocturnas para Experiencia nocturna), se muestra "Sin datos", nunca 0.
- **RF-03, índice en la app:** etiquetas en español: `VERY_COMFORTABLE` → Muy cómodo, `COMFORTABLE` → Cómodo, `MIXED_OPINIONS` → Opiniones mixtas, `UNCOMFORTABLE` → Poco cómodo, `FEW_OPINIONS` → Pocas opiniones. Con `index` nulo no se muestra número. Si `nightDataMissing` es verdadero, indicar que faltan opiniones de noche. Siempre con el aviso de que el índice refleja percepción comunitaria y no garantiza seguridad.

## Decisiones pendientes

- **RF-09:** proveedor de mosaicos OpenStreetMap para MapLibre (no el servidor oficial de OSM). Debe decidirse antes de implementar el mapa.
