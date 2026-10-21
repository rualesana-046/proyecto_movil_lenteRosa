# Lente Rosa

App Android que muestra cómo percibe la comunidad un lugar o una ruta, **sin rastreo continuo de ubicación**, más un modo discreto de ayuda rápida. El Índice de Confort refleja percepción comunitaria: nunca garantiza seguridad ni sustituye a los servicios de emergencia.

Proyecto académico de Ingeniería de Software.

## Estructura

| Carpeta | Contenido |
|---|---|
| `android-app/` | App Android: Kotlin, Jetpack Compose, Material 3, minSdk 26 |
| `backend-api/` | API: Java 21, Spring Boot 4.1, monolito modular con PostGIS |
| `docs/` | Requisitos, tareas, deuda técnica y decisiones de arquitectura (ADR) |
| `infra/` | PostGIS local con Docker Compose |
| `.github/workflows/` | Integración continua |

Las versiones exactas y su justificación están en [`docs/adr/0001-monorepo-y-versiones.md`](docs/adr/0001-monorepo-y-versiones.md).

## Requisitos

| Herramienta | Versión | Para qué |
|---|---|---|
| Git | cualquiera reciente | Clonar el repositorio |
| JDK | 21 | Compilar la API (Gradle lo exige con su toolchain) |
| Docker Desktop | reciente, en ejecución | Pruebas de la API (Testcontainers) y PostGIS local |
| Android Studio | Rabbit 1 (2026.2.1) o superior | SDK de Android (API 37) y emulador |

No hace falta instalar Gradle: cada parte trae su propio wrapper (`gradlew` / `gradlew.bat`).

Comprueba Java con `java -version` (debe decir 21).

## Instalación

```bash
git clone git@github.com:rualesana-046/proyecto_movil_lenteRosa.git
cd proyecto_movil_lenteRosa
```

Copia el archivo de variables de ejemplo y completa usuario y contraseña **locales** para el PostGIS de desarrollo. `.env` nunca se versiona.

- Mac: `cp .env.example .env`
- Windows (PowerShell): `Copy-Item .env.example .env`

### SDK de Android

Abre `android-app/` en Android Studio una vez: crea `android-app/local.properties` con la ruta del SDK (ese archivo no se versiona). También puedes crearlo a mano:

- Mac: `sdk.dir=/Users/<tu-usuario>/Library/Android/sdk`
- Windows: `sdk.dir=C\:\\Users\\<tu-usuario>\\AppData\\Local\\Android\\Sdk`

## Probar

### API

Con Docker Desktop en ejecución:

- Mac: `cd backend-api && ./gradlew test`
- Windows: `cd backend-api; .\gradlew.bat test`

Las pruebas levantan su propio PostGIS con Testcontainers (no necesitan `.env`) y verifican:

- `GET /api/v1/health`, `/api/v1/health/liveness` y `/api/v1/health/readiness` responden 200 con `status` `UP`, sin detalles.
- `/actuator/health` no está expuesto.
- Las migraciones habilitan PostGIS 3.6 y sus restricciones rechazan datos inválidos.
- La búsqueda por distancia, el detalle con promedios por dimensión y las opiniones anónimas, por HTTP real.
- Los parámetros fuera de los límites responden 400 con un mensaje claro.
- Los límites entre módulos (Spring Modulith) y las reglas de capas (ArchUnit).

La imagen `postgis/postgis` solo existe para amd64: en Macs con Apple Silicon corre emulada y la primera ejecución tarda más.

### App

- Mac: `cd android-app && ./gradlew lint assembleDebug`
- Windows: `cd android-app; .\gradlew.bat lint assembleDebug`

El APK queda en `android-app/app/build/outputs/apk/debug/app-debug.apk`.

## Ejecutar

### API con PostGIS local

1. Levanta PostGIS desde la raíz del repositorio y espera a que esté `healthy`:

   ```bash
   docker compose --env-file .env -f infra/docker-compose.yml up -d
   ```

   ```bash
   docker compose --env-file .env -f infra/docker-compose.yml ps
   ```

   Usa el puerto `5433` de este equipo (no el 5432), para convivir con un PostgreSQL local que ya esté instalado. La API apunta a `localhost:5433` por defecto.

2. Carga las variables de `.env` en la terminal (Spring no lee `.env` por sí solo) y arranca la API con el perfil `local`, que agrega datos **ficticios** de Pasto (8 lugares marcados `[FICTICIO]` y 6 opiniones):

   - Mac (zsh/bash):

     ```bash
     set -a; source .env; set +a
     cd backend-api && SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
     ```

   - Windows (PowerShell):

     ```powershell
     Get-Content .env | Where-Object { $_ -match '^[A-Z_]+=' } | ForEach-Object { $k, $v = $_ -split '=', 2; Set-Item "env:$k" $v }
     $env:SPRING_PROFILES_ACTIVE = "local"
     cd backend-api; .\gradlew.bat bootRun
     ```

   Usa siempre el perfil `local` con tu base local: si la arrancas una vez con él y después sin él, Flyway avisa que falta la migración del seed. Los datos ficticios nunca entran al jar que se despliega.

3. Comprueba la salud: `curl http://localhost:8080/api/v1/health` debe responder `{"groups":["liveness","readiness"],"status":"UP"}`.

4. Apaga PostGIS al terminar:

   ```bash
   docker compose --env-file .env -f infra/docker-compose.yml down
   ```

### Endpoints públicos (sin iniciar sesión)

| Método | Ruta | Qué devuelve |
|---|---|---|
| GET | `/api/v1/places?lat=&lng=&radiusMeters=&q=&page=&size=` | Lugares cercanos, del más cercano al más lejano, con `distanceMeters`. Radio de 1 a 5000 m (por defecto 1000); `size` de 1 a 50 (por defecto 20) |
| GET | `/api/v1/places/{id}` | Detalle con `reviewCount`, `lastReviewAt` y `averages` por dimensión (nulos si no hay datos) |
| GET | `/api/v1/places/{id}/reviews?page=&size=` | Opiniones anónimas, de la más reciente a la más antigua |
| GET | `/api/v1/health` | Salud de la API |

Ejemplo con los datos ficticios:

```bash
curl "http://localhost:8080/api/v1/places?lat=1.2136&lng=-77.2811&radiusMeters=1000"
```

Los errores siguen el formato RFC 9457 (`application/problem+json`): 400 con un mensaje claro si un parámetro está fuera de los límites, y 404 si el lugar no existe. Las coordenadas de la búsqueda no se guardan ni se escriben en los logs.

### App en el emulador

Abre `android-app/` en Android Studio, elige un emulador con API 37 (por ejemplo `Medium Phone API 37`) y pulsa **Run**. Desde la terminal, con el emulador encendido:

- Mac: `cd android-app && ./gradlew installDebug`
- Windows: `cd android-app; .\gradlew.bat installDebug`

## Forma de trabajo

Las reglas para personas y agentes (permisos, secretos, commits, costos, idioma del código y formato de informe) están en [`AGENTS.md`](AGENTS.md). Las tareas se siguen en [`docs/tasks.md`](docs/tasks.md) y la deuda técnica en [`docs/tech-debt.md`](docs/tech-debt.md).
