package com.example.demo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.ZonedDateTime;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        ZonedDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fields) {
}
