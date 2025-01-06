package com.project.cadence.repository;

import com.project.cadence.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, String> {
    boolean existsByType(String type);
}
