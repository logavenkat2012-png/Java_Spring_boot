package com.example.rentvideo.service;

import com.example.rentvideo.dto.VideoRequest;
import com.example.rentvideo.entity.Video;
import com.example.rentvideo.exception.ResourceNotFoundException;
import com.example.rentvideo.repository.VideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VideoService {

    private final VideoRepository videoRepository;

    public VideoService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Transactional(readOnly = true)
    public List<Video> listAvailable(String genre) {
        if (genre == null || genre.isBlank()) {
            return videoRepository.findByAvailableTrue();
        }
        return videoRepository.findByAvailableTrueAndGenreIgnoreCase(genre.trim());
    }

    @Transactional(readOnly = true)
    public Video getById(Long id) {
        return videoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video not found with id " + id));
    }

    @Transactional
    public Video create(VideoRequest request) {
        Video video = new Video();
        video.setTitle(request.title().trim());
        video.setDirector(request.director().trim());
        video.setGenre(request.genre().trim());
        video.setAvailable(request.available() == null || request.available());
        return videoRepository.save(video);
    }

    @Transactional
    public Video update(Long id, VideoRequest request) {
        Video video = getById(id);
        video.setTitle(request.title().trim());
        video.setDirector(request.director().trim());
        video.setGenre(request.genre().trim());
        if (request.available() != null) {
            video.setAvailable(request.available());
        }
        return videoRepository.save(video);
    }

    @Transactional
    public void delete(Long id) {
        Video video = getById(id);
        videoRepository.delete(video);
    }
}
