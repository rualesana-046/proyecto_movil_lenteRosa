# ADR 0011 — Navigation Compose y bienvenida de tres pasos (RF-11, TD-09)

- **Estado:** aceptado
- **PR:** #15
- **Decide:** AM (textos de la bienvenida, pendientes de su aprobación en el PR); el agente (diseño técnico)

## Contexto

La app navegaba con una pila propia (ADR 0008), suficiente para cuatro pantallas. RF-11 agrega una bienvenida al primer uso, y las transiciones entre pantallas quedaron pendientes en ADR 0010. TD-09 pedía migrar a Navigation Compose antes de crecer más.

## Decisión

### Navegación

1. **Navigation Compose 2.10.2** (estable, de septiembre) con **rutas tipadas** (`@Serializable`): `Onboarding`, `Home`, `Account`, `Privacy`, `PlaceDetail(id)` y `Contribute(id, name)`. Reemplaza la pila propia y su codificación manual. Resuelve TD-09.
2. **ViewModels por pantalla:** cada ViewModel pertenece a su entrada de la pila (Hilt con `NavBackStackEntry`). Ya no hacen falta claves manuales por lugar.
3. **Transiciones entre pantallas** con los tokens de `Motion` (ADR 0010): al avanzar, la pantalla entra desde el final y, al volver, desde el inicio. Con "Quitar animaciones", sin animación.
4. **Resultado de Aportar:** al publicar, Aportar deja una marca en la pantalla anterior (`savedStateHandle`), y la ficha se recarga con el índice actualizado.
5. La pestaña del inicio (Mapa o Lista) se recuerda al ir a otras pantallas y volver.

### Bienvenida (RF-11)

6. **Solo al primer uso.** Una marca booleana en **DataStore Preferences 1.3.0** (estable, de octubre), en el archivo local `ajustes`. No guarda datos personales. Saltar la bienvenida también la marca como vista.
7. **La primera pantalla se decide una sola vez al abrir la app.** Mientras se lee la marca (un instante) solo se ve el fondo del tema.
8. **Al terminar o saltar,** se va al inicio y la bienvenida sale de la pila: "atrás" no vuelve a ella.
9. **Tres pasos** que se pueden deslizar. El avance se muestra en texto ("Paso 2 de 3") y con puntos, donde el actual es más ancho, para no depender solo del color. Hay un botón "Saltar" hasta el último paso. Con "Quitar animaciones", "Siguiente" cambia de paso sin desplazamiento.
10. **Textos propuestos** (decisión de producto de AM):
    1. *Cómo se siente un lugar:* qué mide la comunidad, y que es percepción, no garantía de seguridad ni reemplazo de los servicios de emergencia.
    2. *Tu privacidad primero:* sin seguimiento ni ubicación guardada, "Cerca de mí" bajo demanda y uso sin cuenta.
    3. *Tu opinión cuida a otras:* para opinar se necesita una cuenta con el correo verificado, y las opiniones son anónimas.
11. **Color de texto:** la bienvenida no está dentro de un Scaffold ni de una Surface, así que fija el color de texto del tema. Sin eso, el título salía negro en modo oscuro. `OnboardingTest` verifica el color con el que se dibuja el título.

## Consecuencias

- Las pantallas nuevas (guardados, Modo discreto) se agregan como rutas tipadas.
- DataStore queda disponible para otros ajustes locales, como los del Modo discreto.
