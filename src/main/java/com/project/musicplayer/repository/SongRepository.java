package com.project.musicplayer.repository;

import com.project.musicplayer.model.Song;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface SongRepository extends CrudRepository<Song, String> {
    @Query(value = "SELECT * FROM song WHERE id IN ((SELECT song_id FROM artist_created_songs WHERE artist_id = :artistId) UNION (SELECT song_id FROM artist_featured_songs WHERE artist_id = :artistId)) " +
            "AND (:recordId IS NULL OR record_id = :recordId)", nativeQuery = true)
    Set<Song> getAllSongs(@Param("artistId") String artistId, @Param("recordId") String recordId);
}
