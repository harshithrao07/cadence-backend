package com.project.cadence.repository;

import com.project.cadence.model.Record;
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

}
