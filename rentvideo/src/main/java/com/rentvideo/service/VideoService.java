package com.rentvideo.service;

import com.rentvideo.dto.VideoRequest;
import com.rentvideo.dto.VideoResponse;
import com.rentvideo.entity.Video;
import com.rentvideo.exception.ConflictException;
import com.rentvideo.exception.ResourceNotFoundException;
import com.rentvideo.repository.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    @Transactional(readOnly = true)
    public List<VideoResponse> listAvailable() {
        return videoRepository.findByAvailableTrueAndDeletedFalse().stream().map(VideoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public VideoResponse get(Long id) {
        return VideoResponse.from(find(id));
    }

    @Transactional
    public VideoResponse create(VideoRequest req) {
        Video v = new Video();   // available = true by default
        apply(v, req);
        return VideoResponse.from(videoRepository.save(v));
    }

    @Transactional
    public VideoResponse update(Long id, VideoRequest req) {
        Video v = find(id);
        apply(v, req);           // availability is controlled only by rent/return, never by update
        return VideoResponse.from(videoRepository.save(v));
    }

    @Transactional
    public void delete(Long id) {
        Video v = videoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video " + id + " not found"));
        if (!v.isAvailable()) {
            throw new ConflictException("Video is currently rented and cannot be deleted");
        }
        v.setDeleted(true);
        v.setAvailable(false);
        videoRepository.save(v);
    }

    private Video find(Long id) {
        return videoRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Video " + id + " not found"));
    }

    private void apply(Video v, VideoRequest req) {
        v.setTitle(req.title().trim());
        v.setDirector(req.director().trim());
        v.setGenre(req.genre().trim());
    }
}
