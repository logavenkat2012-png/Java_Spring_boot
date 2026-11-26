package com.rentvideo.controller;

import com.rentvideo.dto.VideoRequest;
import com.rentvideo.dto.VideoResponse;
import com.rentvideo.service.VideoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    /** Any authenticated user: videos that can currently be rented. */
    @GetMapping
    public List<VideoResponse> listAvailable() {
        return videoService.listAvailable();
    }

    @GetMapping("/{id}")
    public VideoResponse get(@PathVariable Long id) {
        return videoService.get(id);
    }

    // ---- ADMIN only (enforced in SecurityConfig) ----

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VideoResponse create(@Valid @RequestBody VideoRequest request) {
        return videoService.create(request);
    }

    @PutMapping("/{id}")
    public VideoResponse update(@PathVariable Long id, @Valid @RequestBody VideoRequest request) {
        return videoService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        videoService.delete(id);
    }
}
