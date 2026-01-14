package com.project.cadence.repository;

import com.project.cadence.model.Record;
import com.project.cadence.model.RecordType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.Set;

public interface RecordRepository extends CrudRepository<Record, String> {
    @Query("""
    SELECT DISTINCT r
    FROM Record r
    JOIN FETCH r.artists a
    WHERE a.id = :artistId
      AND (:recordType IS NULL OR r.recordType = :recordType)
""")
    Set<Record> findArtistRecordsByArtistId(
            @Param("artistId") String artistId,
            @Param("recordType") RecordType recordType
    );


    @Query("""
    SELECT r FROM Record r
    LEFT JOIN FETCH r.artists
    WHERE r.id = :recordId
""")
    Optional<Record> findByIdWithArtists(@Param("recordId") String recordId);


}
