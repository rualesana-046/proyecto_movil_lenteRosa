package com.lenterosa.api.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UuidV7Test {

    private static final long FIXED_MILLIS = 1_760_918_400_000L; // 2025-10-20T00:00:00Z

    @Test
    @DisplayName("La versión es 7 y la variante es 2 (RFC 9562)")
    void hasVersionSevenAndRfcVariant() {
        for (int i = 0; i < 1_000; i++) {
            UUID uuid = UuidV7.next();

            assertThat(uuid.version()).isEqualTo(7);
            assertThat(uuid.variant()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("Los 48 bits altos guardan los milisegundos Unix del reloj")
    void embedsUnixMillisTimestamp() {
        UuidV7 generator = new UuidV7(Clock.fixed(Instant.ofEpochMilli(FIXED_MILLIS), ZoneOffset.UTC), new SecureRandom());

        UUID uuid = generator.generate();

        assertThat(uuid.getMostSignificantBits() >>> 16).isEqualTo(FIXED_MILLIS);
    }

    @Test
    @DisplayName("Una secuencia queda en orden estricto con el reloj del sistema")
    void sequenceIsStrictlyIncreasing() {
        assertStrictlyIncreasing(generate(UuidV7::next, 10_000));
    }

    @Test
    @DisplayName("Dentro del mismo milisegundo el contador mantiene el orden, aunque se agote")
    void sameMillisecondKeepsOrderBeyondCounterCapacity() {
        UuidV7 generator = new UuidV7(Clock.fixed(Instant.ofEpochMilli(FIXED_MILLIS), ZoneOffset.UTC), new SecureRandom());

        // 10 000 supera los 4096 valores del contador de 12 bits: obliga a avanzar el timestamp.
        assertStrictlyIncreasing(generate(generator::generate, 10_000));
    }

    @Test
    @DisplayName("Si el reloj retrocede, el orden se mantiene")
    void clockGoingBackwardsKeepsOrder() {
        AtomicLong millis = new AtomicLong(FIXED_MILLIS);
        Clock clock = new Clock() {
            @Override
            public ZoneOffset getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return Instant.ofEpochMilli(millis.get());
            }
        };
        UuidV7 generator = new UuidV7(clock, new SecureRandom());

        UUID before = generator.generate();
        millis.addAndGet(-5_000);
        UUID after = generator.generate();

        assertStrictlyIncreasing(List.of(before, after));
    }

    private static List<UUID> generate(Supplier<UUID> supplier, int count) {
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(supplier.get());
        }
        return uuids;
    }

    // Compara como enteros de 128 bits sin signo, que es el orden de los bytes en la base.
    private static void assertStrictlyIncreasing(List<UUID> uuids) {
        for (int i = 1; i < uuids.size(); i++) {
            UUID previous = uuids.get(i - 1);
            UUID current = uuids.get(i);
            int high = Long.compareUnsigned(previous.getMostSignificantBits(), current.getMostSignificantBits());
            int comparison = high != 0
                    ? high
                    : Long.compareUnsigned(previous.getLeastSignificantBits(), current.getLeastSignificantBits());
            assertThat(comparison).as("UUID %d debe ser mayor que el %d", i, i - 1).isNegative();
        }
    }
}
