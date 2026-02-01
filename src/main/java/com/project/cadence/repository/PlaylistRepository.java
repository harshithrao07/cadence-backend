package com.project.cadence.repository;

import com.project.cadence.model.Playlist;
import com.project.cadence.model.PlaylistVisibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends CrudRepository<Playlist, String> {
    Optional<Playlist> findByIdAndOwnerEmail(String s, String email);

    List<Playlist> findAllByOwner_EmailAndIsSystemFalseOrderByCreatedAtDesc(String email);

    Page<Playlist> findByVisibilityAndNameContainingIgnoreCase(PlaylistVisibility playlistVisibility, String s, Pageable pageable);
}
