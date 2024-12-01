package com.project.musicplayer.repository;

import com.project.musicplayer.model.Artist;
import com.project.musicplayer.model.Releases;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface ArtistRepository extends CrudRepository<Artist, String> {

    @Query(value = "SELECT * FROM releases WHERE id IN (SELECT release_id FROM song WHERE id IN (SELECT song_id from artist_featured_song WHERE artist_id = :artistId))", nativeQuery = true)
    Set<Releases> findFeatureReleasesByArtistId(@Param("artistId") String artistId);

    Optional<Artist> findByName(String name);

    boolean existsByName(String name);
}
