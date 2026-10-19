# ADR 0001 — Monorepo y versiones base

- **Estado:** aceptado
- **Fecha:** 2026-10-18
- **Decide:** AM

## Contexto

Lente Rosa tiene tres piezas desplegables (app Android, API y base de datos) y un plazo de 14 días hábiles. Hace falta una base que compile y se pruebe desde el día 1, con versiones compatibles entre sí y verificadas en fuentes oficiales, y un repositorio público que el docente pueda revisar.

## Decisión

1. **Monorepo** con `android-app/`, `backend-api/`, `docs/`, `infra/` y `.github/workflows/`. Cada parte tiene su propio build de Gradle (con Kotlin DSL) y su propio wrapper, sin un build raíz común.
2. **API como monolito modular** (`places`, `reviews`, `comfortindex`, `insights`, `alerts`, `users`) con capas `domain`, `application` e `infrastructure`. Los límites entre módulos se verifican con Spring Modulith y las capas con ArchUnit, ambos solo en pruebas.
3. **Salud de la API** con Spring Boot Actuator, exponiendo solo `health` y sin detalles públicos, con alias `/api/v1/health`.
4. **Versiones fijadas** (verificadas el 2026-10-18):

| Componente | Versión | Motivo |
|---|---|---|
| Java | 21 (toolchain de Gradle) | Versión LTS aprobada; Spring Boot 4.1.1 admite de 17 a 26 |
| Spring Boot | 4.1.1 | Último GA de la línea 4.1 |
| Gradle (API y app) | 9.7.0 | Máximo que Kotlin 2.4.21 certifica; AGP 9.3 pide ≥ 9.5.0; Boot admite 9.x |
| Android Studio | Rabbit 1 (2026.2.1) | Instalado; admite AGP 7.1–9.4 |
| AGP | 9.3.3 | Último parche de la línea que Kotlin 2.4.21 certifica; admite API 37 |
| Kotlin | 2.4.21 | Último estable; Kotlin integrado de AGP 9 |
| Compose BOM | 2026.09.00 | Último BOM estable |
| Testcontainers | 2.0.5 (administrado por Boot) | Coincide con la versión que fija Boot 4.1.1 |
| Flyway | 12.4.0 (administrado por Boot) + `flyway-database-postgresql` | Flyway 10+ separa el soporte de PostgreSQL en un módulo propio |
| PostGIS (pruebas) | `postgis/postgis:18-3.6` | Misma versión mayor que Neon (PostgreSQL 18 + PostGIS 3.6.4) |
| Spring Modulith | 2.1.1 (solo pruebas) | Compilado contra Spring Boot 4.1.1 |
| ArchUnit | 1.5.1 (solo pruebas) | Reglas de capas dentro de cada módulo |

5. **Hilt y KSP no entran el día 1.** Se incorporan el día 6, cuando haya dependencias reales que inyectar.

## Consecuencias

- La imagen `postgis/postgis` solo se publica para amd64. En Macs con Apple Silicon corre emulada: arranca más lento, pero funciona. En el CI (ubuntu amd64) corre nativa.
- No se usan AGP 9.4.x ni Gradle 9.8.x, que son más nuevos, hasta que Kotlin los certifique (TD-01 en `docs/tech-debt.md`).
- Cambiar cualquiera de estas versiones requiere un ADR nuevo.

## Fuentes

- https://docs.spring.io/spring-boot/system-requirements.html
- https://spring.io/projects/spring-boot
- https://developer.android.com/studio/releases
- https://developer.android.com/build/releases/agp-9-3-0-release-notes
- https://kotlinlang.org/docs/gradle-configure-project.html
- https://developer.android.com/develop/ui/compose/bom
- https://github.com/testcontainers/testcontainers-java/releases
- https://hub.docker.com/r/postgis/postgis
- https://neon.com/docs/extensions/pg-extensions
- https://spring.io/projects/spring-modulith
