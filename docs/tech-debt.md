# Deuda técnica

Prioridades: **alta** (bloquea una funcionalidad o la seguridad), **media** (afecta calidad o mantenimiento), **baja** (mejora sin urgencia).

| ID | Descripción | Prioridad | Origen | Estado |
|---|---|---|---|---|
| TD-01 | Actualizar AGP (≥ 9.4) y Gradle (≥ 9.8) cuando una versión estable de Kotlin los certifique. Kotlin 2.4.21 certifica hasta AGP 9.3.1 y Gradle 9.7.0 (revisado en ADR 0005). | baja | PR #1 (ADR 0001) | pendiente |
| TD-02 | Quitar `allowEmptyShould(true)` de `LayeredArchitectureTest` cuando los módulos tengan clases. Mientras exista, las reglas de capas pasan sin verificar nada. | media | PR #1 | resuelta (PR #3) |
| TD-03 | Agregar `android:dataExtractionRules` (Android 12+) que excluya los datos de la app de copias en la nube y transferencias; la app solo declara `allowBackup="false"`, que lint marca como obsoleto. | media | PR #1 (lint) | pendiente |
| TD-04 | Evaluar Dependabot updates (actualizaciones de seguridad y de versiones con PRs automáticos). Solo están activas las alertas. Revisar al cerrar el alcance funcional. | baja | PR #2 (configuración del repositorio) | pendiente |
| TD-05 | Con la autenticación (RF-02, RF-04): exigir `author_id` en las opiniones nuevas desde la API, y al eliminar una cuenta dejar sus opiniones anónimas (`author_id = NULL`) en vez de borrarlas, por la Ley 1581 de 2012 (protección de datos personales). | media | PR #3 (V3, RF-01) | en curso: la anonimización al eliminar la cuenta está en PR #9; falta exigir `author_id` con RF-04 |
| TD-06 | El Índice de Confort traía a la API todas las opiniones de los lugares consultados; el costo crecía con su cantidad. Se resolvió calculando sumas ponderadas en la base (ADR 0004). | baja | PR #5 (RF-03) | resuelta (PR #7) |
| TD-07 | Evaluar Spring Boot 4.2 junto con Spring Modulith 2.2 (línea nueva, no solo parches). | baja | PR #8 (ADR 0005) | pendiente |
| TD-08 | Compose BOM 2026.11.01 o posterior (Compose 1.13) exige `compileSdk` 37.1: instalar la plataforma `android-37.1` del SDK (requiere aprobación) y confirmar qué versión de AGP la admite. | baja | PR #8 (ADR 0005) | pendiente |
