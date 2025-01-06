package com.project.cadence.repository;

import com.project.cadence.model.InvalidatedToken;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JwtRepository extends CrudRepository<InvalidatedToken, String> {
}
