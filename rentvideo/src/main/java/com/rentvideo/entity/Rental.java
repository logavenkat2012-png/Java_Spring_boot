package com.rentvideo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/** A rental is "active" while returnedAt is null. */
@Entity
@Table(name = "rentals", indexes = {
        @Index(name = "idx_rental_user_returned", columnList = "user_id, returned_at"),
        @Index(name = "idx_rental_video_returned", columnList = "video_id, returned_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Rental {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "video_id", nullable = false)
    private Video video;

    @Column(name = "rented_at", nullable = false)
    private Instant rentedAt;

    @Column(name = "returned_at")
    private Instant returnedAt;
}
