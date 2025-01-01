package com.project.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "song")
public class Song {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String songUrl;

    @Column(name = "total_duration", nullable = false)
    private int totalDuration;

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "record_id")
    private String recordId;

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "artistSongs")
    private Set<Artist> createdBy = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "featureSongs")
    private Set<Artist> features = new HashSet<>();

    @ManyToMany(mappedBy = "likedSongs")
    private Set<User> likedBy = new HashSet<>();

    @ManyToMany(mappedBy = "songs")
    private Set<Playlist> playlists = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_genre",
            joinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id", referencedColumnName = "id")
    )
    private Set<Genre> genres = new HashSet<>();
}
