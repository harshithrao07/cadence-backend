package com.project.cadence.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    @JsonManagedReference
    private Set<Artist> createdBy = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "featureSongs")
    @JsonManagedReference
    private Set<Artist> features = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "likedSongs")
    private Set<User> likedBy = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "songs")
    private Set<Playlist> playlists = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "song_genre",
            joinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id", referencedColumnName = "id")
    )
    @JsonBackReference
    private Set<Genre> genres = new HashSet<>();
}
