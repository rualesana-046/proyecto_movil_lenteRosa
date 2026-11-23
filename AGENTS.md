# AGENTS.md — Lente Rosa

Fuente única de instrucciones para cualquier agente de desarrollo (Claude Code u otro) que trabaje en este repositorio. `CLAUDE.md` solo importa este archivo.

## Proyecto

Lente Rosa es una app Android para mujeres que muestra cómo percibe la comunidad un lugar o una ruta, **sin rastreo continuo de ubicación**, más un modo discreto de ayuda rápida. El Índice de Confort refleja percepción comunitaria: nunca garantiza seguridad ni sustituye a los servicios de emergencia.

Proyecto académico de Ingeniería de Software. La responsable es AM; el agente trabaja **de forma autónoma** dentro de los límites de la sección "Permisos".

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

## Permisos

El agente trabaja de forma autónoma en: código, pruebas, documentación, commits pequeños, push a ramas de trabajo, apertura de Pull Requests y corrección del CI.

### Merge

- **Merge autónomo** con CI en verde para Pull Requests de documentación, deuda técnica, actualización de versiones, pruebas y refactor. Después del merge, el agente avisa a AM con un resumen corto.
- **Pull Requests de funcionalidades (RF):** el agente presenta el informe de 8 puntos y espera la aprobación de AM antes del merge.

### Decisiones técnicas y de diseño

- El agente elige su recomendación, la registra en un ADR y la destaca en el informe.
- Pregunta antes solo si la decisión afecta a la seguridad o la privacidad de las usuarias.
- Los cambios de producto o de alcance (requisitos, reglas de producto, funcionalidades) siempre se consultan con AM.

### Siempre requieren aprobación explícita de AM

El agente se detiene y pide un "sí" explícito de AM en el chat, para esa acción concreta, antes de:

- Crear, cambiar o borrar recursos en la nube (Google Cloud, Firebase, Neon) o cualquier cosa con facturación o costo.
- Crear, mover o usar secretos y credenciales.
- Borrar datos o aplicar migraciones destructivas.
- Hacer force push o cambiar el ruleset o la configuración de protección del repositorio.
- Instalar software en el equipo o cambiar configuración global (shell, git global, variables de entorno).

Una aprobación vale para una acción, no para las siguientes. Nunca afirmes que algo se probó si no se ejecutó.

## Redacción y atribución

- En documentación, comentarios de código, mensajes de commit, títulos y descripciones de Pull Request y nombres de rama solo se nombran funcionalidades (RF-01…, RNF-01…), módulos y decisiones técnicas. No se escriben días, plazos, tiempos ni entregas.
- Los commits no llevan líneas `Co-Authored-By` y las descripciones de Pull Request no llevan líneas de atribución a herramientas.

## Secretos

El repositorio es **público**. Ningún secreto entra nunca al historial.

- Nunca se versionan: `.env`, `local.properties`, `google-services.json`, llaves de firma (`*.jks`, `*.keystore`), `CLAUDE.local.md`, credenciales de nube.
- Los secretos llegan por variables de entorno o por el gestor de secretos de la nube, nunca escritos en el código ni en los archivos de configuración versionados.
- Antes de proponer un commit, revisa el diff buscando secretos.
- GitHub tiene activados el escaneo de secretos y el bloqueo de push con secretos.

## Git y commits

- `main` es estable y está protegida: todo cambio entra por Pull Request.
- Una rama por tarea, por ejemplo `feat/RF-03-indice`.
- Se trabaja con commits pequeños, uno por paso lógico (migración, dependencia, dominio de un módulo, adaptador, endpoint, documentación). Cada commit lleva sus propias pruebas.
- **Cada commit compila y pasa las pruebas por sí solo**: antes de cada commit se ejecuta el build de la parte afectada.
- Mensajes en formato Conventional Commits **con alcance**, en español: `tipo(alcance): descripción`. Tipos: `feat`, `fix`, `docs`, `test`, `build`, `ci`, `refactor`, `chore`. Alcances: el módulo (`places`, `reviews`, `comfortindex`, `insights`, `alerts`, `users`, `shared`), `db`, `api`, `app`, `ci`, `agents`, `tasks`, `tech-debt`, `adr`, `requirements`.
- Integración por Pull Request con CI en verde, según las reglas de merge de la sección "Permisos". El merge es **por rebase**, para que `main` conserve todos los commits de la rama.
- La hoja de ruta está en `docs/tasks.md`, por funcionalidad, con los estados: pendiente, en curso, bloqueada, en revisión, completada.

## Costos

Costo esperado: USD 0 al mes.

- Cloud Run (proyecto `lente-rosa-api`) es el único servicio con tarjeta: mínimo 0 y máximo 2 instancias, cobro por solicitud y alerta de presupuesto en USD 1. La alerta avisa, no frena.
- Firebase (`lente-rosa-app`) en plan Spark, sin facturación. Gemini Flash en capa gratuita, con plantillas de respaldo si se agota.
- Neon y GitHub Actions en planes gratuitos.
- El despliegue usa Workload Identity Federation, sin claves permanentes.
- Ningún proyecto, facturación ni despliegue se crea sin autorización de AM.

## Informe de 8 puntos

Antes de pedir el merge de un Pull Request de funcionalidad (RF), el agente presenta un informe en español con estos 8 puntos, en este orden. Para los Pull Requests de merge autónomo basta un resumen corto después del merge.

1. **Objetivo:** qué se intentó conseguir.
2. **Cambios:** archivos y funcionalidades creados o modificados.
3. **Decisiones:** qué se decidió y por qué.
4. **Pruebas:** comandos ejecutados y resultados reales.
5. **Riesgos o deuda técnica:** problemas pendientes y su prioridad.
6. **Git:** rama, commits, resumen del diff, Pull Request y estado del CI.
7. **Siguiente paso:** una tarea concreta.
8. **Aprobación requerida:** qué necesitas que AM autorice antes de continuar.
