# ADR 0015 — Cápsula de IA con el plan gratuito de Gemini (RF-06)

- **Estado:** aceptado
- **PR:** #19
- **Decide:** AM (plan, proyecto de la clave, protecciones, edad mínima y comentarios que se usan); el agente (diseño técnico, en el PR de RF-06)

## Contexto

RF-06 resume con Gemini los comentarios de un lugar. Los términos adicionales de la API de Gemini (vigentes desde el 23 de marzo de 2026) distinguen dos planes:

- **Plan gratuito:** Google usa lo que se envía y lo que responde el modelo para mejorar sus productos y sus modelos. Personas de Google pueden leer las entradas y salidas, después de separarlas de la cuenta y del proyecto. Los términos piden no enviar información personal, sensible ni confidencial.
- **Plan de pago:** Google no usa los datos para mejorar productos; los guarda un tiempo limitado solo para detectar abusos. Una solicitud es de pago si el proyecto de la clave tiene una cuenta de facturación activa.

Los términos también exigen 18 años o más, y no permiten usar la API en apps dirigidas a menores de 18 o que probablemente usen menores. Colombia está entre los países disponibles.

## Decisión

1. **Plan gratuito, válido solo para el piloto académico.** Lente Rosa no se publica en Play Store. Antes de cualquier lanzamiento público hay que pasar al plan de pago (TD-12).
2. **La clave vive en `lente-rosa-app`** (Firebase, plan Spark, sin facturación). Nunca se crea en un proyecto con facturación, porque eso la convertiría en una clave de pago. La clave solo la usa la API (regla del proyecto: nunca entra a la app). Se restringe a la Generative Language API.
3. **Envío mínimo:** solo el texto de cada comentario y si la visita fue de día o de noche. Nada de autora, cuenta, fecha, coordenadas ni nombre del lugar.
4. **Entre 3 y 20 comentarios:** con menos de 3 no se genera cápsula, para que un comentario aislado no pueda reconocerse. Se usan como máximo los 20 más recientes.
5. **Limpieza antes de enviar,** en la API: datos de contacto (correos, enlaces y teléfonos, como en el ADR 0007), direcciones, usuarios con @ y secuencias de números se reemplazan por marcas. Los nombres de personas no se pueden detectar de forma fiable con reglas: se usa una lista de nombres comunes, la instrucción al modelo de no repetir nombres y la validación del punto 6.
6. **Validación de la respuesta:** si la respuesta trae números de contacto, correos, enlaces, nombres o no cumple el formato, se descarta y se usa la plantilla de respaldo. La plantilla también se usa si la IA falla o se agota la cuota.
7. **«Resumen hecho con IA»:** la cápsula siempre lleva esa etiqueta y el aviso de que refleja percepción, no una garantía de seguridad.
8. **Solo al llegar una opinión nueva:** la cápsula se genera cuando se publica una opinión con comentario (reacción al evento, sin procesos programados) y el resultado se guarda. Consultar la ficha nunca llama a Gemini.
9. **Aviso claro** en Aportar (bajo el campo de comentario) y en «Tu privacidad»: el comentario se envía a Google Gemini para crear el resumen, Google puede usar el texto para mejorar sus productos, y no se deben escribir nombres ni datos que identifiquen a nadie.
10. **Solo comentarios escritos después del aviso nuevo.** Los comentarios anteriores se publicaron sin ese aviso y nunca se envían a Gemini.
11. **Solo para mayores de 18:** confirmación al crear la cuenta y aviso en «Tu privacidad».

## Consecuencias

- Las cápsulas solo aparecen cuando haya al menos 3 comentarios escritos después del aviso; los datos ficticios del seed se marcan como posteriores al aviso para la demostración.
- La cuota del plan gratuito es baja y Google no publica cifras: la plantilla de respaldo es parte normal del funcionamiento, no un caso raro.
- La detección de nombres es parcial; el aviso pide no escribirlos y la validación descarta las respuestas que los repiten.
- Si se lanza públicamente, este ADR se reemplaza por uno con el plan de pago (TD-12).
