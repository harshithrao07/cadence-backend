package com.project.cadence.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TriggerInitializer implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    public TriggerInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("DROP TRIGGER IF EXISTS follow_insert");

        jdbcTemplate.execute("""
            CREATE TRIGGER follow_insert
            AFTER INSERT ON artist_following
            FOR EACH ROW
            UPDATE artist
            SET followers_count = followers_count + 1
            WHERE id = NEW.artist_id;
        """);
    }
}
