package com.project.musicplayer.repository;

import com.project.musicplayer.model.RecordType;
import com.project.musicplayer.model.Record;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface RecordRepository extends CrudRepository<Record, String> {
    boolean existsByTitle(String title);

    @Query(value = "SELECT * FROM record WHERE id IN (SELECT record_id FROM artist_records WHERE artist_id = :artistId) " +
            "AND (:recordType IS NULL OR record_type = :recordType)", nativeQuery = true)
    Set<Record> findArtistRecordsByArtistId(
            @Param("artistId") String artistId,
            @Param("recordType") String recordType);


    @Query(value = "SELECT * FROM record WHERE id IN (SELECT record_id FROM artist_featured_records WHERE artist_id = :artistId)", nativeQuery = true)
    Set<Record> findFeatureRecordsByArtistId(@Param("artistId") String artistId);

}
