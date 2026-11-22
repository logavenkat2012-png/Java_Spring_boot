package com.example.rentvideo.dto;

import jakarta.validation.constraints.NotBlank;

public record VideoRequest(
        @NotBlank String title,
        @NotBlank String director,
        @NotBlank String genre,
        Boolean available) {
}
