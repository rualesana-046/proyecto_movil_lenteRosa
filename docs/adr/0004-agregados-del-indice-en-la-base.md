# ADR 0004 — Sumas del Índice de Confort calculadas en la base

- **Estado:** aceptado
- **PR:** #7
- **Decide:** el agente (decisión técnica delegada; no afecta a la seguridad ni a la privacidad)

## Contexto

Con el ADR 0003, cada lectura del índice traía a la API todas las opiniones de los lugares consultados y las recorría en memoria (TD-06). El costo crecía con la cantidad de opiniones por lugar.

## Decisión

1. El peso por antigüedad es exponencial (`0,5 ^ (edad / vida media)`), así que todo lo que necesita la fórmula se puede resumir, por lugar, en sumas ponderadas de tamaño fijo: suma de pesos, suma ponderada de cada dimensión, suma de pesos y suma ponderada de la experiencia nocturna, y sumas ponderadas del promedio de cada opinión y de su cuadrado (para la dispersión, como `sqrt(E[x²] − E[x]²)`).
2. Esas sumas las calcula PostgreSQL con una sola consulta agrupada por lugar (`ReviewQueries.decayedRatingSums`), que devuelve una fila por lugar sin importar cuántas opiniones tenga.
3. La consulta es una **estadística genérica** del módulo `reviews`: recibe el instante de referencia y la vida media como parámetros y no contiene el valor previo, los pesos de dimensión, los umbrales ni las etiquetas. La fórmula sigue viviendo solo en `comfortindex` (`ComfortIndexCalculator`, que ahora calcula a partir de `RatingAggregates`).
4. `RatingAggregates.from(muestras, ahora)` calcula las mismas sumas en memoria y es la **referencia**: `ComfortIndexSqlEquivalenceTest` comprueba, con 20 lugares y opiniones aleatorias (día, noche, antiguas y con fecha futura), que el índice calculado con las sumas de la base coincide con el de referencia.
5. Se elimina la API `ratingsForPlaces`, que ya no tiene usos.

## Consecuencias

- El costo de cada lectura deja de crecer en memoria con la cantidad de opiniones; la base agrega con un recorrido de las opiniones de los lugares pedidos.
- La fórmula (versión 1) no cambia: los ejemplos del ADR 0003 siguen pasando como pruebas de regresión.
- Si algún día se cambia la forma del peso por antigüedad y deja de ser exponencial, estas sumas ya no servirían y habría que revisar este ADR.
- TD-06 queda resuelta.
