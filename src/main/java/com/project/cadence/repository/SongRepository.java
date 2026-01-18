package com.project.cadence.repository;

import com.project.cadence.model.Artist;
import com.project.cadence.model.Record;
import com.project.cadence.model.Song;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SongRepository extends CrudRepository<Song, String> {
    @Query(value = "SELECT * FROM song WHERE id IN (SELECT song_id FROM artist_created_songs WHERE artist_id = :artistId) " +
            "AND (:recordId IS NULL OR record_id = :recordId)", nativeQuery = true)
    Set<Song> getAllSongs(@Param("artistId") String artistId, @Param("recordId") String recordId);

    @Query("""
    SELECT COALESCE(SUM(ph.playCount), 0)
    FROM PlayHistory ph
    WHERE ph.song.id = :songId
""")
    Long getTotalPlaysForSong(String songId);

    @Query("""
    SELECT DISTINCT s
    FROM Song s
    LEFT JOIN FETCH s.genres
    WHERE s.record.id = :recordId
    ORDER BY s.order
""")
    List<Song> findByRecordIdWithGenres(@Param("recordId") String recordId);

    @Query("""
    SELECT a
    FROM Song s
    JOIN s.createdBy a
    WHERE s.id = :songId
""")
    Set<Artist> findArtistsBySongId(@Param("songId") String songId);

    @Modifying
    @Query(
            value = "DELETE FROM artist_created_songs WHERE song_id = :songId",
            nativeQuery = true
    )
    void deleteSongArtists(@Param("songId") String songId);

    List<Song> findByRecordOrderByOrderAsc(Record record);
}
