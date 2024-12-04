package com.project.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "releases")
public class Releases {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(name = "release_timestamp")
    private long releaseTimestamp;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    private ReleaseType releaseType;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "release_id", referencedColumnName = "id")
    private Set<Song> songs = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "artistReleases")
    private Set<Artist> artists = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "featureReleases")
    private Set<Artist> features = new HashSet<>();
}
