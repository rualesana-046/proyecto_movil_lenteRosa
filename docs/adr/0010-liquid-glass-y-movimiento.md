# ADR 0010 — Liquid glass y movimiento (RNF-03)

- **Estado:** aceptado
- **PR:** #14
- **Decide:** AM (reglas: respetar "reducir movimiento", contraste AA con pruebas, atribución fuera de zonas translúcidas, nunca liquid glass en el Modo discreto); el agente (diseño técnico)

## Contexto

RNF-03 pide liquid glass y animaciones fuera del Modo discreto, aplicados a todas las pantallas. Compose no trae un desenfoque del contenido que queda detrás de una superficie. El texto sobre vidrio debe seguir cumpliendo WCAG AA (RNF-02), sin depender de lo que haya detrás.

## Decisión

### Vidrio

1. **Haze 2.1.0** (`dev.chrisbanes.haze:haze-glass`, versión estable con más de dos semanas). El contenido de cada pantalla se marca con `hazeSource`, y las barras y tarjetas usan `hazeGlass` con el estilo `GlassStyle.regular`, que Haze recomienda para superficies con texto.
2. **Contraste garantizado por un tinte:** cada superficie de vidrio lleva el color de superficie del tema al **90 %** de opacidad.
   - `GlassContrastTest` compone ese tinte sobre los dos extremos posibles, fondo negro y fondo blanco, y exige 4,5:1 para `onBackground`, `onSurfaceVariant` y `primary` en ambos temas.
   - Como la luminancia crece con cada canal, cualquier otro fondo queda entre esos dos extremos. La prueba también exige que el texto quede del mismo lado de la luminancia en ambos casos, para que ningún fondo intermedio pueda igualarla.
   - Al 80 % la prueba falló: el rosa principal quedó en 4,24:1 sobre fondo negro. Por eso se subió.
3. **El tinte se dibuja siempre debajo del efecto.** Si el dispositivo no puede renderizar el vidrio, Haze no dibuja nada: en el emulador de desarrollo pasó exactamente eso. Aun así, el tinte queda y el contraste se mantiene.
4. **Sobre el mapa no hay desenfoque.** La vista nativa de MapLibre no se puede capturar como fuente. Allí, la barra superior y las tarjetas usan el mismo tinte sin desenfoque, con el mismo contraste garantizado.
5. **Zonas de vidrio:**
   - barras superiores de todas las pantallas;
   - barra de navegación inferior;
   - tarjeta del lugar y avisos sobre el mapa.

   El contenido pasa por detrás de las barras, y los rellenos dejan libre su espacio al principio y al final de cada lista.
6. **Atribución fuera de las zonas translúcidas:** la franja de atribución es opaca y queda entre el mapa y la barra inferior. `MapAttributionLayoutTest` busca todas las superficies de vidrio (marca semántica `GlassSurface`) y falla si alguna se superpone a la atribución.
7. **Modo discreto y alto contraste:** `GlassPolicy` devuelve `OPAQUE` siempre en el Modo discreto (RF-08) y cuando está activo el "Texto de alto contraste" del sistema. En ambos casos las superficies son opacas. Además, la configuración de accesibilidad de Haze activa más contraste y bordes visibles.

### Movimiento

8. **`Motion`:** transiciones cortas (180–280 ms) con aceleración "emphasized decelerate". Se aplican a:
   - la tarjeta del lugar, que sube y se desvanece;
   - "Buscar en esta zona", que aparece con escala;
   - los avisos;
   - las respuestas que faltan en Aportar;
   - el fundido entre las pestañas Mapa y Lista;
   - el reacomodo de la lista.
9. **"Quitar animaciones":** si la escala de animación o de transición del sistema es 0, todo cambia sin animación. `MapMotionTest` comprueba en 50 ms, menos que la animación más corta, que la tarjeta ya está en su lugar final y que desaparece de inmediato. Con animaciones, la misma prueba falla, y así se confirmó que de verdad distingue los dos casos.
10. **Transiciones entre pantallas:** quedan para la migración a Navigation Compose (TD-09), que trae sus propias transiciones. Así no se hacen dos veces. Usarán los mismos tokens de `Motion`.

## Consecuencias

- El desenfoque real depende del dispositivo. En el emulador solo se vio el tinte, así que queda pendiente verlo en un teléfono real.
- Detrás de las barras se ve un reflejo tenue del contenido (10 %) cuando no hay desenfoque.
