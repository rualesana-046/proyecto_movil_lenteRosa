# ADR 0007 — Aportar una opinión (RF-04)

- **Estado:** aceptado
- **PR:** #10
- **Decide:** AM (límites, privacidad de comentarios y reglas de producto); el agente (diseño técnico)

## Contexto

RF-04 pide que una usuaria autenticada valore un lugar, que la opinión se muestre anónima y que exista un límite contra abuso. El esquema de `reviews` (V3) ya admite `author_id` y `night_experience` nulo en visitas de día. ADR 0006 dejó pendiente exigir el correo verificado para opinar.

## Decisión

### Endpoint

1. `POST /api/v1/places/{id}/reviews` con `lighting`, `womenPresence`, `commercialVisibility` (obligatorias, de 1 a 5), `visitPeriod` (`DAY` o `NIGHT`), `nightExperience` (obligatoria si es `NIGHT`; nula si es `DAY`) y `comment` opcional.
2. Exige sesión (401) y el claim `email_verified` del token (403, "Verifica tu correo para poder opinar."). La autora es el `sub` del token; se guarda en `author_id` y **nunca** aparece en la respuesta.
3. Respuestas: 201 con `Location` y la opinión anónima; 404 si el lugar no existe; 400 (`reason` `INVALID` o `CONTACT_DATA`) con un mensaje claro; 429 con `Retry-After` en segundos, redondeado hacia arriba. JSON mal formado responde 400.
4. El Índice de Confort se calcula al leer (ADR 0003 y 0004), así que la opinión nueva se refleja en la siguiente consulta sin procesos adicionales.
5. No se editan ni se borran opiniones en RF-04. La ubicación de la persona no se pide.

### Límites contra abuso

6. Una opinión por autora, lugar y periodo (día o noche) cada 7 días, y como máximo 20 por autora en 24 horas. El `Retry-After` indica cuándo vuelve a estar permitido.
7. Las comprobaciones y el guardado ocurren en una sola transacción con un bloqueo consultivo por autora (`pg_advisory_xact_lock(hashtextextended(author_id, 0))`), para que dos envíos simultáneos no superen el límite. `ReviewSubmissionService` usa `@Transactional` por este motivo.

### Privacidad de los comentarios

8. Se rechazan comentarios con datos de contacto: correos, enlaces (`http(s)://`, `www.` o dominios comunes) y teléfonos, entendidos como secuencias de 7 o más dígitos separados opcionalmente por espacios, puntos o guiones, con `+57` opcional.
9. Las direcciones colombianas se permiten ("Calle 18 # 20-30", "Carrera 27 #15-40"): sus grupos de números son cortos.
10. **Limitación conocida:** una dirección seguida de un número sin texto intermedio (por ejemplo "15-40 302") suma 7 dígitos y se rechaza. El mensaje pide quitar datos de contacto, y basta escribir "apto 302" para que pase. Se acepta este falso positivo antes que dejar pasar teléfonos.

## Consecuencias

- Resuelve TD-05: las opiniones nuevas siempre llevan autora y al eliminar la cuenta quedan anónimas (ADR 0006).
- El detector es heurístico: no reemplaza la moderación si llega a hacer falta.
