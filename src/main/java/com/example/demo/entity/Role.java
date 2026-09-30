package com.example.demo.entity;

public enum Role {
    STUDENT,
    TECHNICIAN,
    ADMIN;


    public String authority() {
        return "ROLE_" + name();
    }
}
