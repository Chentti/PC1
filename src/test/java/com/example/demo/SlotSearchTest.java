package com.example.demo;

import com.example.demo.entity.*;
import com.example.demo.repository.EquipmentSlotRepository;
import com.example.demo.repository.LaboratoryRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.common.TimeUtil;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** GET /slots: solo AVAILABLE y futuros, filtros combinables y paginación. */
@SpringBootTest
@AutoConfigureMockMvc
class SlotSearchTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository userRepository;
    @Autowired LaboratoryRepository laboratoryRepository;
    @Autowired EquipmentSlotRepository slotRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private String token;
    private long labA, labB;
    private ZonedDateTime base;

    @BeforeEach
    void setUp() throws Exception {
        String s = UUID.randomUUID().toString().substring(0, 8);
        User u = new User();
        u.setUsername("stu" + s);
        u.setEmail("stu" + s + "@utec.edu.pe");
        u.setPassword(passwordEncoder.encode("password123"));
        u.setRole(Role.STUDENT);
        User manager = new User();
        manager.setUsername("mgr" + s);
        manager.setEmail("mgr" + s + "@utec.edu.pe");
        manager.setPassword(passwordEncoder.encode("password123"));
        manager.setRole(Role.ADMIN);
        userRepository.save(u);
        userRepository.save(manager);

        labA = lab(manager, "A" + s);
        labB = lab(manager, "B" + s);
        base = TimeUtil.now().plusDays(10).withHour(8).withMinute(0).withSecond(0);

        String res = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"stu" + s + "\",\"password\":\"password123\"}"))
                .andReturn().getResponse().getContentAsString();
        token = mapper.readTree(res).get("token").asText();
    }

    private long lab(User manager, String name) {
        Laboratory l = new Laboratory();
        l.setUsername(name);
        l.setEmail(name + "@utec.edu.pe");
        l.setLocation("Bloque");
        l.setManager(manager);
        return laboratoryRepository.save(l).getId();
    }

    private void slot(long labId, String code, ZonedDateTime start, SlotStatus status) {
        EquipmentSlot e = new EquipmentSlot();
        e.setLaboratoryId(labId);
        e.setEquipmentCode(code);
        e.setStartTime(start);
        e.setEndTime(start.plusHours(1));
        e.setCapacity(2);
        e.setStatus(status);
        slotRepository.save(e);
    }

    private ResultActions search(String query) throws Exception {
        return mvc.perform(get("/slots" + query).header("Authorization", "Bearer " + token));
    }

    @Test
    void soloDevuelveAvailableYFuturos() throws Exception {
        slot(labA, "EQ1", base, SlotStatus.AVAILABLE);
        slot(labA, "EQ1", base.plusHours(2), SlotStatus.FULL);
        slot(labA, "EQ1", base.plusHours(4), SlotStatus.CANCELLED);
        slot(labA, "EQ1", TimeUtil.now().minusDays(1), SlotStatus.AVAILABLE); // pasado

        search("?laboratoryId=" + labA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("AVAILABLE"));
    }

    @Test
    void filtroPorLaboratorio() throws Exception {
        slot(labA, "EQ1", base, SlotStatus.AVAILABLE);
        slot(labB, "EQ1", base, SlotStatus.AVAILABLE);

        search("?laboratoryId=" + labB)
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].laboratoryId").value(labB));
    }

    @Test
    void filtrosCombinados_laboratorioEquipoYFrom() throws Exception {
        slot(labA, "EQ1", base, SlotStatus.AVAILABLE);
        slot(labA, "EQ1", base.plusDays(2), SlotStatus.AVAILABLE);
        slot(labA, "EQ2", base.plusDays(2), SlotStatus.AVAILABLE);
        slot(labB, "EQ1", base.plusDays(2), SlotStatus.AVAILABLE);

        String from = base.plusDays(1).toOffsetDateTime().toString();
        search("?laboratoryId=" + labA + "&equipmentCode=EQ1&from=" + from.replace("+", "%2B"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].equipmentCode").value("EQ1"))
                .andExpect(jsonPath("$.content[0].laboratoryId").value(labA));
    }

    @Test
    void paginacion_ordenadaPorStartTime() throws Exception {
        for (int i = 0; i < 5; i++) slot(labA, "EQ1", base.plusHours(i * 2L), SlotStatus.AVAILABLE);

        search("?laboratoryId=" + labA + "&page=1&size=2")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void parametrosInvalidos_400() throws Exception {
        search("?size=0").andExpect(status().isBadRequest());
        search("?page=-1").andExpect(status().isBadRequest());
        search("?from=no-es-fecha").andExpect(status().isBadRequest());
    }

    @Test
    void sinToken_401() throws Exception {
        mvc.perform(get("/slots")).andExpect(status().isUnauthorized());
    }
}
