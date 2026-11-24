# ADR 0010 — Liquid glass y movimiento (RNF-03)

- **Estado:** aceptado
- **PR:** #14
- **Decide:** AM (reglas: respetar "reducir movimiento", contraste AA con pruebas, atribución fuera de zonas translúcidas, nunca liquid glass en el Modo discreto); el agente (diseño técnico)

## Contexto

RNF-03 pide liquid glass y animaciones fuera del Modo discreto, aplicados a todas las pantallas. Compose no trae un desenfoque del contenido que queda detrás de una superficie. El texto sobre vidrio debe seguir cumpliendo WCAG AA (RNF-02), sin depender de lo que haya detrás.

## Decisión

### Vidrio

1. **Haze 2.1.0** (`dev.chrisbanes.haze:haze-glass`, versión estable con más de dos semanas). El contenido de cada pantalla se marca con `hazeSource`, y las barras y tarjetas usan `hazeGlass` con el estilo `GlassStyle.regular`, que Haze recomienda para superficies con texto.
2. **Los cuatro ingredientes del vidrio,** de abajo hacia arriba:
   - **Desenfoque con saturación:** Haze desenfoca el contenido de atrás y aumenta su saturación (`chromaMultiplier` 1,4) cuando el dispositivo lo soporta.
   - **Degradado translúcido:** el color de superficie del tema, al 95 % arriba y al 90 % abajo, con un 7 % de rosa principal en la parte baja.
   - **Resplandor interno:** una luz radial que nace en la esquina superior. Es blanca al 35 % en claro y rosa al 10 % en oscuro, para no aclarar el fondo del texto claro.
   - **Brillo de borde:** un filo de 1 dp, más intenso arriba (blanco al 75 % en claro y al 30 % en oscuro) y casi transparente abajo.
3. **Contraste garantizado:**
   - `GlassContrastTest` compone cada extremo del degradado, con y sin el resplandor en su punto máximo, sobre fondo negro y blanco. Exige 4,5:1 para `onBackground`, `onSurfaceVariant` y `primary` en ambos temas.
   - Como la luminancia crece con cada canal, cualquier otro fondo queda entre esos extremos. La prueba también exige que el texto quede siempre del mismo lado de la luminancia.
   - Con una opacidad del 80 % la prueba falló: el rosa principal quedó en 4,24:1 sobre negro. Por eso el mínimo es 90 %.
   - El brillo de borde va solo en el filo, donde no hay texto.
   - **El degradado se dibuja encima del desenfoque.** Si el dispositivo no puede renderizar el vidrio, Haze no dibuja nada, y en el emulador pasó exactamente eso. Aun así, el degradado queda y el contraste no cambia.
4. **Sobre el mapa no hay desenfoque.** La vista nativa de MapLibre no se puede capturar como fuente. Allí, la barra superior y las tarjetas usan el mismo tinte sin desenfoque, con el mismo contraste garantizado.
5. **Zonas de vidrio** (con los cuatro ingredientes; sobre el mapa, sin el desenfoque):
   - barras superiores de todas las pantallas;
   - barra de navegación inferior;
   - tarjeta del lugar y avisos sobre el mapa.

   El contenido pasa por detrás de las barras, y los rellenos dejan libre su espacio al principio y al final de cada lista.
6. **Atribución fuera de las zonas translúcidas:** la franja de atribución es opaca y queda entre el mapa y la barra inferior. `MapAttributionLayoutTest` busca todas las superficies de vidrio (marca semántica `GlassSurface`) y falla si alguna se superpone a la atribución.
7. **Modo discreto y alto contraste:** `GlassPolicy` devuelve `OPAQUE` siempre en el Modo discreto (RF-08) y cuando está activo el "Texto de alto contraste" del sistema. En ambos casos las superficies son opacas. Además, la configuración de accesibilidad de Haze activa más contraste y bordes visibles.

### Mapa oscuro

8. Con el tema oscuro, el mapa usa el estilo `dark` de OpenFreeMap: un mapa claro de noche deslumbra y llama la atención en la calle. Es el mismo proveedor y tiene la misma atribución. Se configura igual que el claro, con `lenteRosa.mapStyleUrlDark`. Al cambiar el tema, el estilo se recarga con los marcadores.

### Movimiento

9. **`Motion`:** transiciones cortas (180–280 ms) con aceleración "emphasized decelerate". Se aplican a:
   - la tarjeta del lugar, que sube y se desvanece;
   - "Buscar en esta zona", que aparece con escala;
   - los avisos;
   - las respuestas que faltan en Aportar;
   - el fundido entre las pestañas Mapa y Lista;
   - el reacomodo de la lista.
10. **"Quitar animaciones":** si la escala de animación o de transición del sistema es 0, todo cambia sin animación. `MapMotionTest` comprueba en 50 ms, menos que la animación más corta, que la tarjeta ya está en su lugar final y que desaparece de inmediato. Con animaciones, la misma prueba falla, y así se confirmó que de verdad distingue los dos casos.
11. **Transiciones entre pantallas:** quedan para la migración a Navigation Compose (TD-09), que trae sus propias transiciones. Así no se hacen dos veces. Usarán los mismos tokens de `Motion`.

## Consecuencias

- El desenfoque real depende del dispositivo. En el emulador solo se vio el tinte, así que queda pendiente verlo en un teléfono real.
- Detrás de las barras se ve un reflejo tenue del contenido (5–10 %) cuando no hay desenfoque.
