package com.example.demo.security;

/**
 * Usuario autenticado sacado del JWT.
 * En un controller: public X metodo(@AuthenticationPrincipal AuthUser me) { me.id() ... }
 */
public record AuthUser(Long id, String username, String role) {
}
