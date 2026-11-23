# ADR 0003 — Índice de Confort (RF-03)

- **Estado:** aceptado
- **PR:** #5
- **Decide:** AM

## Contexto

RF-03 pide un Índice de Confort de 0 a 100 basado en un promedio bayesiano con peso por recencia, que muestre etiqueta, cantidad de opiniones, antigüedad e incertidumbre, y cuya fórmula viva solo en la API y sea verificable. Cada opinión califica de 1 a 5 cuatro dimensiones: iluminación, afluencia de mujeres, visibilidad comercial y experiencia nocturna; la experiencia nocturna es opcional en visitas de día (ADR 0002). El índice refleja percepción comunitaria: nunca garantiza seguridad.

## Decisión: fórmula versión 1

1. **Peso por antigüedad.** Cada opinión pesa `w = 0,5 ^ (edad en días / 90)`: vida media de 90 días. Una opinión con fecha futura cuenta como recién hecha.
2. **Promedio bayesiano por dimensión** (escala 1–5): `score = (k·m + Σ wᵢ·xᵢ) / (k + Σ wᵢ)` con valor previo neutral `m = 3` y peso `k = 3`. Solo cuentan las opiniones que califican esa dimensión: una experiencia nocturna nula no cuenta como 0. La evidencia de la dimensión es `Σ wᵢ` de esas opiniones.
3. **Combinación.** Las cuatro dimensiones pesan 0,25. El peso de la experiencia nocturna se escala por la parte de la evidencia que la califica (`0,25 × evidencia nocturna / evidencia total`) y los pesos se renormalizan. Un lugar visitado solo de día se evalúa por sus tres dimensiones de día.
4. **Escala.** `índice = (combinado − 1) × 25`, redondeado a entero, mitad hacia arriba. Antes se redondea a 6 decimales para que el ruido de punto flotante no cambie el entero (74,5 → 75; 74,4 → 74).
5. **Sin opiniones** el índice es nulo, nunca 50.

### Parámetros

| Parámetro | Valor | Justificación |
|---|---|---|
| Vida media | 90 días | La iluminación, el comercio y la afluencia cambian por temporada; una opinión de un trimestre vale la mitad y una de un año, 0,06. Con menos, se perderían datos cuando hay pocas opiniones |
| Valor previo `m` | 3 | Punto medio de la escala: sin información el índice no se inclina. No se usa el promedio de la ciudad para que un lugar no herede la percepción de otros |
| Peso del previo `k` | 3 | Equivale a tres opiniones recientes: una sola opinión de 5 da 62, no 100 |
| Pesos de dimensión | 0,25 | Lo más neutral y explicable hasta tener evidencia para otra ponderación |

### Incertidumbre

- **Opiniones efectivas** `n_eff = Σ wᵢ` sobre todas las opiniones. Incertidumbre **alta** si `n_eff < 3`, **media** si `3 ≤ n_eff < 8`, **baja** si `n_eff ≥ 8`.
- **Dispersión:** desviación estándar ponderada del promedio de cada opinión (solo sus dimensiones presentes), en escala 1–5.
- **Sin datos nocturnos** (`nightDataMissing`): la evidencia nocturna es menor que 1.

### Etiquetas

Se calculan sobre el **índice ya redondeado**, para que coincidan con lo que se muestra, en este orden de prioridad:

1. **Pocas opiniones** (`FEW_OPINIONS`): `n_eff < 2` o sin opiniones.
2. **Opiniones mixtas** (`MIXED_OPINIONS`): dispersión ≥ 1,0, sea cual sea el promedio.
3. Por banda del índice: **Muy cómodo** (`VERY_COMFORTABLE`) ≥ 75; **Cómodo** (`COMFORTABLE`) 60–74; **Opiniones mixtas** 40–59; **Poco cómodo** (`UNCOMFORTABLE`) < 40.
4. **Tope sin datos nocturnos:** si `nightDataMissing` es verdadero, la etiqueta máxima es **Cómodo**, nunca Muy cómodo. El número del índice no cambia.

### API

- Detalle (`GET /api/v1/places/{id}`): bloque `comfort` con `index`, `label`, `uncertainty`, `effectiveReviews` (2 decimales), `dispersion` (2 decimales), `nightDataMissing`, `dimensions` (promedios bayesianos a 1 decimal; la nocturna es nula sin evidencia) y `formulaVersion`. Los promedios crudos siguen en `averages`.
- Búsqueda (`GET /api/v1/places`): cada resultado trae `comfortIndex` y `comfortLabel`, calculados para toda la página con una sola consulta de opiniones.
- Las etiquetas viajan como códigos en inglés; la app muestra el texto en español.

### Arquitectura

- `comfortindex.domain.ComfortIndexCalculator` es puro (sin Spring) y es el único lugar con la fórmula.
- `ComfortIndexService` (capa `application`) obtiene las sumas ponderadas por antigüedad de cada lugar con la API de solo lectura de `reviews`, que no incluye comentarios ni autoría, y calcula con un `Clock` inyectado. Las sumas se calculan en la base (ADR 0004).
- Los tipos del resultado (`ComfortIndex`, `ComfortLabel`, `Uncertainty`, `DimensionComfort`) están en el paquete base del módulo porque son su API pública; el dominio los produce directamente.
- Dependencias: `places` → `comfortindex` → `reviews`, y `places` → `reviews`, sin ciclos.
- El índice se calcula al leer: refleja la última opinión y el paso del tiempo, sin procesos programados.

## Consecuencias

- Cada lectura del detalle o de una página de búsqueda agrega en la base las opiniones de esos lugares, con una fila por lugar (ADR 0004).
- El mismo lugar cambia de índice con el paso del tiempo aunque no lleguen opiniones; por eso las pruebas usan un reloj fijo.
- Cambiar parámetros o reglas exige una nueva versión de fórmula (`formulaVersion`) y un ADR que la documente.
- Los ejemplos del diseño están en `ComfortIndexCalculatorTest` como pruebas de regresión.
