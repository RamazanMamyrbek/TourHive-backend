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
    @DisplayName("Flyway schema history table should contain successful V1 migration")
    void flywayMigrationShouldApplySuccessfully() {
        Integer migrationCount = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success = true",
                Integer.class
        );

        assertThat(migrationCount).isNotNull().isEqualTo(1);
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
