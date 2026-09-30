package com.example.demo.auth;

import com.example.demo.auth.dto.LoginRequest;
import com.example.demo.auth.dto.LoginResponse;
import com.example.demo.auth.dto.RegisterRequest;
import com.example.demo.auth.dto.RegisterResponse;
import com.example.demo.entity.Role;
import com.example.demo.exception.InvalidCredentialsException;
import com.example.demo.exception.UserAlreadyExistsException;
import com.example.demo.security.JwtService;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegisterResponse register(RegisterRequest req) {
        // TODO parcial: si piden dominio, descomentar:
        // if (!req.email().toLowerCase().endsWith("@utec.edu.pe"))
        //     throw new InvalidEmailDomainException("El email debe terminar en @utec.edu.pe");

        if (userRepository.existsByUsername(req.username()))
            throw new UserAlreadyExistsException("El username ya está registrado");
        if (userRepository.existsByEmail(req.email()))
            throw new UserAlreadyExistsException("El email ya está registrado");

        User user = new User();
        user.setUsername(req.username());
        user.setEmail(req.email());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setRole(Role.STUDENT); // el auto-registro siempre es estudiante
        user = userRepository.save(user);

        return new RegisterResponse(user.getId(), user.getUsername(), user.getEmail());
    }

    public LoginResponse login(LoginRequest req) {
        User user = userRepository.findByUsername(req.username())
                .orElseThrow(() -> new InvalidCredentialsException("Credenciales incorrectas"));
        if (!passwordEncoder.matches(req.password(), user.getPassword()))
            throw new InvalidCredentialsException("Credenciales incorrectas");

        String token = jwtService.generateToken(user.getId(), user.getUsername(), user.getRole().authority());
        return new LoginResponse(token, jwtService.getExpirationSeconds());
    }
}
