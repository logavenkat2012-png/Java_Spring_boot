package com.example.rentvideo.controller;

import com.example.rentvideo.dto.VideoRequest;
import com.example.rentvideo.dto.VideoResponse;
import com.example.rentvideo.service.VideoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    private final VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }

    /** CUSTOMER or ADMIN: list available videos, optionally filtered by genre. */
    @GetMapping
    public List<VideoResponse> listAvailable(@RequestParam(required = false) String genre) {
        return videoService.listAvailable(genre).stream().map(VideoResponse::from).toList();
    }

    /** CUSTOMER or ADMIN: get one video. */
    @GetMapping("/{id}")
    public VideoResponse getById(@PathVariable Long id) {
        return VideoResponse.from(videoService.getById(id));
    }

    /** ADMIN only. */
    @PostMapping
    public ResponseEntity<VideoResponse> create(@Valid @RequestBody VideoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(VideoResponse.from(videoService.create(request)));
    }

    /** ADMIN only. */
    @PutMapping("/{id}")
    public VideoResponse update(@PathVariable Long id, @Valid @RequestBody VideoRequest request) {
        return VideoResponse.from(videoService.update(id, request));
    }

    /** ADMIN only. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        videoService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
