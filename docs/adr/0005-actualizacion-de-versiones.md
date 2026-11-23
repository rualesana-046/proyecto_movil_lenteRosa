# ADR 0005 — Actualización de versiones de bibliotecas

- **Estado:** aceptado
- **PR:** #8
- **Decide:** el agente (decisión técnica delegada; no afecta a la seguridad ni a la privacidad)

## Contexto

El ADR 0001 fija las versiones y exige un ADR para cambiarlas. TD-01 pide subir AGP y Gradle cuando Kotlin los certifique. Se revisaron las versiones estables publicadas y la tabla de compatibilidad oficial de Kotlin.

## Decisión

| Componente | Antes | Ahora | Motivo |
|---|---|---|---|
| Spring Boot | 4.1.1 | **4.1.3** | Parches de la misma línea (Hibernate 7.4.15, driver de PostgreSQL 42.7.14); sin cambios de API |
| Spring Modulith | 2.1.1 | **2.1.3** | Parches de la línea compatible con Spring Boot 4.1 |
| lifecycle-runtime-ktx | 2.11.0 | **2.11.1** | Parche |
| AGP | 9.3.3 | 9.3.3 | Kotlin 2.4.21, la última versión estable, certifica hasta AGP 9.3.1 y Gradle 9.7.0. AGP 9.5.2 y Gradle 9.9.2 siguen fuera de lo certificado (TD-01) |
| Gradle | 9.7.0 | 9.7.0 | Igual que AGP |
| Compose BOM | 2026.09.00 | 2026.09.00 | 2026.11.01 (Compose 1.13) exige compilar contra `android-37.1`; el proyecto usa `android-37.0` y subirlo requiere instalar una plataforma nueva del SDK (TD-08) |

No se sube a Spring Boot 4.2 en esta actualización: es una línea nueva que exige Spring Modulith 2.2 y conviene evaluarla por separado (TD-07).

## Consecuencias

- `./gradlew build` en la API (167 pruebas) y `lint assembleDebug` en la app pasan con estas versiones.
- TD-01 sigue pendiente hasta que una versión estable de Kotlin certifique AGP ≥ 9.4 y Gradle ≥ 9.8.
- Se registran TD-07 (Spring Boot 4.2) y TD-08 (Compose 1.13 y `compileSdk` 37.1).
