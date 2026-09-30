package com.example.demo;

import com.example.demo.entity.Role;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Flujo completo con seguridad JWT real sobre H2. */
@SpringBootTest
@AutoConfigureMockMvc
class LabReserveIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String adminToken;
    private String studentToken;
    private Long adminId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        adminId = createUser("admin" + suffix, Role.ADMIN);
        adminToken = login("admin" + suffix);

        // el estudiante sí entra por el auto-registro público
        String studentName = "stu" + suffix;
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + studentName + "\",\"email\":\"" + studentName
                                + "@utec.edu.pe\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());
        studentToken = login(studentName);
    }

    @Test
    void sinToken_devuelve401() throws Exception {
        mvc.perform(get("/laboratories")).andExpect(status().isUnauthorized());
    }

    @Test
    void registro_siempreCreaStudent_yPasswordQuedaEncriptado() throws Exception {
        String name = "reg" + UUID.randomUUID().toString().substring(0, 6);
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + name + "\",\"email\":\"" + name
                        + "@utec.edu.pe\",\"password\":\"password123\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated());

        User u = userRepository.findByUsername(name).orElseThrow();
        assertEquals(Role.STUDENT, u.getRole());
        assertEquals(true, passwordEncoder.matches("password123", u.getPassword()));
    }

    @Test
    void registro_usernameDuplicado_devuelve409() throws Exception {
        String name = "dup" + UUID.randomUUID().toString().substring(0, 6);
        String body = "{\"username\":\"" + name + "\",\"email\":\"" + name
                + "@utec.edu.pe\",\"password\":\"password123\"}";
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
    }

    @Test
    void estudiante_noPuedeCrearLaboratorio_403() throws Exception {
        postAs("/laboratories", studentToken, labJson(adminId)).andExpect(status().isForbidden());
    }

    @Test
    void laboratorio_managerInexistente_404_yManagerEstudiante_400() throws Exception {
        postAs("/laboratories", adminToken, labJson(999999L)).andExpect(status().isNotFound());

        Long studentId = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.STUDENT).findFirst().orElseThrow().getId();
        postAs("/laboratories", adminToken, labJson(studentId)).andExpect(status().isBadRequest());
    }

    @Test
    void laboratorio_validacion_devuelve400ConCampos() throws Exception {
        postAs("/laboratories", adminToken, "{\"username\":\"\",\"email\":\"no-es-email\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.username").exists())
                .andExpect(jsonPath("$.fields.email").exists())
                .andExpect(jsonPath("$.fields.managerId").exists());
    }

    @Test
    void flujoCompleto_slotSeLlenaYSeLiberaAlCancelar() throws Exception {
        long labId = createLab();
        long slotId = createSlot(labId, "MIC-01", 1);

        // reserva -> capacity 1 => slot FULL
        JsonNode reservation = json(postAs("/reservations", studentToken,
                "{\"slotId\":" + slotId + ",\"purpose\":\"Práctica de circuitos\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RESERVED")));
        getAs("/slots/" + slotId, adminToken).andExpect(jsonPath("$.status").value("FULL"));

        // un segundo estudiante ya no puede reservar
        String other = "stu2" + UUID.randomUUID().toString().substring(0, 6);
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + other + "\",\"email\":\"" + other
                        + "@utec.edu.pe\",\"password\":\"password123\"}")).andExpect(status().isCreated());
        postAs("/reservations", login(other), "{\"slotId\":" + slotId + ",\"purpose\":\"x\"}")
                .andExpect(status().isConflict());

        // cancelar libera el cupo
        patchAs("/reservations/" + reservation.get("id").asLong() + "/cancel", studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        getAs("/slots/" + slotId, adminToken).andExpect(jsonPath("$.status").value("AVAILABLE"));

        getAs("/reservations/me", studentToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void reserva_purposeMayorA250_devuelve400() throws Exception {
        long slotId = createSlot(createLab(), "MIC-02", 2);
        postAs("/reservations", studentToken,
                "{\"slotId\":" + slotId + ",\"purpose\":\"" + "a".repeat(251) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.purpose").exists());
    }

    @Test
    void slot_solapadoDelMismoEquipo_devuelve409() throws Exception {
        long labId = createLab();
        createSlot(labId, "MIC-03", 2);
        postAs("/slots", adminToken, slotJson(labId, "MIC-03", 2)).andExpect(status().isConflict());
    }

    @Test
    void slot_endTimeAnteriorAStartTime_devuelve400() throws Exception {
        long labId = createLab();
        ZonedDateTime start = ZonedDateTime.now().plusDays(3);
        postAs("/slots", adminToken, "{\"laboratoryId\":" + labId + ",\"equipmentCode\":\"MIC-04\",\"startTime\":\""
                + start + "\",\"endTime\":\"" + start.minusHours(1) + "\",\"capacity\":2}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void administrador_noPuedeReservar_403() throws Exception {
        long slotId = createSlot(createLab(), "MIC-05", 2);
        postAs("/reservations", adminToken, "{\"slotId\":" + slotId + ",\"purpose\":\"x\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelarSlot_cancelaLasReservasActivas() throws Exception {
        long slotId = createSlot(createLab(), "MIC-06", 3);
        JsonNode r = json(postAs("/reservations", studentToken, "{\"slotId\":" + slotId + ",\"purpose\":\"x\"}")
                .andExpect(status().isCreated()));

        patchAs("/slots/" + slotId + "/cancel", adminToken)
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));

        getAs("/reservations/me", studentToken)
                .andExpect(jsonPath("$.content[?(@.id==" + r.get("id").asLong() + ")].status").value("CANCELLED"));
    }

    // ---------- helpers ----------

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

    private String labJson(Long managerId) {
        return "{\"username\":\"lab-" + UUID.randomUUID().toString().substring(0, 6)
                + "\",\"email\":\"lab@utec.edu.pe\",\"location\":\"Bloque A\",\"managerId\":" + managerId + "}";
    }

    private long createLab() throws Exception {
        return json(postAs("/laboratories", adminToken, labJson(adminId)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    private static final ZonedDateTime START = ZonedDateTime.now().plusDays(2).withNano(0);

    private String slotJson(long labId, String code, int capacity) {
        return "{\"laboratoryId\":" + labId + ",\"equipmentCode\":\"" + code + "\",\"startTime\":\"" + START
                + "\",\"endTime\":\"" + START.plusHours(1) + "\",\"capacity\":" + capacity + "}";
    }

    private long createSlot(long labId, String code, int capacity) throws Exception {
        return json(postAs("/slots", adminToken, slotJson(labId, code, capacity)).andExpect(status().isCreated()))
                .get("id").asLong();
    }

    private JsonNode json(ResultActions a) throws Exception {
        return mapper.readTree(a.andReturn().getResponse().getContentAsString());
    }

    private ResultActions postAs(String url, String token, String body) throws Exception {
        return mvc.perform(post(url)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private ResultActions getAs(String url, String token) throws Exception {
        return mvc.perform(get(url)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions patchAs(String url, String token) throws Exception {
        return mvc.perform(patch(url)
                .header("Authorization", "Bearer " + token));
    }
}
