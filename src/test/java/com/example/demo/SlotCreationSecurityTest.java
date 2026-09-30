package com.example.demo;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.ZonedDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** POST /slots: solo ROLE_TECHNICIAN o ROLE_ADMIN con Bearer token. */
@SpringBootTest
@AutoConfigureMockMvc
class SlotCreationSecurityTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken, technicianToken, studentToken;
    private long labId;

    @BeforeEach
    void setUp() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        Long adminId = createUser("adm" + s, Role.ADMIN);
        createUser("tec" + s, Role.TECHNICIAN);
        createUser("stu" + s, Role.STUDENT);
        adminToken = login("adm" + s);
        technicianToken = login("tec" + s);
        studentToken = login("stu" + s);

        String res = mvc.perform(post("/laboratories").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"lab" + s + "\",\"email\":\"lab@utec.edu.pe\","
                                + "\"location\":\"Bloque A\",\"managerId\":" + adminId + "}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        labId = mapper.readTree(res).get("id").asLong();
    }

    /** Cada llamada usa un equipo distinto para no chocar por solapamiento. */
    private ResultActions createSlot(String token, String equipmentCode) throws Exception {
        ZonedDateTime start = ZonedDateTime.now().plusDays(5).withNano(0);
        var req = post("/slots").contentType(MediaType.APPLICATION_JSON)
                .content("{\"laboratoryId\":" + labId + ",\"equipmentCode\":\"" + equipmentCode
                        + "\",\"startTime\":\"" + start + "\",\"endTime\":\"" + start.plusHours(2)
                        + "\",\"capacity\":3}");
        if (token != null) req = req.header("Authorization", "Bearer " + token);
        return mvc.perform(req);
    }

    @Test
    void technician_creaTurno_201() throws Exception {
        createSlot(technicianToken, "EQ-T")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.capacity").value(3));
    }

    @Test
    void admin_creaTurno_201() throws Exception {
        createSlot(adminToken, "EQ-A").andExpect(status().isCreated());
    }

    @Test
    void student_noPuedeCrearTurno_403() throws Exception {
        createSlot(studentToken, "EQ-S").andExpect(status().isForbidden());
    }

    @Test
    void sinToken_401() throws Exception {
        createSlot(null, "EQ-N").andExpect(status().isUnauthorized());
    }

    @Test
    void tokenInvalido_401() throws Exception {
        createSlot("token.falso.invalido", "EQ-X").andExpect(status().isUnauthorized());
    }

    @Test
    void technician_conBodyInvalido_400() throws Exception {
        mvc.perform(post("/slots").header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"equipmentCode\":\"\",\"capacity\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.laboratoryId").exists())
                .andExpect(jsonPath("$.fields.equipmentCode").exists())
                .andExpect(jsonPath("$.fields.capacity").exists());
    }

    @Test
    void technician_laboratorioInexistente_404() throws Exception {
        ZonedDateTime start = ZonedDateTime.now().plusDays(6).withNano(0);
        mvc.perform(post("/slots").header("Authorization", "Bearer " + technicianToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"laboratoryId\":999999,\"equipmentCode\":\"EQ\",\"startTime\":\"" + start
                                + "\",\"endTime\":\"" + start.plusHours(1) + "\",\"capacity\":1}"))
                .andExpect(status().isNotFound());
    }

    private Long createUser(String username, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@utec.edu.pe");
        u.setPassword(passwordEncoder.encode("password123"));
        u.setRole(role);
        return userRepository.save(u).getId();
    }

    private String login(String username) throws Exception {
        String res = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"password123\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(res).get("token").asText();
    }
}
