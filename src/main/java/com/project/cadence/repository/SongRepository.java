package com.project.cadence.repository;

import com.project.cadence.dto.song.SongBaseDTO;
import com.project.cadence.dto.song.TopSongsInArtistProfileDTO;
import com.project.cadence.model.Artist;
import com.project.cadence.model.Song;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface SongRepository extends CrudRepository<Song, String> {
    @Query("""
                SELECT a
                FROM Song s
                JOIN s.createdBy a
                WHERE s.id = :songId
                ORDER BY index(a)
            """)
    List<Artist> findCreatorsBySongId(@Param("songId") String songId);

    @Query("""
                SELECT new com.project.cadence.dto.song.TopSongsInArtistProfileDTO(
                    s.id,
                    s.title,
                    s.totalDuration,
                    r.coverUrl,
                    COALESCE(SUM(ph.playCount), 0),
                    r.id,
                    r.title
                )
                FROM Song s
                JOIN s.record r
                LEFT JOIN PlayHistory ph ON ph.song = s
                JOIN s.createdBy a
                WHERE a.id = :artistId
                GROUP BY
                    s.id,
                    s.title,
                    s.totalDuration,
                    r.coverUrl,
                    r.id,
                    r.title
                ORDER BY COALESCE(SUM(ph.playCount), 0) DESC
            """)
    Page<TopSongsInArtistProfileDTO> findTopSongsForArtist(
            @Param("artistId") String artistId,
            Pageable pageable
    );

    Page<Song> findByTitleContainingIgnoreCase(String searchKey, Pageable pageable);

    @Query("""
                SELECT new com.project.cadence.dto.song.SongBaseDTO(
                    s.id,
                    s.title,
                    s.totalDuration,
                    r.id,
                    r.title,
                    r.coverUrl
                )
                FROM PlayHistory ph
                JOIN ph.song s
                JOIN s.record r
                WHERE ph.lastPlayedAt >= :since
                GROUP BY s.id, s.title, s.totalDuration, r.id, r.title, r.coverUrl
                ORDER BY SUM(ph.playCount) DESC
            """)
    List<SongBaseDTO> findTrendingSongs(
            @Param("since") Instant since,
            Pageable pageable
    );

    @Query("""
                SELECT s.id,
                       new com.project.cadence.dto.artist.ArtistPreviewDTO(
                           a.id,
                           a.name,
                           a.profileUrl
                       )
                FROM Song s
                JOIN s.createdBy a
                WHERE s.id IN :songIds
            """)
    List<Object[]> findArtistsForSongs(@Param("songIds") List<String> songIds);

    @Query("""
                SELECT s.id,
                       new com.project.cadence.dto.genre.GenrePreviewDTO(
                           g.id,
                           g.type
                       )
                FROM Song s
                JOIN s.genres g
                WHERE s.id IN :songIds
            """)
    List<Object[]> findGenresForSongs(@Param("songIds") List<String> songIds);

    @Query("""
                SELECT g.id
                FROM PlayHistory ph
                JOIN ph.song s
                JOIN s.genres g
                WHERE ph.user.id = :userId
                GROUP BY g.id
                ORDER BY SUM(ph.playCount) DESC
            """)
    List<String> findTopGenresForUser(
            @Param("userId") String userId,
            Pageable pageable
    );

    @Query("""
                SELECT new com.project.cadence.dto.song.SongBaseDTO(
                    s.id,
                    s.title,
                    s.totalDuration,
                    r.id,
                    r.title,
                    r.coverUrl
                )
                FROM Song s
                JOIN s.record r
                JOIN s.genres g
                LEFT JOIN PlayHistory ph
                    ON ph.song = s AND ph.user.id = :userId
                WHERE g.id IN :genreIds
                  AND (ph.playCount IS NULL OR ph.playCount < 5)
                GROUP BY s.id, s.title, s.totalDuration, r.id, r.title, r.coverUrl
                ORDER BY SUM(COALESCE(ph.playCount, 0)) ASC
            """)
    List<SongBaseDTO> findRecommendedFromGenres(
            @Param("userId") String userId,
            @Param("genreIds") List<String> genreIds,
            Pageable pageable
    );

    @Query("""
                SELECT new com.project.cadence.dto.song.SongBaseDTO(
                    s.id,
                    s.title,
                    s.totalDuration,
                    r.id,
                    r.title,
                    r.coverUrl
                )
                FROM PlayHistory ph
                JOIN ph.song s
                JOIN s.record r
                WHERE ph.user.id = :userId
                ORDER BY ph.lastPlayedAt DESC
            """)
    List<SongBaseDTO> findRecentlyPlayedSongs(
            @Param("userId") String userId,
            Pageable pageable
    );

}
