package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class InvalidEmailDomainException extends ApiException {
    public InvalidEmailDomainException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
