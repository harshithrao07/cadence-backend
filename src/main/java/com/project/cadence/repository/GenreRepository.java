package com.project.cadence.repository;

import com.project.cadence.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GenreRepository extends JpaRepository<Genre, String> {
    boolean existsByType(String type);

    @Query("""
                SELECT g FROM Genre g
                WHERE LOWER(g.type) LIKE LOWER(CONCAT('%', :key, '%'))
            """)
    List<Genre> searchByKey(@Param("key") String key);
}
