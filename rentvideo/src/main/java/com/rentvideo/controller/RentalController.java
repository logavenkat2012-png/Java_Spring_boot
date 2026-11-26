package com.rentvideo.controller;

import com.rentvideo.dto.RentalResponse;
import com.rentvideo.service.RentalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RentalController {

    private final RentalService rentalService;

    @PostMapping("/videos/{videoId}/rent")
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse rent(@PathVariable Long videoId, Authentication auth) {
        return rentalService.rent(auth.getName(), videoId);
    }

    @PostMapping("/videos/{videoId}/return")
    public RentalResponse returnVideo(@PathVariable Long videoId, Authentication auth) {
        return rentalService.returnVideo(auth.getName(), videoId);
    }

    /** The caller's own rentals (history by default, ?activeOnly=true for current ones). */
    @GetMapping("/rentals/me")
    public List<RentalResponse> myRentals(@RequestParam(defaultValue = "false") boolean activeOnly,
                                          Authentication auth) {
        return rentalService.myRentals(auth.getName(), activeOnly);
    }
}
