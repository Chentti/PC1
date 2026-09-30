package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        // Para que las fechas leídas de la BD salgan con -05:00
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        SpringApplication.run(Application.class, args);
    }
}
