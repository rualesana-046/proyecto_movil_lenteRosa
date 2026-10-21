# Tareas

Estados: **pendiente**, **en curso**, **bloqueada**, **en revisión**, **completada**.

Cada día cierra con algo que se prueba. Los días 13 y 14 son margen y entrega.

| Día | Tarea | Requisitos | Estado |
|---|---|---|---|
| 1 | Base: monorepo, guías del agente, API con `/api/v1/health` y prueba sobre PostGIS, app vacía que compila, CI | — | completada |
| 2 | Datos: migraciones Flyway, módulos `places` y `reviews`, consulta como invitada | RF-01 | pendiente |
| 3 | Índice: `comfortindex` con promedio bayesiano, recencia e incertidumbre, con pruebas de casos límite | RF-03 | pendiente |
| 4 | Identidad: Firebase Auth en la app, validación de tokens en la API, guardar y valorar, límite de abuso | RF-02, RF-04, RF-05 | pendiente |
| 5 | Nube temprana: primer despliegue en Cloud Run con Neon y secretos; prueba de humo | RNF-05 | pendiente |
| 6 | App I: onboarding, sistema de diseño en ambos temas, explorar con mapa; incorporar Hilt y KSP | RF-09, RF-11, RNF-03 | pendiente |
| 7 | App II: ficha del lugar, aportar opinión, cuenta | RF-04 | pendiente |
| 8 | Sin conexión: caché Room, antigüedad visible, errores de red claros | RF-10 | pendiente |
| 9 | Modo discreto: atajo, tile y las tres acciones con estados reales | RF-08 | pendiente |
| 10 | IA: cápsula con Gemini, validación de salida, respaldo con plantillas | RF-06 | pendiente |
| 11 | Alertas: detección de cambios y envío con FCM a quien guardó el lugar | RF-07 | pendiente |
| 12 | Integración: regresión completa, accesibilidad, contraste, despliegue automático | RNF-02, RNF-06 | pendiente |
| 13 | Margen: corregir lo pendiente, liquid glass y animaciones finales | RNF-03 | pendiente |
| 14 | Entrega: APK firmado, instalación limpia, documentación, deuda técnica y demo | — | pendiente |

## Notas para la app (días 6–7)

- Pantalla Aportar: las dimensiones Iluminación, Afluencia de mujeres y Visibilidad comercial son obligatorias. Experiencia nocturna es obligatoria si eligió "Fui de noche"; si eligió "Fui de día", esa pregunta ofrece la opción "No fui de noche" y se envía sin valor (`nightExperience` nulo).
- Ficha del lugar: si el promedio de una dimensión es nulo (sin opiniones, o sin visitas nocturnas para Experiencia nocturna), se muestra "Sin datos", nunca 0.

## Decisiones pendientes

- Proveedor de mosaicos OpenStreetMap para MapLibre: decidir antes del día 6.
