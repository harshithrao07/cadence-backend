package com.project.cadence.repository;

import com.project.cadence.dto.song.TopSongsInArtistProfileDTO;
import com.project.cadence.model.Artist;
import com.project.cadence.model.Song;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SongRepository extends CrudRepository<Song, String> {
    @Query("""
    SELECT s.createdBy
    FROM Song s
    WHERE s.id = :songId
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

}
