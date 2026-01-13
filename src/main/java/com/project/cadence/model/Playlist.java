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
@Table(name = "playlist")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Enumerated(value = EnumType.STRING)
    private PlaylistVisibility visibility;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "playlist_songs",
            joinColumns = @JoinColumn(name = "playlist_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id")
    )
    private Set<Song> songs = new HashSet<>();

    @ManyToMany(mappedBy = "likedPlaylists", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();
}
