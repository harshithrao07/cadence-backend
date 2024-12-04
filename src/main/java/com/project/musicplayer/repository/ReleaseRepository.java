package com.project.musicplayer.repository;

import com.project.musicplayer.model.Releases;
import org.springframework.data.repository.CrudRepository;

public interface ReleaseRepository extends CrudRepository<Releases, String> {
}
