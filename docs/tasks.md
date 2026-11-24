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
| RF-01 | Consulta como invitada | API: `places`, `reviews` (búsqueda por distancia, detalle con promedios, opiniones anónimas). App: explorar y ficha del lugar | completada: API y app (lista y ficha del lugar) | #3, #5, #11 |
| RF-02 | Cuentas | `users`, Firebase Auth en la app, validación de tokens en la API (ADR 0006) | completada: API y app (registro, verificación, apodo, eliminar cuenta). Falta probar el inicio con Google en un dispositivo real | #9 |
| RF-03 | Índice de Confort 0–100 | `comfortindex`: promedio bayesiano, recencia e incertidumbre, con pruebas de casos límite (ADR 0003). App: mostrar índice, etiqueta e incertidumbre | completada: API y app con etiqueta, ícono, índice e incertidumbre (ADR 0008) | #5, #11 |
| RF-04 | Aportar opinión | `reviews` (creación con autora, límite de abuso, comentarios sin datos de contacto; ADR 0007), pantalla Aportar | completada: API y pantalla Aportar en la app | #10, #11 |
| RF-05 | Lugares guardados | `users`, `places` | pendiente | — |
| RF-06 | Cápsula de IA | `insights`: Gemini Flash, validación de salida, plantillas de respaldo | pendiente | — |
| RF-07 | Alertas de cambio | `alerts`: detección de cambios y envío con FCM a quien guardó el lugar | pendiente | — |
| RF-08 | Modo discreto | App: atajo del ícono, tile de ajustes rápidos, tres acciones con estados reales | pendiente | — |
| RF-09 | Mapa | App: MapLibre con OpenFreeMap (Stadia como alternativa), atribución siempre visible, "Cerca de mí" bajo demanda y aviso de privacidad (ADR 0009) | completada | #12 |
| RF-10 | Sin conexión | App: caché Room, antigüedad visible, errores de red claros | pendiente | — |
| RF-11 | Onboarding | App: tres pasos al primer uso, con Navigation Compose (ADR 0011) | en revisión | #15 |

## Requisitos no funcionales

| RNF | Requisito | Cómo se cubre | Estado |
|---|---|---|---|
| RNF-01 | Privacidad | Ubicación solo bajo demanda; las coordenadas de búsqueda no se guardan ni se registran (prueba en RF-01); ubicación aproximada solo con "Cerca de mí" y aviso de privacidad en la app (ADR 0009) | en curso |
| RNF-02 | Accesibilidad | WCAG AA, contraste verificado, nunca solo color, movimiento reducido | en curso: contraste AA verificado por pruebas (también sobre vidrio, ADR 0010), escala de confort con texto e ícono (ADR 0008) y "Quitar animaciones" respetado |
| RNF-03 | Diseño | Sistema de diseño en temas claro y oscuro, Plus Jakarta Sans, liquid glass y animaciones fuera del modo discreto | en revisión: tokens, tipografía, escala de confort (ADR 0008), liquid glass, animaciones y mapa oscuro (ADR 0010), transiciones entre pantallas (ADR 0011) |
| RNF-04 | Seguridad | Sin secretos en el repositorio; clave de IA solo en la API; tokens validados en la API | en curso |
| RNF-05 | Costo | Cloud Run con topes y alerta de presupuesto | pendiente |
| RNF-06 | Calidad | Regresión completa en CI, APK firmado, instalación limpia y README completo | en curso |
| RNF-07 | Compatibilidad | Android 8.0 (API 26) o superior | completada |
| RNF-08 | Honestidad | Ninguna integración simulada se presenta como terminada | en curso |

## Orden de trabajo

Primero se termina toda la app; después vienen el despliegue de la API y el APK firmado para probar en un teléfono real. El orden puede ajustarse si hay dependencias técnicas, explicándolo en el informe del PR.

1. Liquid glass y animaciones (RNF-03).
2. Bienvenida de tres pasos (RF-11) y migración a Navigation Compose (TD-09).
3. Lugares guardados y alertas sutiles (RF-05, RF-07).
4. Modo discreto (RF-08). Sus decisiones de producto se consultan antes con AM.
5. Caché sin conexión con la antigüedad de los datos (RF-10).
6. Cápsula de IA con Gemini (RF-06). Antes se le indica a AM qué debe crear en Google AI Studio.
7. Repaso de accesibilidad (RNF-02).
8. Despliegue de la API (RNF-05) y APK firmado para el teléfono (RNF-06).

## Pruebas pendientes en dispositivo real

Lo que no se pudo verificar en el emulador ni con pruebas automáticas. Se marca cuando AM lo prueba en su teléfono.

- [ ] **Cerca de mí con ubicación real** (RF-09): el permiso de ubicación aproximada, el centrado del mapa y la búsqueda alrededor. En el emulador no llegó la posición simulada.
- [ ] **Inicio de sesión con Google** (RF-02): Credential Manager con una cuenta de Google real.
- [ ] **Aportar con una cuenta verificada** (RF-04) contra la API desplegada: publicar, límite de 7 días, rechazo de teléfonos y dirección aceptada.
- [ ] **Revisión visual del mapa** (RF-09): legibilidad de marcadores y atribución al sol, gestos y rendimiento en un teléfono de gama media.
- [ ] **Liquid glass con desenfoque real** (RNF-03): en el emulador Haze no dibujó el vidrio y solo se vio el tinte. Revisar en el teléfono el desenfoque detrás de las barras, la legibilidad y la fluidez al desplazarse.
- [ ] **Fluidez general** (RNF-03, RF-11): transiciones entre pantallas, bienvenida y desplazamiento en un teléfono de gama media. En el emulador, sobrecargado justo después de instalar, la app tardó en responder una vez (ANR) y luego funcionó con normalidad.

## Alcance del piloto: Pasto

El piloto se hace en Pasto (Nariño): los datos ficticios del seed, la demostración y el centro inicial del mapa son de Pasto. El código no se restringe por ciudad: la búsqueda, las opiniones y el índice funcionan con cualquier coordenada.

## Notas por funcionalidad

- **RF-04, pantalla Aportar:** Iluminación, Afluencia de mujeres y Visibilidad comercial son obligatorias. Experiencia nocturna es obligatoria si eligió "Fui de noche"; si eligió "Fui de día", esa pregunta ofrece la opción "No fui de noche" y se envía sin valor (`nightExperience` nulo).
- **RF-01, ficha del lugar:** si el promedio de una dimensión es nulo (sin opiniones, o sin visitas nocturnas para Experiencia nocturna), se muestra "Sin datos", nunca 0.
- **RF-03, índice en la app:** etiquetas en español: `VERY_COMFORTABLE` → Muy cómodo, `COMFORTABLE` → Cómodo, `MIXED_OPINIONS` → Opiniones mixtas, `UNCOMFORTABLE` → Poco cómodo, `FEW_OPINIONS` → Pocas opiniones. Con `index` nulo no se muestra número. Si `nightDataMissing` es verdadero, indicar que faltan opiniones de noche. Siempre con el aviso de que el índice refleja percepción comunitaria y no garantiza seguridad.

## Decisiones pendientes

- Ninguna por ahora. El proveedor de mosaicos de RF-09 se decidió en ADR 0009 (OpenFreeMap, con Stadia Maps como alternativa).
