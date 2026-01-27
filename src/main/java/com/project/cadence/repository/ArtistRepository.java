package com.project.cadence.repository;

import com.project.cadence.model.Artist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface ArtistRepository extends JpaRepository<Artist, String> {

    Page<Artist> findByNameStartingWithIgnoreCase(String trim, PageRequest pageable);

    @Query("""
    SELECT COUNT(DISTINCT ph.user.id)
    FROM PlayHistory ph
    JOIN ph.song s
    JOIN s.createdBy a
    WHERE a.id = :artistId
      AND ph.lastPlayedAt >= :fromDate
""")
    Long getTotalListenersForGivenDuration(
            String artistId,
            Instant fromDate
    );

}
