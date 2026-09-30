package com.example.demo;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository userRepository;

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private ResultActions register(String username, String email, String password) throws Exception {
        return mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"email\":\"" + email
                        + "\",\"password\":\"" + password + "\"}"));
    }

    private ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
    }

    @Test
    void register_ok_devuelve201ConIdUsernameEmail_sinPassword() throws Exception {
        String u = "ok" + unique();
        register(u, u + "@utec.edu.pe", "password123")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.username").value(u))
                .andExpect(jsonPath("$.email").value(u + "@utec.edu.pe"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void register_guardaPasswordEncriptadoYRolInicial() throws Exception {
        String u = "enc" + unique();
        register(u, u + "@utec.edu.pe", "password123").andExpect(status().isCreated());

        User saved = userRepository.findByUsername(u).orElseThrow();
        assertNotEquals("password123", saved.getPassword());
        assertTrue(saved.getPassword().startsWith("$2"), "debe ser un hash BCrypt");
        assertEquals(Role.STUDENT, saved.getRole());
    }

    @Test
    void register_usernameDuplicado_devuelve409() throws Exception {
        String u = "dupu" + unique();
        register(u, u + "@utec.edu.pe", "password123").andExpect(status().isCreated());
        register(u, "otro" + unique() + "@utec.edu.pe", "password123").andExpect(status().isConflict());
    }

    @Test
    void register_emailDuplicado_devuelve409() throws Exception {
        String u = "dupe" + unique();
        String email = u + "@utec.edu.pe";
        register(u, email, "password123").andExpect(status().isCreated());
        register("otro" + unique(), email, "password123").andExpect(status().isConflict());
    }

    @Test
    void register_passwordCorto_devuelve400() throws Exception {
        String u = "pw" + unique();
        register(u, u + "@utec.edu.pe", "corta12")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.password").exists());
    }

    @Test
    void register_emailInvalido_devuelve400() throws Exception {
        register("em" + unique(), "no-es-un-email", "password123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }

    @Test
    void login_credencialesValidas_devuelveTokenYExpiresIn() throws Exception {
        String u = "lg" + unique();
        register(u, u + "@utec.edu.pe", "password123").andExpect(status().isCreated());

        login(u, "password123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void login_passwordIncorrecto_devuelve401() throws Exception {
        String u = "bad" + unique();
        register(u, u + "@utec.edu.pe", "password123").andExpect(status().isCreated());

        login(u, "incorrecta123").andExpect(status().isUnauthorized());
    }

    @Test
    void login_usuarioInexistente_devuelve401() throws Exception {
        login("noexiste" + unique(), "password123").andExpect(status().isUnauthorized());
    }
}
