package com.pbanakar.huntlog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbanakar.huntlog.dto.request.LoginRequest;
import com.pbanakar.huntlog.dto.request.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/auth/register → 201, returns JWT token, email, name")
    void register_success() throws Exception {
        String email = "alice_" + UUID.randomUUID() + "@test.com";
        RegisterRequest request = new RegisterRequest();
        request.setName("Alice Dev");
        request.setEmail(email);
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Alice Dev"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register with duplicate email → 409 Conflict")
    void register_duplicateEmail_returns409() throws Exception {
        String email = "bob_" + UUID.randomUUID() + "@test.com";
        RegisterRequest request = new RegisterRequest();
        request.setName("Bob Dev");
        request.setEmail(email);
        request.setPassword("password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Attempt same email again
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already registered"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with valid credentials → 200, returns token")
    void login_success() throws Exception {
        String email = "charlie_" + UUID.randomUUID() + "@test.com";
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Charlie Dev");
        registerReq.setEmail(email);
        registerReq.setPassword("secretPass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(email);
        loginReq.setPassword("secretPass123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.name").value("Charlie Dev"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login with invalid password → 401 Unauthorized")
    void login_invalidPassword_returns401() throws Exception {
        String email = "dave_" + UUID.randomUUID() + "@test.com";
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setName("Dave Dev");
        registerReq.setEmail(email);
        registerReq.setPassword("secretPass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest();
        loginReq.setEmail(email);
        loginReq.setPassword("wrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/applications without token → 401 Unauthorized")
    void getApplications_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Unauthorized"));
    }
}
