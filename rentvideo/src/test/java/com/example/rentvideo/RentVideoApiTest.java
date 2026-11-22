package com.example.rentvideo;

import com.example.rentvideo.entity.Role;
import com.example.rentvideo.entity.User;
import com.example.rentvideo.entity.Video;
import com.example.rentvideo.repository.UserRepository;
import com.example.rentvideo.repository.VideoRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RentVideoApiTest {

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired VideoRepository videos;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        saveUser("admin@test.com", "admin123", Role.ADMIN);
        saveUser("customer@test.com", "cust1234", Role.CUSTOMER);
    }

    private void saveUser(String email, String password, Role role) {
        User u = new User();
        u.setEmail(email);
        u.setPassword(encoder.encode(password));
        u.setFirstName("First");
        u.setLastName("Last");
        u.setRole(role);
        users.save(u);
    }

    private static final String VIDEO_JSON =
            "{\"title\":\"Inception\",\"director\":\"Christopher Nolan\",\"genre\":\"Sci-Fi\"}";

    // ---------- registration & login ----------

    @Test
    void registerDefaultsToCustomerAndHidesPassword() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"new@test.com\",\"password\":\"secret1\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void registerStoresBcryptHash() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"hash@test.com\",\"password\":\"secret1\",\"firstName\":\"A\",\"lastName\":\"B\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        User saved = users.findByEmail("hash@test.com").orElseThrow();
        Assertions.assertTrue(saved.getPassword().startsWith("$2"));
    }

    @Test
    void registerDuplicateEmailReturns409() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"admin@test.com\",\"password\":\"secret1\",\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registerInvalidBodyReturns400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void loginWithValidCredentialsReturnsProfile() throws Exception {
        mvc.perform(post("/api/auth/login").with(httpBasic("customer@test.com", "cust1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("customer@test.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mvc.perform(post("/api/auth/login").with(httpBasic("customer@test.com", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- authorization ----------

    @Test
    void videosRequireAuthentication() throws Exception {
        mvc.perform(get("/api/videos")).andExpect(status().isUnauthorized());
    }

    @Test
    void customerCanBrowseOnlyAvailableVideos() throws Exception {
        videos.save(new Video("A", "Dir", "Drama", true));
        videos.save(new Video("B", "Dir", "Drama", false));
        mvc.perform(get("/api/videos").with(httpBasic("customer@test.com", "cust1234")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title").value("A"));
    }

    @Test
    void customerCannotCreateUpdateOrDelete() throws Exception {
        Video v = videos.save(new Video("A", "Dir", "Drama", true));
        mvc.perform(post("/api/videos").with(httpBasic("customer@test.com", "cust1234"))
                        .contentType(MediaType.APPLICATION_JSON).content(VIDEO_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/videos/" + v.getId()).with(httpBasic("customer@test.com", "cust1234"))
                        .contentType(MediaType.APPLICATION_JSON).content(VIDEO_JSON))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/videos/" + v.getId()).with(httpBasic("customer@test.com", "cust1234")))
                .andExpect(status().isForbidden());
    }

    // ---------- admin video management ----------

    @Test
    void adminCanCreateVideoAndItDefaultsToAvailable() throws Exception {
        mvc.perform(post("/api/videos").with(httpBasic("admin@test.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content(VIDEO_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    void adminCreateWithMissingFieldsReturns400() throws Exception {
        mvc.perform(post("/api/videos").with(httpBasic("admin@test.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanUpdateAndDeleteVideo() throws Exception {
        Video v = videos.save(new Video("Old", "Dir", "Drama", true));

        mvc.perform(put("/api/videos/" + v.getId()).with(httpBasic("admin@test.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New\",\"director\":\"Dir2\",\"genre\":\"Action\",\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New"))
                .andExpect(jsonPath("$.available").value(false));

        mvc.perform(delete("/api/videos/" + v.getId()).with(httpBasic("admin@test.com", "admin123")))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/videos/" + v.getId()).with(httpBasic("admin@test.com", "admin123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void unknownVideoReturns404() throws Exception {
        mvc.perform(get("/api/videos/99999").with(httpBasic("customer@test.com", "cust1234")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/videos/99999").with(httpBasic("admin@test.com", "admin123")))
                .andExpect(status().isNotFound());
    }
}
