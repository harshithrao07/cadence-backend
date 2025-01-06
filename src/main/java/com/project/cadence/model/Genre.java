package com.project.cadence.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
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

    @EqualsAndHashCode.Exclude
    @ManyToMany(mappedBy = "genres")
    @JsonManagedReference
    private Set<Song> songs = new HashSet<>();
}
