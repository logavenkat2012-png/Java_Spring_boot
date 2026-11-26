package com.rentvideo.repository;

import com.rentvideo.entity.Video;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoRepository extends JpaRepository<Video, Long> {

    List<Video> findByAvailableTrueAndDeletedFalse();

    Optional<Video> findByIdAndDeletedFalse(Long id);

    /** Row lock on the video: two users cannot rent the same copy at the same time. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Video v where v.id = :id and v.deleted = false")
    Optional<Video> findByIdForUpdate(@Param("id") Long id);
}
