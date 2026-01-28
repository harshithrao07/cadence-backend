package com.project.cadence.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_email", columnList = "email")
        }
)
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

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String passwordHash;

    @Column(name = "profile_url")
    private String profileUrl;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Role role = Role.USER;

    @Builder.Default
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE)
    private List<PlayHistory> playHistory = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "owner", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Playlist> createdPlaylists = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "liked_playlists",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "playlist_id", referencedColumnName = "id"),
            indexes = {
                    @Index(name = "idx_liked_playlists_user_id", columnList = "user_id"),
                    @Index(name = "idx_liked_playlists_playlist_id", columnList = "playlist_id")
            },
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"user_id", "playlist_id"})
            }
    )
    @OrderColumn(name = "like_order")
    private List<Playlist> likedPlaylists = new ArrayList<>();

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "artist_following",
            joinColumns = @JoinColumn(name = "user_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id", referencedColumnName = "id"),
            indexes = {
                    @Index(name = "idx_artist_following_artist_id", columnList = "artist_id"),
                    @Index(name = "idx_artist_following_user_id", columnList = "user_id")
            },
            uniqueConstraints = {
                    @UniqueConstraint(columnNames = {"user_id", "artist_id"})
            }
    )
    @OrderColumn(name = "follow_order")
    private List<Artist> artistFollowing = new ArrayList<>();
}
