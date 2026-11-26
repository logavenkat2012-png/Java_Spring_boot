package com.rentvideo.repository;

import com.rentvideo.entity.Rental;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RentalRepository extends JpaRepository<Rental, Long> {

    long countByUserIdAndReturnedAtIsNull(Long userId);

    Optional<Rental> findByUserIdAndVideoIdAndReturnedAtIsNull(Long userId, Long videoId);

    List<Rental> findByUserIdOrderByRentedAtDesc(Long userId);

    List<Rental> findByUserIdAndReturnedAtIsNullOrderByRentedAtDesc(Long userId);
}
