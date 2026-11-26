package com.rentvideo.dto;

import com.rentvideo.entity.Rental;

import java.time.Instant;

public record RentalResponse(Long id, Long videoId, String videoTitle, Instant rentedAt, Instant returnedAt) {
    public static RentalResponse from(Rental r) {
        return new RentalResponse(r.getId(), r.getVideo().getId(), r.getVideo().getTitle(),
                r.getRentedAt(), r.getReturnedAt());
    }
}
