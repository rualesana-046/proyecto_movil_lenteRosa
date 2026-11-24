package com.lenterosa.api.reviews.domain;

import java.util.regex.Pattern;

/**
 * Detecta datos de contacto en un comentario, para no publicar datos personales de nadie:
 * correos, enlaces y teléfonos.
 *
 * <p>Teléfono: una secuencia de 7 o más dígitos, opcionalmente con prefijo {@code +57} y con un
 * espacio, guion o punto entre dígitos. Las direcciones colombianas ("Calle 18 # 20-30",
 * "Carrera 27 #15-40") no se marcan: el {@code #} y los grupos cortos cortan la secuencia.
 * Limitación conocida: números de dirección seguidos sin palabra intermedia ("15-40 302") suman 7
 * dígitos y se marcan.
 */
public final class ContactDataDetector {

    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");

    private static final Pattern URL = Pattern.compile(
            "(?i)\\b(https?://|www\\.)\\S+"
                    + "|\\b[\\w-]+\\.(com|co|net|org|info|io|me|app|ly|gl|link|xyz|site|online)(\\.[a-z]{2})?\\b(/\\S*)?");

    // +57 opcional, luego al menos 7 dígitos con un separador opcional (espacio, guion o punto) entre ellos.
    private static final Pattern PHONE = Pattern.compile("(\\+\\s?57[\\s.-]?)?\\d(?:[\\s.-]?\\d){6,}");

    private ContactDataDetector() {
    }

    public static boolean containsContactData(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return EMAIL.matcher(text).find() || URL.matcher(text).find() || PHONE.matcher(text).find();
    }
}
