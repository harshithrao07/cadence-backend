package com.project.musicplayer.repository;

import com.project.musicplayer.model.ReleaseType;
import com.project.musicplayer.model.Releases;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Set;

public interface ReleaseRepository extends CrudRepository<Releases, String> {

    @Query(value = "SELECT * FROM releases WHERE id IN (SELECT release_id FROM artist_releases WHERE artist_id = :artistId) " +
            "AND (:releaseType IS NULL OR release_type = :releaseType)", nativeQuery = true)
    Set<Releases> findArtistReleasesByArtistId(
            @Param("artistId") String artistId,
            @Param("releaseType") ReleaseType releaseType);


    @Query(value = "SELECT * FROM releases WHERE id IN (SELECT release_id FROM artist_feature_releases WHERE artist_id = :artistId)", nativeQuery = true)
    Set<Releases> findFeatureReleasesByArtistId(@Param("artistId") String artistId);

}
