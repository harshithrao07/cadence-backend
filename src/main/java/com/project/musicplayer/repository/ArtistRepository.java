package com.project.musicplayer.repository;

import com.project.musicplayer.model.Artist;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistRepository extends CrudRepository<Artist, String> {
}
