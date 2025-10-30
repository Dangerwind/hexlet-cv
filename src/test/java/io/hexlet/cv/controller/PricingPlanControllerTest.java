package io.hexlet.cv.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.hexlet.cv.model.User;
import io.hexlet.cv.model.enums.RoleType;
import io.hexlet.cv.model.marketing.PricingPlan;
import io.hexlet.cv.repository.PricingPlanRepository;
import io.hexlet.cv.repository.UserRepository;
import io.hexlet.cv.util.JWTUtils;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class PricingPlanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PricingPlanRepository pricingPlanRepository;

    @Autowired
    private JWTUtils jwtUtils;

    @Autowired
    private BCryptPasswordEncoder encoder;

    private static final String ADMIN_EMAIL = "admin_pricing@example.com";
    private static final String CANDIDATE_EMAIL = "candidate_pricing@example.com";

    private String adminToken;
    private String candidateToken;
    private PricingPlan testPricingPlan;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        pricingPlanRepository.deleteAll();

        var admin = new User();
        admin.setEmail(ADMIN_EMAIL);
        admin.setEncryptedPassword(encoder.encode("password"));
        admin.setRole(RoleType.ADMIN);
        userRepository.save(admin);


        var candidate = new User();
        candidate.setEmail(CANDIDATE_EMAIL);
        candidate.setEncryptedPassword(encoder.encode("password"));
        candidate.setRole(RoleType.CANDIDATE);
        userRepository.save(candidate);

        adminToken = jwtUtils.generateAccessToken(admin.getEmail());
        candidateToken = jwtUtils.generateAccessToken(candidate.getEmail());

        testPricingPlan = new PricingPlan();
        testPricingPlan.setName("Test Plan");
        testPricingPlan.setDescription("Test Description");
        testPricingPlan.setOriginalPrice(100.0);
        testPricingPlan.setDiscountPercent(10.0);
        pricingPlanRepository.save(testPricingPlan);
    }

    @AfterEach
    void tearDown() {
        userRepository.deleteAll();
        pricingPlanRepository.deleteAll();
    }

    @Test
    void testGetPricingSectionAsAdmin() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing")
                        .cookie(new Cookie("access_token", adminToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.props.activeMainSection").value("marketing"))
                .andExpect(jsonPath("$.props.activeSubSection").value("pricing"))
                .andExpect(jsonPath("$.props.pageTitle").value("Тарифы и скидки"))
                .andExpect(jsonPath("$.props.pricing").exists());
    }

    @Test
    void testGetPricingSectionAsCandidateForbidden() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing")
                        .cookie(new Cookie("access_token", candidateToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetPricingCreateFormAsAdmin() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing/create")
                        .cookie(new Cookie("access_token", adminToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.props.activeMainSection").value("marketing"))
                .andExpect(jsonPath("$.props.activeSubSection").value("pricing"));
    }

    @Test
    void testGetPricingCreateFormAsCandidateForbidden() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing/create")
                        .cookie(new Cookie("access_token", candidateToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGetPricingEditFormAsAdmin() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing/" + testPricingPlan.getId() + "/edit")
                        .cookie(new Cookie("access_token", adminToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.props.activeMainSection").value("marketing"))
                .andExpect(jsonPath("$.props.activeSubSection").value("pricing"))
                .andExpect(jsonPath("$.props.pricing.id").value(testPricingPlan.getId()));
    }

    @Test
    void testGetPricingEditFormAsCandidateForbidden() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/pricing/" + testPricingPlan.getId() + "/edit")
                        .cookie(new Cookie("access_token", candidateToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreatePricingAsAdmin() throws Exception {
        String createPricingJson = """
                {
                    "name": "Новый тарифный план",
                    "original_price": 150.0,
                    "discount_percent": 20.0,
                    "description": "Описание нового плана"
                }
                """;

        mockMvc.perform(post("/ru/admin/marketing/pricing")
                        .cookie(new Cookie("access_token", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPricingJson)
                        .header("X-Inertia", "true"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ru/admin/marketing/pricing"));

        assertEquals(2, pricingPlanRepository.count());
    }


    @Test
    void testCreatePricingAsCandidateForbidden() throws Exception {
        String createPricingJson = """
                {
                    "name": "Новый план",
                    "original_price": 150.0
                }
                """;

        mockMvc.perform(post("/ru/admin/marketing/pricing")
                        .cookie(new Cookie("access_token", candidateToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPricingJson))
                .andExpect(status().isForbidden());
    }

    @Test
    void testCreatePricingInvalidData() throws Exception {
        String invalidPricingJson = """
                {
                    "name": "",
                    "original_price": -100.0
                }
                """;

        mockMvc.perform(post("/ru/admin/marketing/pricing")
                        .cookie(new Cookie("access_token", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidPricingJson))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void testUpdatePricing_AsAdmin() throws Exception {
        String updatePricingJson = """
                {
                    "name": "Обновленное название",
                    "original_price": 200.0,
                    "discount_percent": 25.0,
                    "description": "Обновленное описание"
                }
                """;

        mockMvc.perform(put("/ru/admin/marketing/pricing/" + testPricingPlan.getId())
                        .cookie(new Cookie("access_token", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePricingJson)
                        .header("X-Inertia", "true"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/ru/admin/marketing/pricing"));

        PricingPlan updated = pricingPlanRepository.findById(testPricingPlan.getId()).orElseThrow();
        assertEquals("Обновленное название", updated.getName());
        assertEquals(200.0, updated.getOriginalPrice());
        assertEquals(25.0, updated.getDiscountPercent());
        assertEquals(150.0, updated.getFinalPrice());
    }

    @Test
    void testUpdatePricingNotFound() throws Exception {
        String updatePricingJson = """
                {
                    "name": "Обновленное название"
                }
                """;

        mockMvc.perform(put("/ru/admin/marketing/pricing/99999")
                        .cookie(new Cookie("access_token", adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePricingJson))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeletePricingAsAdmin() throws Exception {
        mockMvc.perform(delete("/ru/admin/marketing/pricing/" + testPricingPlan.getId())
                        .cookie(new Cookie("access_token", adminToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "/ru/admin/marketing/pricing"));


        assertFalse(pricingPlanRepository.existsById(testPricingPlan.getId()));
    }

    @Test
    void testDeletePricingAsCandidateForbidden() throws Exception {
        mockMvc.perform(delete("/ru/admin/marketing/pricing/" + testPricingPlan.getId())
                        .cookie(new Cookie("access_token", candidateToken)))
                .andExpect(status().isForbidden());
    }

    @Test
    void testInvalidSection_NotFound() throws Exception {
        mockMvc.perform(get("/ru/admin/marketing/invalid-section")
                        .cookie(new Cookie("access_token", adminToken))
                        .header("X-Inertia", "true"))
                .andExpect(status().isSeeOther());
    }
}