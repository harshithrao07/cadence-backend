package com.project.musicplayer.repository;

import com.project.musicplayer.model.InvalidatedToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JwtRepository extends CrudRepository<InvalidatedToken, String> {
}
