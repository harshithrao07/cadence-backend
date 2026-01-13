package com.project.cadence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "users")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String email;

    @Column(name = "firstname", nullable = false)
    private String name;

    @Column(nullable = false)
    private String password;

    @Column(name = "profile_url")
    private String profileUrl;

    @Column(name = "date_of_birth")
    private Date dateOfBirth;

    @Enumerated(EnumType.STRING)
    private Role role;

    /* -------- Play history (composite key model) -------- */

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private Set<PlayHistory> playHistory = new HashSet<>();

    /* -------- Owned playlists -------- */

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private Set<Playlist> playlists = new HashSet<>();

    /* -------- Likes -------- */

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "liked_songs",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id")
    )
    private Set<Song> likedSongs = new HashSet<>();

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "liked_playlists",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "playlist_id", referencedColumnName = "id")
    )
    private Set<Playlist> likedPlaylists = new HashSet<>();

    /* -------- User follows user -------- */

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_following",
            joinColumns = @JoinColumn(name = "follower_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "following_id", referencedColumnName = "id")
    )
    private Set<User> userFollowing = new HashSet<>();

    @ManyToMany(mappedBy = "userFollowing", fetch = FetchType.LAZY)
    private Set<User> userFollowers = new HashSet<>();

    /* -------- User follows artists -------- */

    @ManyToMany(cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    @JoinTable(
            name = "artist_following",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id", referencedColumnName = "id")
    )
    private Set<Artist> artistFollowing = new HashSet<>();
}
