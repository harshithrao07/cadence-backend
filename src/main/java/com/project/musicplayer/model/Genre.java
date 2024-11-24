package com.project.musicplayer.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "genre")
public class Genre {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String type;

    @ManyToMany(mappedBy = "genres")
    private Set<Song> songs;

    @ManyToMany(mappedBy = "genrePreferences")
    private Set<User> users;
}
