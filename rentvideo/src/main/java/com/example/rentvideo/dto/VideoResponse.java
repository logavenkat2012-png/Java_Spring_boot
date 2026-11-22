package com.example.rentvideo.dto;

import com.example.rentvideo.entity.Video;

public record VideoResponse(Long id, String title, String director, String genre, boolean available) {

    public static VideoResponse from(Video v) {
        return new VideoResponse(v.getId(), v.getTitle(), v.getDirector(), v.getGenre(), v.isAvailable());
    }
}
