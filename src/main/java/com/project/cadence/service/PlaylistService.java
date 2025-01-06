package com.project.cadence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cadence.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    ObjectMapper objectMapper = new ObjectMapper();
}
