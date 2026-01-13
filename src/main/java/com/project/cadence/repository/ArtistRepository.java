package com.project.cadence.repository;

import com.project.cadence.dto.song.SongsInArtistProfileDTO;
import com.project.cadence.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, String> {

    boolean existsByName(String name);

    @Query("""
    SELECT new com.project.cadence.dto.song.SongsInArtistProfileDTO(
        s.id,
        s.title,
        s.totalDuration,
        s.coverUrl,
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
        s.coverUrl,
        r.id,
        r.title
    ORDER BY COALESCE(SUM(ph.playCount), 0) DESC
""")
    List<SongsInArtistProfileDTO> findTopSongsForArtist(
            String artistId,
            Pageable pageable
    );

    @Query("""
    SELECT COUNT(DISTINCT ph.user.id)
    FROM PlayHistory ph
    JOIN ph.song s
    JOIN s.createdBy a
    WHERE a.id = :artistId
      AND ph.lastPlayedAt >= :fromDate
""")
    Long getMonthlyListenersForArtist(
            String artistId,
            Instant fromDate
    );

    Page<Artist> findByNameContainingIgnoreCase(String trim, PageRequest pageable);
}
