package com.lenterosa.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(LenteRosaApiApplication.class);

    @Test
    @DisplayName("Los módulos solo se usan entre sí a través de su API pública y sin ciclos")
    void modulesRespectBoundaries() {
        modules.verify();
    }
}
