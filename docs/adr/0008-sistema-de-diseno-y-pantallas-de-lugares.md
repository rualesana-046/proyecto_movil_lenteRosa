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
3. **Plus Jakarta Sans como fuente descargable** de Google Fonts (`ui-text-google-fonts`, versión del BOM de Compose). No se versionan archivos de fuente y el sistema la comparte entre apps. Si no está disponible (sin Google Play o sin conexión la primera vez), se usa la fuente del sistema y la app funciona igual. `font_certs.xml` contiene los certificados públicos del proveedor, tomados de los ejemplos oficiales de Android (Apache 2.0). Si más adelante hace falta la fuente sin Google Play, se evaluará incluirla en la app (licencia OFL).
4. **Escala de confort sin rojo:** un tono por etiqueta (verde azulado, verde, ocre, violeta y gris), siempre con texto en español e ícono propio, nunca solo color. La prueba verifica contraste AA, que ningún tono sea rojo y que cada etiqueta tenga texto, ícono y tono distintos. Una etiqueta desconocida se muestra como "Pocas opiniones", nunca como cómoda.
5. Íconos como vectores propios (trazos de Material Icons, Apache 2.0), sin la librería de íconos.
6. Liquid glass y animaciones quedan para un PR posterior (RNF-03).

### Pantallas

7. **Lista:** lugares alrededor del centro del piloto (Pasto, `PilotArea`), radio de 3 km, con búsqueda por nombre. El centro es solo un valor inicial: el código no se restringe por ciudad. No se pide la ubicación de la persona.
8. **Ficha:** índice con etiqueta e incertidumbre, aviso si faltan opiniones de noche, promedios por dimensión ("Sin datos" si son nulos, nunca 0), opiniones recientes y el aviso de que el índice no garantiza seguridad.
9. **Aportar:** las tres dimensiones y el periodo son obligatorios. De noche la experiencia nocturna es obligatoria. De día se ofrece "No fui de noche" y se envía `nightExperience` nulo. Sin sesión o sin correo verificado, la ficha lleva a la cuenta. La API valida de nuevo y la app muestra sus mensajes en español, incluido el tiempo de espera de `Retry-After`.
10. Al aportar se pide un ID token nuevo, por si la persona acaba de verificar el correo.
11. **Navegación** con una pila propia que se guarda con `rememberSaveable`, sin Navigation Compose: son cuatro pantallas. Se reevalúa con el mapa (RF-09) y el onboarding (RF-11).
12. `NetworkStatus` pasa al paquete `network`, compartido por la cuenta y los lugares.

## Consecuencias

- Las pantallas siguientes usan los mismos tokens, la tipografía y la insignia de confort.
- Sin Google Play, la app se ve con la fuente del sistema.
