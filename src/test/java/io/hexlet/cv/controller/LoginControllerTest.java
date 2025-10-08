package io.hexlet.cv.controller;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hexlet.cv.dto.user.auth.LoginRequestDTO;
import io.hexlet.cv.model.User;
import io.hexlet.cv.model.enums.RoleType;
import io.hexlet.cv.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@AutoConfigureMockMvc
public class LoginControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BCryptPasswordEncoder encoder;

    private User userData;
    private final String testPassword = "test_password";

    @AfterEach
    public void garbageDbDelete() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setUp() {

        userRepository.deleteAll();

        userData = new User();
        userData.setEmail("test@gmail.com");
        userData.setEncryptedPassword(encoder.encode(testPassword));
        userData.setFirstName("firstName");
        userData.setLastName("lastName");
        userData.setRole(RoleType.CANDIDATE);

        userRepository.save(userData);

        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity()).build();
    }

    @Test
    public void testLoginUser() throws Exception {
        var data = new LoginRequestDTO();
        data.setEmail(userData.getEmail());
        data.setPassword(testPassword);

        var request = post("/ru/users/sign_in").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isFound())
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("access_token"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("refresh_token"))));
    }

    @Test
    public void testBadEmailUser() throws Exception {

        var data = new LoginRequestDTO();
        data.setEmail("new_email@yandex.ru");
        data.setPassword(testPassword);

        var request = post("/ru/users/sign_in").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.email")
                        .value("Пользователь не найден"));
    }

    @Test
    public void testBadPasswordUser() throws Exception {
        var data = new LoginRequestDTO();
        data.setEmail(userData.getEmail());
        data.setPassword("another_password");

        var request = post("/ru/users/sign_in").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.password")
                        .value("Неверный пароль"));
    }

    // Inertia тесты ---------
    @Test
    void testInertiaBadPasswordUser() throws Exception {
        var dto = new LoginRequestDTO();
        dto.setEmail(userData.getEmail());
        dto.setPassword("another_password");

        mockMvc.perform(post("/ru/users/sign_in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_in"))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users/sign_in"));
    }

    @Test
    void testInertiaBadEmailUser() throws Exception {
        var dto = new LoginRequestDTO();
        dto.setEmail("new_email@yandex.ru");
        dto.setPassword(testPassword);

        mockMvc.perform(post("/ru/users/sign_in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_in"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users/sign_in"));
    }

    @Test
    void testInertiaLoginUserCookies() throws Exception {
        var dto = new LoginRequestDTO();
        dto.setEmail(userData.getEmail());
        dto.setPassword(testPassword);

        mockMvc.perform(post("/ru/users/sign_in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_in"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ru/dashboard"))
                .andExpect(flash().attributeCount(0))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("access_token"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("refresh_token"))));
    }

}
