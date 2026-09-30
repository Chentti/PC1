package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Todas las excepciones de negocio heredan de aquí y definen su código HTTP. */
@Getter
public abstract class ApiException extends RuntimeException {
    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
