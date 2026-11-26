package com.rentvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "videos")
@Getter
@Setter
@NoArgsConstructor
public class Video {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String director;

    @Column(nullable = false)
    private String genre;

    /** Denormalised flag so browsing is a cheap indexed query. Kept in sync with rentals under a row lock. */
    @Column(nullable = false)
    private boolean available = true;

    /** Soft delete, so rental history keeps pointing at a real row. */
    @Column(nullable = false)
    private boolean deleted = false;
}
