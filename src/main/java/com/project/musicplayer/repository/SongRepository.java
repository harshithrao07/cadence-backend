package com.project.musicplayer.repository;

import com.project.musicplayer.model.Song;
import org.springframework.data.repository.CrudRepository;

public interface SongRepository extends CrudRepository<Song, String> {
}
