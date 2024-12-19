package com.project.musicplayer.repository;

import com.project.musicplayer.model.Artist;
import com.project.musicplayer.model.Releases;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.awt.print.Pageable;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, String> {

    Optional<Artist> findByName(String name);

    boolean existsByName(String name);
}
