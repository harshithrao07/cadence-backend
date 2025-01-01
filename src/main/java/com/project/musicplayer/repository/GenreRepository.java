package com.project.musicplayer.repository;

import com.project.musicplayer.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GenreRepository extends JpaRepository<Genre, String> {
    boolean existsByType(String type);
}
