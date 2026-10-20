# Deuda técnica

Prioridades: **alta** (bloquea la entrega o la seguridad), **media** (afecta calidad o mantenimiento), **baja** (mejora sin urgencia).

| ID | Descripción | Prioridad | Origen | Estado |
|---|---|---|---|---|
| TD-01 | Actualizar a AGP 9.4.x y Gradle 9.8.x cuando Kotlin los certifique. Hoy Kotlin 2.4.21 certifica hasta AGP 9.3.1 y Gradle 9.7.0. | baja | Día 1, ADR 0001 | pendiente |
| TD-02 | Quitar `allowEmptyShould(true)` de `LayeredArchitectureTest` cuando los módulos tengan clases (día 2–3). Mientras exista, las reglas de capas pasan sin verificar nada. | media | Día 1 | pendiente |
| TD-03 | Agregar `android:dataExtractionRules` (Android 12+) que excluya los datos de la app de copias en la nube y transferencias; hoy solo está `allowBackup="false"`, que lint marca como obsoleto. | media | Día 1, lint | pendiente |
| TD-04 | Evaluar Dependabot updates (actualizaciones de seguridad y de versiones con PRs automáticos). Hoy solo están activas las alertas. Revisar al final del proyecto. | baja | Día 1, configuración del repositorio | pendiente |
