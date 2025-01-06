package com.project.cadence.repository;

import com.project.cadence.model.Playlist;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
public interface PlaylistRepository extends CrudRepository<Playlist, String> {
    Set<Optional<Playlist>> findByUserId(String userId);
}
