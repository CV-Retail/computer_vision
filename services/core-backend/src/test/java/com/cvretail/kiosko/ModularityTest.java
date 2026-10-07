package com.cvretail.kiosko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

/** Unit test: static analysis of the compiled classes, no Spring context. */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(KioskoApplication.class);

    @Test
    void hasExactlyTheThreeModules() {
        Set<String> names = modules.stream()
                .map(module -> module.getIdentifier().toString())
                .collect(Collectors.toSet());

        assertThat(names).containsExactlyInAnyOrder("campaigns", "rules", "reports");
    }

    @Test
    void moduleStructureIsValid() {
        modules.verify();
    }
}
