package com.project.musicplayer.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrackRecordInfoDTO(
        String id,
        String title
) {
}
