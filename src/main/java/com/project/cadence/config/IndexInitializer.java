package com.project.cadence.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class IndexInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public IndexInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer count = jdbcTemplate.queryForObject("""
                    SELECT COUNT(1)
                    FROM information_schema.statistics
                    WHERE table_schema = DATABASE()
                      AND table_name = 'song'
                      AND index_name = 'idx_song_record_id'
                """, Integer.class);

        if (count != null && count == 0) {
            jdbcTemplate.execute("""
                        CREATE INDEX idx_song_record_id
                        ON song(record_id);
                    """);
        }
    }

}
