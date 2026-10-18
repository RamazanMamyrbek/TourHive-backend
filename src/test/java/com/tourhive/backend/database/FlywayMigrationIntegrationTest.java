package com.tourhive.backend.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
class FlywayMigrationIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Flyway schema history table should contain successful V1, V2, V3, and V4 migrations")
    void flywayMigrationShouldApplySuccessfully() {
        Integer v1Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class
        );
        Integer v2Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success = true",
                Integer.class
        );
        Integer v3Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '3' AND success = true",
                Integer.class
        );
        Integer v4Count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '4' AND success = true",
                Integer.class
        );

        assertThat(v1Count).isNotNull().isEqualTo(1);
        assertThat(v2Count).isNotNull().isEqualTo(1);
        assertThat(v3Count).isNotNull().isEqualTo(1);
        assertThat(v4Count).isNotNull().isEqualTo(1);
    }

    @Test
    @DisplayName("PostgreSQL uuid-ossp extension should be enabled by V1 migration")
    void uuidExtensionShouldBeInstalled() {
        String extName = jdbcTemplate.queryForObject(
                "SELECT extname FROM pg_extension WHERE extname = 'uuid-ossp'",
                String.class
        );

        assertThat(extName).isEqualTo("uuid-ossp");
    }
}
