package com.project.cadence.repository;

import com.project.cadence.model.Artist;
import com.project.cadence.model.Record;
import com.project.cadence.model.Song;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecordRepository extends CrudRepository<Record, String> {
    List<Record> findByArtistsOrderByReleaseTimestampDesc(Artist artist, Pageable of);

    @Query("""
    SELECT s
    FROM Record r
    JOIN r.songs s
    WHERE r.id = :recordId
    ORDER BY INDEX(s)
""")
    List<Song> getAllSongsByRecordId(@Param("recordId") String recordId);

}
