package com.rentvideo;

import com.rentvideo.dto.RentalResponse;
import com.rentvideo.entity.Rental;
import com.rentvideo.entity.User;
import com.rentvideo.entity.Video;
import com.rentvideo.exception.ConflictException;
import com.rentvideo.exception.ResourceNotFoundException;
import com.rentvideo.repository.RentalRepository;
import com.rentvideo.repository.UserRepository;
import com.rentvideo.repository.VideoRepository;
import com.rentvideo.service.RentalService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock UserRepository userRepository;
    @Mock VideoRepository videoRepository;
    @Mock RentalRepository rentalRepository;
    @InjectMocks RentalService service;

    private User user() {
        User u = new User();
        u.setId(1L);
        u.setEmail("a@b.com");
        return u;
    }

    private Video video(long id, boolean available) {
        Video v = new Video();
        v.setId(id);
        v.setTitle("Title " + id);
        v.setAvailable(available);
        return v;
    }

    @Test
    void rentMarksVideoUnavailableAndCreatesRental() {
        Video v = video(10, true);
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(user()));
        when(rentalRepository.countByUserIdAndReturnedAtIsNull(1L)).thenReturn(1L);
        when(videoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(v));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

        RentalResponse r = service.rent("a@b.com", 10L);

        assertEquals(10L, r.videoId());
        assertNotNull(r.rentedAt());
        assertNull(r.returnedAt());
        assertFalse(v.isAvailable());
    }

    @Test
    void rentFailsWhenUserAlreadyHasTwoActiveRentals() {
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(user()));
        when(rentalRepository.countByUserIdAndReturnedAtIsNull(1L)).thenReturn(2L);

        assertThrows(ConflictException.class, () -> service.rent("a@b.com", 10L));
        verify(rentalRepository, never()).save(any());
    }

    @Test
    void rentFailsWhenVideoNotAvailable() {
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(user()));
        when(rentalRepository.countByUserIdAndReturnedAtIsNull(1L)).thenReturn(0L);
        when(videoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(video(10, false)));

        assertThrows(ConflictException.class, () -> service.rent("a@b.com", 10L));
    }

    @Test
    void rentFailsWhenVideoMissing() {
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(user()));
        when(rentalRepository.countByUserIdAndReturnedAtIsNull(1L)).thenReturn(0L);
        when(videoRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.rent("a@b.com", 99L));
    }

    @Test
    void returnMakesVideoAvailableAgain() {
        User u = user();
        Video v = video(10, false);
        Rental rental = new Rental();
        rental.setUser(u);
        rental.setVideo(v);
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(u));
        when(videoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(v));
        when(rentalRepository.findByUserIdAndVideoIdAndReturnedAtIsNull(1L, 10L)).thenReturn(Optional.of(rental));
        when(rentalRepository.save(any(Rental.class))).thenAnswer(i -> i.getArgument(0));

        RentalResponse r = service.returnVideo("a@b.com", 10L);

        assertNotNull(r.returnedAt());
        assertTrue(v.isAvailable());
    }

    @Test
    void returnFailsWithoutActiveRental() {
        User u = user();
        when(userRepository.findByEmailForUpdate("a@b.com")).thenReturn(Optional.of(u));
        when(videoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(video(10, true)));
        when(rentalRepository.findByUserIdAndVideoIdAndReturnedAtIsNull(1L, 10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.returnVideo("a@b.com", 10L));
    }
}
