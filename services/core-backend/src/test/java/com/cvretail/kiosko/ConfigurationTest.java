package com.cvretail.kiosko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.ClassPathResource;

/**
 * Unit test: resolves application.yml with only its own values, so it does not depend on the
 * environment of the machine running the tests and starts no Spring context.
 */
class ConfigurationTest {

    private MutablePropertySources sources(Map<String, Object> values) throws IOException {
        MutablePropertySources sources = new MutablePropertySources();
        if (!values.isEmpty()) {
            sources.addLast(new MapPropertySource("values", values));
        }
        for (PropertySource<?> source :
                new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml"))) {
            sources.addLast(source);
        }
        return sources;
    }

    @Test
    void missingDatabaseUrlFailsNamingTheVariable() throws IOException {
        var resolver = new PropertySourcesPropertyResolver(sources(Map.of()));

        assertThatThrownBy(() -> resolver.getRequiredProperty("spring.datasource.url"))
                .hasMessageContaining("DB_URL");
    }

    @Test
    void missingVendorFailsNamingTheVariable() throws IOException {
        var resolver = new PropertySourcesPropertyResolver(sources(Map.of()));

        assertThatThrownBy(() -> resolver.getRequiredProperty("spring.flyway.locations"))
                .hasMessageContaining("DB_VENDOR");
    }

    @Test
    void vendorSelectsTheMigrationFolder() throws IOException {
        var resolver = new PropertySourcesPropertyResolver(sources(Map.of("DB_VENDOR", "postgresql")));

        assertThat(resolver.getRequiredProperty("spring.flyway.locations"))
                .isEqualTo("classpath:db/migration/common,classpath:db/migration/postgresql");
    }

    @Test
    void onlyHealthIsExposed() throws IOException {
        var resolver = new PropertySourcesPropertyResolver(sources(Map.of()));

        assertThat(resolver.getRequiredProperty("management.endpoints.web.exposure.include"))
                .isEqualTo("health");
        assertThat(resolver.getRequiredProperty("management.endpoint.health.show-details"))
                .isEqualTo("never");
    }
}
