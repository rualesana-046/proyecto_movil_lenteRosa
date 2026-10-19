# AGENTS.md — Lente Rosa

Fuente única de instrucciones para cualquier agente de desarrollo (Claude Code u otro) que trabaje en este repositorio. `CLAUDE.md` solo importa este archivo.

## Proyecto

Lente Rosa es una app Android para mujeres que muestra cómo percibe la comunidad un lugar o una ruta, **sin rastreo continuo de ubicación**, más un modo discreto de ayuda rápida. El Índice de Confort refleja percepción comunitaria: nunca garantiza seguridad ni sustituye a los servicios de emergencia.

Proyecto académico de Ingeniería de Software. La responsable es AM; el agente trabaja **supervisado**.

## Estructura del monorepo

| Carpeta | Contenido |
|---|---|
| `android-app/` | App Android: Kotlin, Jetpack Compose, Material 3, minSdk 26 |
| `backend-api/` | API: Java 21, Spring Boot 4.1, monolito modular |
| `docs/` | Requisitos, tareas, deuda técnica y ADR |
| `infra/` | Entorno local (Docker Compose) y, más adelante, despliegue |
| `.github/workflows/` | CI con GitHub Actions |

La API es un monolito modular con los módulos `places`, `reviews`, `comfortindex`, `insights`, `alerts` y `users` (paquetes de primer nivel bajo `com.lenterosa.api`). Cada módulo tiene las capas `domain`, `application` e `infrastructure`:

- `domain` no depende de `application`, de `infrastructure` ni de Spring.
- `application` no depende de `infrastructure`.
- Un módulo solo usa la API pública de otro módulo. Spring Modulith (`ModularityTest`) y ArchUnit (`LayeredArchitectureTest`) lo verifican en las pruebas.

Reglas de producto que no se negocian:

- La fórmula del Índice de Confort vive solo en la API.
- La clave de IA (Gemini) nunca entra a la app; solo la API llama a la IA.
- El recálculo y las alertas reaccionan a cada nueva opinión, sin procesos programados.
- La ubicación se pide solo bajo demanda.

## Idioma del código

- Todo el código va en **inglés**: clases, métodos, variables, paquetes, nombres de archivos de código y de migraciones (por ejemplo `V1__enable_postgis.sql`).
- Solo los **comentarios** (incluido Javadoc/KDoc) van en español.
- Para describir pruebas en español se usa `@DisplayName`, nunca el nombre del método.
- La documentación (`README.md`, `docs/`) y los mensajes de commit van en español.

## Comandos

```bash
# API (necesita Docker en ejecución para Testcontainers)
cd backend-api && ./gradlew build

# App Android
cd android-app && ./gradlew lint assembleDebug
```

Las versiones están fijadas en `docs/adr/0001-monorepo-y-versiones.md`. No las cambies sin un ADR nuevo.

## Permisos: qué necesita aprobación explícita de AM

El agente **no** hace nada de lo siguiente sin un "sí" explícito de AM en el chat, para esa acción concreta:

- Crear, mover o borrar archivos fuera de lo que la tarea aprobada pide.
- `git commit`, `git push`, crear ramas remotas, abrir o fusionar Pull Requests.
- Despliegues, creación o cambio de recursos en la nube (Google Cloud, Firebase, Neon, GitHub) y cualquier acción que tenga costo.
- Instalar software en el equipo o cambiar configuración global (shell, git global, variables de entorno).

Una aprobación vale para una acción, no para las siguientes. Nunca afirmes que algo se probó si no se ejecutó.

## Secretos

El repositorio es **público**. Ningún secreto entra nunca al historial.

- Nunca se versionan: `.env`, `local.properties`, `google-services.json`, llaves de firma (`*.jks`, `*.keystore`), `CLAUDE.local.md`, credenciales de nube.
- Los secretos llegan por variables de entorno o por el gestor de secretos de la nube, nunca escritos en el código ni en los archivos de configuración versionados.
- Antes de proponer un commit, revisa el diff buscando secretos.
- GitHub tiene activados el escaneo de secretos y el bloqueo de push con secretos.

## Git y commits

- `main` es estable. Una rama por tarea, por ejemplo `feat/RF-03-indice`.
- Commits pequeños en formato convencional (`feat:`, `fix:`, `docs:`, `test:`, `chore:`, `ci:`, `refactor:`), en español.
- Antes de cada commit: diff y pruebas revisados por AM.
- Integración por Pull Request con CI en verde y aprobación de AM antes del merge.
- Las tareas se siguen en `docs/tasks.md` con los estados: pendiente, en curso, bloqueada, en revisión, completada.

## Costos

Costo esperado: USD 0 al mes.

- Cloud Run (proyecto `lente-rosa-api`) es el único servicio con tarjeta: mínimo 0 y máximo 2 instancias, cobro por solicitud y alerta de presupuesto en USD 1. La alerta avisa, no frena.
- Firebase (`lente-rosa-app`) en plan Spark, sin facturación. Gemini Flash en capa gratuita, con plantillas de respaldo si se agota.
- Neon y GitHub Actions en planes gratuitos.
- El despliegue usa Workload Identity Federation, sin claves permanentes.
- Ningún proyecto, facturación ni despliegue se crea sin autorización de AM.

## Informe obligatorio al terminar cada tarea

Al terminar cada tarea, el agente entrega un informe en español con estos 8 puntos, en este orden:

1. **Objetivo:** qué se intentó conseguir.
2. **Cambios:** archivos y funcionalidades creados o modificados.
3. **Decisiones:** qué se decidió y por qué.
4. **Pruebas:** comandos ejecutados y resultados reales.
5. **Riesgos o deuda técnica:** problemas pendientes y su prioridad.
6. **Git:** diff, propuesta de commit y estado actual, sin ejecutar acciones no autorizadas.
7. **Siguiente paso:** una tarea concreta.
8. **Aprobación requerida:** qué necesitas que AM autorice antes de continuar.
