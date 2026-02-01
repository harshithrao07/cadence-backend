package com.project.cadence.repository;

import com.project.cadence.model.PlayHistory;
import com.project.cadence.model.PlayHistoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlayHistoryRepository extends JpaRepository<PlayHistory, PlayHistoryId> {
    Optional<PlayHistory> findByUserIdAndSongId(String userId, String songId);
}
