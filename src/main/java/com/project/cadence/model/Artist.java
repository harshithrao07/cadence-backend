package com.project.cadence.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "artist")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false, unique = true)
    @ToString.Include
    private String name;

    @Column(name = "profile_url")
    private String profileUrl;

    private String description;

    @Column(
            name = "followers_count",
            nullable = false,
            columnDefinition = "BIGINT DEFAULT 0"
    )
    private long followersCount = 0;

    @ManyToMany(mappedBy = "artists", fetch = FetchType.LAZY)
    private Set<Record> artistRecords = new HashSet<>();

    @ManyToMany(
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE}
    )
    @JoinTable(
            name = "artist_created_songs",
            joinColumns = @JoinColumn(name = "artist_id", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "song_id", referencedColumnName = "id")
    )
    private Set<Song> artistSongs = new HashSet<>();

    @ManyToMany(mappedBy = "artistFollowing", fetch = FetchType.LAZY)
    private Set<User> userFollowers = new HashSet<>();
}
