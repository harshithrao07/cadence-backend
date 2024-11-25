package com.project.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "album")
public class Album {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_year")
    private int releaseYear;

    @Column(name = "cover_url")
    private String coverUrl;

    @OneToMany(cascade = CascadeType.PERSIST, fetch = FetchType.EAGER)
    @JoinColumn(name = "album_id", referencedColumnName = "id")
    private Set<Song> songs = new HashSet<>();

    @ManyToMany(mappedBy = "createdAlbums")
    private Set<Artist> artists = new HashSet<>();
}
