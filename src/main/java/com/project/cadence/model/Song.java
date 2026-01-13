package com.project.cadence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "song")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false)
    @ToString.Include
    private String title;

    @Column
    private String songUrl;

    @Column(name = "total_duration", nullable = false)
    private int totalDuration;

    @Column(name = "cover_url")
    private String coverUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", nullable = false)
    private Record record;

    @ManyToMany(mappedBy = "artistSongs", fetch = FetchType.LAZY)
    private Set<Artist> createdBy = new HashSet<>();

    @ManyToMany(mappedBy = "likedSongs", fetch = FetchType.LAZY)
    private Set<User> likedBy = new HashSet<>();

    @ManyToMany(mappedBy = "songs", fetch = FetchType.LAZY)
    private Set<Playlist> playlists = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_genre",
            joinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id", referencedColumnName = "id")
    )
    private Set<Genre> genres = new HashSet<>();

    @Column(name = "track_order", columnDefinition = "INTEGER DEFAULT 1")
    private int order;
}
