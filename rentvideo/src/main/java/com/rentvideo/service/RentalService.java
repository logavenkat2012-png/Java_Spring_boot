package com.rentvideo.service;

import com.rentvideo.dto.RentalResponse;
import com.rentvideo.entity.Rental;
import com.rentvideo.entity.User;
import com.rentvideo.entity.Video;
import com.rentvideo.exception.ConflictException;
import com.rentvideo.exception.ResourceNotFoundException;
import com.rentvideo.repository.RentalRepository;
import com.rentvideo.repository.UserRepository;
import com.rentvideo.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RentalService {

    public static final int MAX_ACTIVE_RENTALS = 2;

    private final UserRepository userRepository;
    private final VideoRepository videoRepository;
    private final RentalRepository rentalRepository;

    /**
     * Lock order is always user -> video (also in returnVideo) so concurrent calls cannot deadlock.
     * Locking the user makes the "max 2 active rentals" check race-free; locking the video makes
     * "only one renter at a time" race-free.
     */
    @Transactional
    public RentalResponse rent(String email, Long videoId) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (rentalRepository.countByUserIdAndReturnedAtIsNull(user.getId()) >= MAX_ACTIVE_RENTALS) {
            throw new ConflictException("Rental limit reached: you can have at most "
                    + MAX_ACTIVE_RENTALS + " active rentals");
        }

        Video video = videoRepository.findByIdForUpdate(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video " + videoId + " not found"));
        if (!video.isAvailable()) {
            throw new ConflictException("Video " + videoId + " is not available for rent");
        }

        video.setAvailable(false);
        Rental rental = new Rental();
        rental.setUser(user);
        rental.setVideo(video);
        rental.setRentedAt(Instant.now());
        return RentalResponse.from(rentalRepository.save(rental));
    }

    @Transactional
    public RentalResponse returnVideo(String email, Long videoId) {
        User user = userRepository.findByEmailForUpdate(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        Video video = videoRepository.findByIdForUpdate(videoId)
                .orElseThrow(() -> new ResourceNotFoundException("Video " + videoId + " not found"));
        Rental rental = rentalRepository.findByUserIdAndVideoIdAndReturnedAtIsNull(user.getId(), videoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "You have no active rental for video " + videoId));

        rental.setReturnedAt(Instant.now());
        video.setAvailable(true);
        return RentalResponse.from(rentalRepository.save(rental));
    }

    @Transactional(readOnly = true)
    public List<RentalResponse> myRentals(String email, boolean activeOnly) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<Rental> rentals = activeOnly
                ? rentalRepository.findByUserIdAndReturnedAtIsNullOrderByRentedAtDesc(user.getId())
                : rentalRepository.findByUserIdOrderByRentedAtDesc(user.getId());
        return rentals.stream().map(RentalResponse::from).toList();
    }
}
