package com.rentvideo;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** End-to-end: security rules, role checks, rental limit, return flow (H2 in MySQL mode). */
@SpringBootTest
@AutoConfigureMockMvc
class RentVideoApiIntegrationTest {

    @Autowired MockMvc mvc;

    private ResultActions register(String email, String role) throws Exception {
        String roleJson = role == null ? "" : ",\"role\":\"" + role + "\"";
        return mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"Password123\",\"firstName\":\"A\","
                        + "\"lastName\":\"B\"" + roleJson + "}"));
    }

    private String login(String email) throws Exception {
        String basic = Base64.getEncoder().encodeToString((email + ":Password123").getBytes(StandardCharsets.UTF_8));
        String body = mvc.perform(post("/auth/login").header("Authorization", "Basic " + basic))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    private long createVideo(String adminToken, String title) throws Exception {
        String body = mvc.perform(post("/videos").header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"director\":\"D\",\"genre\":\"Drama\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.available").value(true))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private ResultActions authedPost(String token, String url) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token));
    }

    @Test
    void fullFlow() throws Exception {
        // registration: role defaults to CUSTOMER, duplicates rejected, bad input rejected
        register("admin@test.com", "ADMIN").andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        register("cust@test.com", null).andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.password").doesNotExist());
        register("cust@test.com", null).andExpect(status().isConflict());
        register("not-an-email", null).andExpect(status().isBadRequest());

        // login: wrong password -> 401
        String bad = Base64.getEncoder().encodeToString("cust@test.com:wrong".getBytes(StandardCharsets.UTF_8));
        mvc.perform(post("/auth/login").header("Authorization", "Basic " + bad)).andExpect(status().isUnauthorized());

        String admin = login("admin@test.com");
        String cust = login("cust@test.com");

        // authentication / authorization
        mvc.perform(get("/videos")).andExpect(status().isUnauthorized());
        mvc.perform(get("/videos").header("Authorization", "Bearer garbage")).andExpect(status().isUnauthorized());
        mvc.perform(post("/videos").header("Authorization", "Bearer " + cust)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"X\",\"director\":\"D\",\"genre\":\"G\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/videos/1").header("Authorization", "Bearer " + cust)).andExpect(status().isForbidden());

        // admin manages videos
        long v1 = createVideo(admin, "Movie 1");
        long v2 = createVideo(admin, "Movie 2");
        long v3 = createVideo(admin, "Movie 3");

        // rental limit of two
        authedPost(cust, "/videos/" + v1 + "/rent").andExpect(status().isCreated());
        authedPost(cust, "/videos/" + v2 + "/rent").andExpect(status().isCreated());
        authedPost(cust, "/videos/" + v3 + "/rent").andExpect(status().isConflict());

        // rented videos disappear from the available list; renting again is a conflict
        mvc.perform(get("/videos").header("Authorization", "Bearer " + admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + v1 + ")]").isEmpty());
        authedPost(admin, "/videos/" + v1 + "/rent").andExpect(status().isConflict());

        // an actively rented video cannot be deleted
        mvc.perform(delete("/videos/" + v1).header("Authorization", "Bearer " + admin))
                .andExpect(status().isConflict());

        // return frees the slot and the video
        authedPost(cust, "/videos/" + v1 + "/return").andExpect(status().isOk());
        authedPost(cust, "/videos/" + v1 + "/return").andExpect(status().isNotFound());   // already returned
        authedPost(cust, "/videos/" + v3 + "/rent").andExpect(status().isCreated());

        mvc.perform(get("/rentals/me").param("activeOnly", "true").header("Authorization", "Bearer " + cust))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // 404s
        authedPost(admin, "/videos/999999/rent").andExpect(status().isNotFound());
        mvc.perform(get("/videos/999999").header("Authorization", "Bearer " + cust)).andExpect(status().isNotFound());

        // admin update + delete (video 1 is back in stock)
        mvc.perform(put("/videos/" + v1).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\",\"director\":\"D\",\"genre\":\"G\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Renamed"));
        mvc.perform(delete("/videos/" + v1).header("Authorization", "Bearer " + admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/videos/" + v1).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
    }
}
