package com.project.cadence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PlayHistoryId {

    @Column(name = "user_id")
    private String userId;

    @Column(name = "song_id")
    private String songId;
}

