package com.project.cadence.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "record")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class Record {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    @ToString.Include
    private String id;

    @Column(nullable = false)
    @ToString.Include
    private String title;

    @Column(name = "release_timestamp")
    private long releaseTimestamp;

    @Column(name = "cover_url")
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "record_type")
    private RecordType recordType;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "record_id", referencedColumnName = "id")
    private Set<Song> songs = new HashSet<>();

    @ManyToMany(mappedBy = "artistRecords", fetch = FetchType.LAZY)
    private Set<Artist> artists = new HashSet<>();
}
