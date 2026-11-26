package com.rentvideo.exception;

/** Request is valid but conflicts with current state (duplicate email, rental limit, already rented...). */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
