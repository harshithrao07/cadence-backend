package com.project.cadence.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "play_history")
public class PlayHistory {
    @Id
    @Column(name = "user_id")
    private String userId;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "song_id", referencedColumnName = "id")
    private Song songId;

    @Column(name = "stop_position", nullable = false)
    private int stopPosition;
}
