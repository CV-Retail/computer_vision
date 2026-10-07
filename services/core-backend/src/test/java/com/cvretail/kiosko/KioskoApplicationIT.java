package com.cvretail.kiosko;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/** Integration test: the context starts on embedded H2 and Flyway applies the baseline. */
@SpringBootTest
@ActiveProfiles("test")
class KioskoApplicationIT {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void contextStartsAndAppliesOnlyTheBaselineMigration() {
        // Flyway also records a schema-creation row without version; only versioned migrations count.
        List<Map<String, Object>> history = jdbc.queryForList(
                "SELECT \"version\", \"success\" FROM \"flyway_schema_history\" "
                        + "WHERE \"version\" IS NOT NULL ORDER BY \"installed_rank\"");

        assertThat(history).hasSize(1);
        assertThat(history.get(0).get("version")).isEqualTo("1");
        assertThat(history.get(0).get("success")).isEqualTo(true);
    }
}
