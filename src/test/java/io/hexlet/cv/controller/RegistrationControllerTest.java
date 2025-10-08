package io.hexlet.cv.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.hexlet.cv.dto.user.auth.RegistrationRequestDTO;
import io.hexlet.cv.mapper.RegistrationMapper;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;


@SpringBootTest
@AutoConfigureMockMvc
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper om;

    @Autowired
    private RegistrationMapper registrationMapper;

    @AfterEach
    public void garbageDbDelete() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setUp() {

        userRepository.deleteAll();

        mockMvc = MockMvcBuilders.webAppContextSetup(wac).defaultResponseCharacterEncoding(StandardCharsets.UTF_8)
                .apply(springSecurity()).build();
    }

    @Test
    public void testCreateUser() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("test@gmail.com");
        data.setPassword("test_password");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isFound());

        var user = userRepository.findByEmail(data.getEmail()).orElse(null);
        assertThat(user).isNotNull();
        assertThat(user.getFirstName()).isEqualTo(data.getFirstName());
        assertThat(user.getLastName()).isEqualTo(data.getLastName());
        assertThat(user.getId()).isNotNull();
    }

    @Test
    public void testDisposableEmail() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("test@sharklasers.com");
        data.setPassword("test_pass_123");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.email")
                        .value("Запрещено использовать одноразовые email"));
    }

    @Test
    public void testEmailPresentInDB() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("test@gmail.com");
        data.setPassword("test_password123");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var newUserData = registrationMapper.map(data);
        newUserData.setEncryptedPassword("123456");
        newUserData.setRole(RoleType.CANDIDATE);

        userRepository.save(newUserData);

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isConflict());
    }

    @Test
    public void testIndex() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk()).andReturn().getResponse();
    }

    @Test
    public void testNonExistentEmail() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("test@goopmal.com");
        data.setPassword("test_pass_123");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.email").value("Домен в email не существует"));
    }

    @Test
    public void testNotCorrectEmail() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("testgmail.com");
        data.setPassword("test_pass_123");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.email").value("Укажите корректный email-адрес"));
    }
    // email с односимвольным TLD ------------
    // проверять нет смысла потому что валидация через запрос в
    // https://cloudflare-dns.com/ отсечет такие email
    // смысл только если запрос не пройдет
    // !!!!!!!!!!!!!!
    /*
    @Test
    public void testNotCorrectTLDEmail() throws Exception {
        var data = new RegInputDTO(); data.setEmail("test@gmail.a");
        data.setPassword("test_pass_123"); data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/users/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request) .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.email")
                        .value("TLD email должен содержать как минимум 2 символа")); //
     // .value("Домен в email не существует"));
    }
*/

    @Test
    public void testNotValidShortPassword() throws Exception {
        var data = new RegistrationRequestDTO();
        data.setEmail("test@gmail.com");
        data.setPassword("test_p");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.password").value("Пароль должен быть не менее 8 символов"));
    }

    @Test
    public void testSimplePassword() throws Exception {
        // имя совпадает
        var data = new RegistrationRequestDTO();
        data.setEmail("test@gmail.com");
        data.setPassword("firstName");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        var request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors.password")
                        .value("Пароль слишком простой — не должен совпадать с email или именем"));

        // фамилия совпадает
        data.setEmail("test@gmail.com");
        data.setPassword("lastName");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.errors.password")
                        .value("Пароль слишком простой — не должен совпадать с email или именем"));
        // email совпадает
        data.setEmail("test@gmail.com");
        data.setPassword("test@gmail.com");
        data.setFirstName("firstName");
        data.setLastName("lastName");

        request = post("/ru/users").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(data));

        mockMvc.perform(request).andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.errors.password")
                        .value("Пароль слишком простой — не должен совпадать с email или именем"));
    }

// Inertia тесты ---------
    // исключение при регистрации цже имеющегося в базе юзера
    // проверяем что 303, что редирект и что флэш сообщения передаются
    @Test

    void testInertiaDuplicateEmail() throws Exception {
        var existing = new User();
        existing.setEmail("test@google.com");
        existing.setFirstName("testFirstName");
        existing.setLastName("testLastName");
        existing.setEncryptedPassword("password");
        existing.setRole(RoleType.CANDIDATE);

        userRepository.save(existing);

        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@google.com");
        dto.setPassword("1234qwery");
        dto.setFirstName("testFirstName");
        dto.setLastName("testLastName");

        //  POST
        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));
    }


    @Test
    void testInertiaSimplePassword() throws Exception {
        // имя совпадает
        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@gmail.com");
        dto.setPassword("firstName");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));
    }

    @Test
    void testInertiaNotValidShortPassword() throws Exception {
        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@gmail.com");
        dto.setPassword("test_p");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))

                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.password").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));


    }

    @Test
    void testInertiaNotCorrectEmail() throws Exception {
        var dto = new RegistrationRequestDTO();
        dto.setEmail("testgmail.com");
        dto.setPassword("test_pass_123");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));
    }

    @Test
    void testInertiaNonExistentEmail() throws Exception {
        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@goopmal.com");
        dto.setPassword("test_pass_123");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));
    }

    @Test
    void testInertiaDisposableEmail() throws Exception {
        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@sharklasers.com");
        dto.setPassword("test_password123");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.component").value("Users/Register"))
                .andExpect(jsonPath("$.props.locale").value("ru"))
                .andExpect(jsonPath("$.props.flash.errors").exists())
                .andExpect(jsonPath("$.props.flash.errors.email").isNotEmpty())
                .andExpect(jsonPath("$.url").value("/ru/users"));
    }

    @Test
    void testInertiaRegistrationUserCookies() throws Exception {
        var dto = new RegistrationRequestDTO();
        dto.setEmail("test@gmail.com");
        dto.setPassword("test_password");
        dto.setFirstName("firstName");
        dto.setLastName("lastName");

        mockMvc.perform(post("/ru/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                        .header("X-Inertia", "true")
                        .header("Referer", "/ru/users/sign_up"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/ru/dashboard"))
                .andExpect(flash().attributeCount(0))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("access_token"))))
                .andExpect(header().stringValues(HttpHeaders.SET_COOKIE,
                        Matchers.hasItem(Matchers.containsString("refresh_token"))));
    }
}
