# ADR 0002 — Modelo de datos y persistencia de lugares y opiniones

- **Estado:** aceptado
- **Fecha:** 2026-10-21
- **Decide:** AM

## Contexto

El día 2 implementa la consulta como invitada (RF-01): buscar lugares cercanos, ver el detalle de un lugar y leer sus opiniones anónimas. Según el prototipo de la Fase 1, cada opinión califica cuatro dimensiones de 1 a 5, indica si la visita fue de día o de noche y puede llevar un comentario. Las búsquedas son geográficas y la API es un monolito modular con capas `domain`, `application` e `infrastructure` (ADR 0001).

## Decisión

### Modelo de datos

1. **`places`** (V2): `id uuid`, `name`, `category` (lista cerrada: `TRANSPORT`, `PARK`, `COMMERCE`, `EDUCATION`, `HEALTH`, `NIGHTLIFE`, `STREET`, `OTHER`), `address`, `location geography(Point, 4326)` con índice GiST y `created_at`.
2. **`reviews`** (V3): `lighting`, `women_presence` y `commercial_visibility` obligatorias de 1 a 5; `night_experience` de 1 a 5, **obligatoria si `visit_period = 'NIGHT'` y opcional si es `'DAY'`** ("No fui de noche"); `comment` opcional de hasta 500 caracteres; `author_id` nulo hasta el día 4 y nunca expuesto.
3. Las reglas se validan dos veces: en el dominio y con restricciones `CHECK` en la base, para que ni una escritura directa deje datos inválidos.
4. Identificadores **UUID v7** (RFC 9562) generados por la API (`shared.UuidV7`, con `SecureRandom`), ordenables por tiempo.
5. Los promedios por dimensión se redondean a un decimal y son **nulos cuando no hay datos**; el promedio nocturno ignora las opiniones que no lo califican.

### Persistencia híbrida

6. **JPA + Hibernate Spatial** para guardar y leer por identificador. Flyway es el único dueño del esquema (`ddl-auto: none`).
7. **SQL nativo de PostGIS solo para la búsqueda por distancia**, con `JdbcClient` (no consultas nativas de JPA): `ST_DWithin` sobre el índice GiST y `ST_Distance` en metros, con todos los valores como parámetros con nombre y `%`, `_` y `\` escapados en la búsqueda por nombre. Es más directo de mapear (la distancia no es una columna de la entidad) y queda igual detrás del puerto `PlaceRepository`.
8. La conversión entre `GeoPoint` (latitud, longitud) y JTS/PostGIS (x = longitud, y = latitud) vive en un único lugar (`PlaceEntity`), con una prueba contra PostGIS que verifica que Pasto no queda invertido.

### Decisiones conscientes de diseño

9. **Casos de uso sin anotaciones de Spring.** `ReviewQueryService` y `PlaceQueryService` son clases normales; cada módulo los registra con una `@Configuration` en su capa `infrastructure`. Así la capa `application` no depende del contenedor y cada commit compila y arranca aunque el adaptador llegue en un commit posterior.
10. **Un solo `PlaceQueryService`** para buscar, ver el detalle y listar opiniones, en lugar de una clase por caso de uso. Son tres lecturas pequeñas sobre los mismos puertos; si crecen, se separan.
11. **El listado de opiniones de un lugar vive en `places`.** Debe responder 404 si el lugar no existe; si estuviera en `reviews`, ese módulo dependería de `places`, que ya depende de `reviews` para las estadísticas, y habría un ciclo. `reviews` expone una API pública de solo lectura (`ReviewQueries`) que nunca incluye datos de la autora.
12. **`Page` y `Pageable` de Spring Data en los puertos de `application`**, como concesión pragmática: evitan duplicar paginación y conversiones. La alternativa es un tipo propio (`PageResult`) en `shared`; se adoptaría si la capa `application` tuviera que quedar libre de Spring por completo. ArchUnit solo prohíbe Spring y JPA en `domain`.

### API

13. Endpoints públicos `GET /api/v1/places`, `/api/v1/places/{id}` y `/api/v1/places/{id}/reviews`, con una forma JSON propia para las páginas (`items`, `page`, `size`, `totalItems`, `totalPages`).
14. Los parámetros fuera de los límites (radio > 5000 m, `size` > 50, `page` < 0, latitud o longitud fuera de rango, incluidos `NaN` e `Infinity`) **responden 400 con `ProblemDetail` (RFC 9457) y un mensaje en lenguaje cotidiano; nunca se recortan en silencio.** Un lugar inexistente responde 404.
15. Las coordenadas de la búsqueda se usan solo para calcular: no se guardan ni se escriben en los logs (RNF-01), y una prueba lo verifica.

### Datos de ejemplo

16. Datos **ficticios** de Pasto (`db/seed`, todos con nombres `[FICTICIO]`) cargados solo con el perfil `local`. Se excluyen de los jar que se despliegan.

## Consecuencias

- La base protege las reglas aunque se escriba directo en ella, a costa de repetir las reglas en dos lugares; las pruebas de `MigrationConstraintsTest` y del dominio las mantienen alineadas.
- Las consultas geográficas dependen de PostGIS: las pruebas usan PostGIS real con Testcontainers (un único contenedor compartido).
- Al llegar la autenticación (día 4) queda pendiente exigir `author_id` en las opiniones nuevas y anonimizar las de cuentas eliminadas (TD-05).
- El Índice de Confort (día 3) se agregará al detalle sin cambiar el modelo de opiniones.
