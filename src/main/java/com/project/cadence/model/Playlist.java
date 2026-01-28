package com.project.cadence.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "playlist",
        indexes = {
                @Index(name = "idx_playlist_user_id", columnList = "user_id")
        }
)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Playlist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false)
    @ToString.Include
    private String name;

    @Column(name = "cover_url")
    private String coverUrl;

    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User owner;

    @Builder.Default
    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private PlaylistVisibility visibility = PlaylistVisibility.PUBLIC;

    @Builder.Default
    @Column(name = "is_system", updatable = false, nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean isSystem = false;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "playlist_songs",
            joinColumns = @JoinColumn(name = "playlist_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id"),
            indexes = {
                    @Index(name = "idx_playlist_songs_playlist_id", columnList = "playlist_id"),
                    @Index(name = "idx_playlist_songs_song_id", columnList = "song_id"),
                    @Index(name = "idx_playlist_songs_playlist_id_song_order", columnList = "playlist_id, song_order")
            },
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"playlist_id", "song_id"})
            }
    )
    @OrderColumn(name = "song_order")
    private List<Song> songs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
