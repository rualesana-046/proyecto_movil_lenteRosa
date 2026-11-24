# ADR 0008 — Sistema de diseño básico y pantallas de lugares (RF-01, RF-03, RF-04)

- **Estado:** aceptado
- **PR:** #11
- **Decide:** AM (alcance: tokens, tipografía y escala de confort desde ahora; liquid glass y animaciones después); el agente (diseño técnico)

## Contexto

La app tenía solo la cuenta (RF-02). Faltaban la lista de lugares, la ficha y la pantalla Aportar, sin mapa. Para no rehacer pantallas, se incluye ya lo básico del sistema de diseño (RNF-02, RNF-03).

## Decisión

### Sistema de diseño

1. **Tokens de color** en claro y oscuro: rosa (`primary`) y ciruela (`secondary`), definidos en `ui/theme/Color.kt`. Sin colores dinámicos del fondo de pantalla, para conservar el contraste verificado.
2. **Contraste AA verificado por pruebas:** `ColorContrastTest` calcula el contraste WCAG de cada pareja de texto y fondo (4,5:1 o más) en ambos temas.
3. **Plus Jakarta Sans incluida en la app** (decide AM): los cuatro pesos que usa la tipografía (Regular, Medium, SemiBold y Bold) como TTF estáticos en `res/font`, tomados del repositorio oficial `tokotype/PlusJakartaSans` (commit `18d1cd2`), unos 516 KB en total. La licencia SIL OFL 1.1 va en `assets/licenses/PlusJakartaSans-OFL.txt`, dentro del APK. Funciona sin conexión y sin Google Play; no se usa la descarga en línea de Google Fonts.
4. **Escala de confort sin rojo:** un tono por etiqueta (verde azulado, verde, ocre, violeta y gris), siempre con texto en español e ícono propio, nunca solo color. La prueba verifica contraste AA, que ningún tono sea rojo y que cada etiqueta tenga texto, ícono y tono distintos. Una etiqueta desconocida se muestra como "Pocas opiniones", nunca como cómoda.
5. Íconos como vectores propios (trazos de Material Icons, Apache 2.0), sin la librería de íconos.
6. Liquid glass y animaciones quedan para un PR posterior (RNF-03).

### Pantallas

7. **Lista:** lugares alrededor del centro del piloto (Pasto, `PilotArea`), radio de 3 km, con búsqueda por nombre. El centro es solo un valor inicial: el código no se restringe por ciudad. No se pide la ubicación de la persona.
8. **Ficha:** índice con etiqueta e incertidumbre, aviso si faltan opiniones de noche, promedios por dimensión ("Sin datos" si son nulos, nunca 0), opiniones recientes y el aviso de que el índice no garantiza seguridad.
9. **Aportar:** las tres dimensiones y el periodo son obligatorios. De noche la experiencia nocturna es obligatoria. De día se ofrece "No fui de noche" y se envía `nightExperience` nulo. Sin sesión o sin correo verificado, la ficha lleva a la cuenta. La API valida de nuevo y la app muestra sus mensajes en español, incluido el tiempo de espera de `Retry-After`.
10. Al aportar se pide un ID token nuevo, por si la persona acaba de verificar el correo.
11. **Navegación** con una pila propia que se guarda con `rememberSaveable`, sin Navigation Compose: son cuatro pantallas. La migración a Navigation Compose queda como deuda técnica (TD-09), para hacerla junto con el mapa (RF-09) y el onboarding (RF-11).
12. `NetworkStatus` pasa al paquete `network`, compartido por la cuenta y los lugares.

## Consecuencias

- Las pantallas siguientes usan los mismos tokens, la tipografía y la insignia de confort.
- La fuente aumenta el APK en unos 516 KB y se ve igual en cualquier dispositivo, con o sin conexión.
