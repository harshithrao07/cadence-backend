package com.project.musicplayer.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @Column(name = "cover_url")
    private String coverUrl;

    @Column(name = "album_id")
    private String albumId;

    @ManyToMany(mappedBy = "createdSongs")
    private Set<Artist> artists;

    @ManyToMany(mappedBy = "likedSongs")
    @JsonIgnore
    private Set<User> likedBy;

    @ManyToMany(mappedBy = "songs")
    @JsonIgnore
    private Set<Playlist> playlists;

    @ManyToMany(cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(
            name = "song_genre",
            joinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id", referencedColumnName = "id")
    )
    private Set<Genre> genres;
}
