package com.lenterosa.api.shared;

import java.security.SecureRandom;
import java.time.Clock;
import java.util.UUID;

/**
 * Generador de UUID versión 7 según el RFC 9562.
 *
 * <p>Estructura (128 bits): 48 bits de milisegundos Unix, 4 bits de versión ({@code 0111}),
 * 12 bits de contador ({@code rand_a}), 2 bits de variante ({@code 10}) y 62 bits aleatorios
 * de {@link SecureRandom}.
 *
 * <p>Orden: dentro del mismo milisegundo se incrementa el contador (RFC 9562, sección 6.2,
 * método 1), así que una secuencia generada por este objeto queda en orden estricto. Si el
 * contador se agota o el reloj retrocede, se avanza el timestamp en vez de repetir o retroceder.
 */
public final class UuidV7 {

    private static final UuidV7 DEFAULT = new UuidV7(Clock.systemUTC(), new SecureRandom());

    private static final long TIMESTAMP_MASK = 0xFFFF_FFFF_FFFFL;
    private static final int MAX_COUNTER = 0xFFF;
    // El contador arranca en la mitad baja para dejar margen de incrementos en el mismo milisegundo.
    private static final int COUNTER_SEED_BOUND = 1 << 11;

    private final Clock clock;
    private final SecureRandom random;
    private long lastTimestamp = -1;
    private int counter;

    UuidV7(Clock clock, SecureRandom random) {
        this.clock = clock;
        this.random = random;
    }

    /** Genera un UUID v7 con el reloj del sistema. */
    public static UUID next() {
        return DEFAULT.generate();
    }

    synchronized UUID generate() {
        long now = clock.millis();
        if (now > lastTimestamp) {
            lastTimestamp = now;
            counter = random.nextInt(COUNTER_SEED_BOUND);
        } else if (counter < MAX_COUNTER) {
            counter++;
        } else {
            lastTimestamp++;
            counter = random.nextInt(COUNTER_SEED_BOUND);
        }

        long mostSignificantBits = ((lastTimestamp & TIMESTAMP_MASK) << 16) | 0x7000L | counter;
        long leastSignificantBits = (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL) | 0x8000_0000_0000_0000L;
        return new UUID(mostSignificantBits, leastSignificantBits);
    }
}
