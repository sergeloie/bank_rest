package com.example.bankcards;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
class LiquibaseMigrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void personTableExists() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT COUNT(*) AS cnt FROM person");
        assertNotNull(result, "Table 'person' should exist");
    }

    @Test
    void cardTableExists() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT COUNT(*) AS cnt FROM card");
        assertNotNull(result, "Table 'card' should exist");
    }

    @Test
    void cardBlockRequestTableExists() {
        List<Map<String, Object>> result = jdbcTemplate.queryForList("SELECT COUNT(*) AS cnt FROM card_block_request");
        assertNotNull(result, "Table 'card_block_request' should exist");
    }

    @Test
    void adminUserExistsInPersonTable() {
        Map<String, Object> admin = jdbcTemplate.queryForMap(
                "SELECT name, role FROM person WHERE name = 'admin'"
        );
        assertEquals("admin", admin.get("name"));
        assertEquals("ADMIN", admin.get("role"));
    }
}
