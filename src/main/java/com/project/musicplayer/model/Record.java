package com.project.musicplayer.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "record")
public class Record {
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
    @Column(name = "record_type")
    private RecordType recordType;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "record_id", referencedColumnName = "id")
    private Set<Song> songs = new HashSet<>();

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "artistRecords")
    @JsonManagedReference
    private Set<Artist> artists = new HashSet<>();
}
