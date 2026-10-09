package com.pbanakar.huntlog.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbanakar.huntlog.dto.request.CreateApplicationRequest;
import com.pbanakar.huntlog.dto.request.RegisterRequest;
import com.pbanakar.huntlog.dto.request.UpdateApplicationRequest;
import com.pbanakar.huntlog.enums.ApplicationStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobApplicationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("huntlog")
            .withUsername("huntlog")
            .withPassword("huntlog");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String BASE_URL = "/api/v1/applications";

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndGetToken("user_" + UUID.randomUUID() + "@huntlog.test", "Test User");
    }

    private String registerAndGetToken(String email, String name) throws Exception {
        RegisterRequest registerReq = new RegisterRequest();
        registerReq.setEmail(email);
        registerReq.setName(name);
        registerReq.setPassword("password123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("token").asText();
    }

    private String createApplication(String token, String company, String role) throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setCompany(company);
        request.setRole(role);

        MvcResult result = mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    @Test
    @DisplayName("POST /applications → 201, correct fields, status=APPLIED, allowedNextStatuses contains SCREENING")
    void post_createsApplication() throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setCompany("Google");
        request.setRole("Software Engineer");
        request.setJobUrl("https://careers.google.com/jobs/123");
        request.setNotes("Dream job");
        request.setLocation("Mountain View, CA");

        mockMvc.perform(post(BASE_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.company").value("Google"))
                .andExpect(jsonPath("$.role").value("Software Engineer"))
                .andExpect(jsonPath("$.status").value("APPLIED"))
                .andExpect(jsonPath("$.appliedDate").isNotEmpty())
                .andExpect(jsonPath("$.jobUrl").value("https://careers.google.com/jobs/123"))
                .andExpect(jsonPath("$.notes").value("Dream job"))
                .andExpect(jsonPath("$.location").value("Mountain View, CA"))
                .andExpect(jsonPath("$.allowedNextStatuses", hasItem("SCREENING")));
    }

    @Test
    @DisplayName("GET /applications returns paginated results for authenticated user")
    void get_returnsPaginatedResults() throws Exception {
        createApplication(userToken, "PaginationTest1", "SWE");
        createApplication(userToken, "PaginationTest2", "SDE");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(2)))
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @DisplayName("GET /applications?status=APPLIED filters correctly")
    void get_filtersbyStatus() throws Exception {
        createApplication(userToken, "FilterCompany", "Analyst");

        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + userToken)
                        .param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("APPLIED"))));
    }

    @Test
    @DisplayName("GET /applications/{id} returns correct application")
    void getById_returnsApplication() throws Exception {
        String body = createApplication(userToken, "GetByIdCo", "PM");
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get(BASE_URL + "/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.company").value("GetByIdCo"));
    }

    @Test
    @DisplayName("PUT with valid transition → 200, status updated")
    void put_validTransition_updates() throws Exception {
        String body = createApplication(userToken, "TransitionCo", "SWE");
        Long id = objectMapper.readTree(body).get("id").asLong();

        UpdateApplicationRequest update = new UpdateApplicationRequest();
        update.setStatus(ApplicationStatus.SCREENING);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"))
                .andExpect(jsonPath("$.allowedNextStatuses", hasItem("INTERVIEW")));
    }

    @Test
    @DisplayName("PUT with INVALID transition → 422, error message contains from and to status")
    void put_invalidTransition_returns422() throws Exception {
        String body = createApplication(userToken, "InvalidTransCo", "SDE");
        Long id = objectMapper.readTree(body).get("id").asLong();

        UpdateApplicationRequest update = new UpdateApplicationRequest();
        update.setStatus(ApplicationStatus.OFFER);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("APPLIED")))
                .andExpect(jsonPath("$.message", containsString("OFFER")));
    }

    @Test
    @DisplayName("DELETE → 204, subsequent GET → 404")
    void delete_thenGet_returns404() throws Exception {
        String body = createApplication(userToken, "DeleteCo", "QA");
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete(BASE_URL + "/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /applications/{nonExistentId} → 404")
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("99999")));
    }

    @Test
    @DisplayName("Per-user data isolation: User B cannot access, update, or delete User A's application")
    void userIsolation_userBCannotAccessUserAData() throws Exception {
        // User A creates an application
        String userAToken = registerAndGetToken("userA_" + UUID.randomUUID() + "@test.com", "User A");
        String userBToken = registerAndGetToken("userB_" + UUID.randomUUID() + "@test.com", "User B");

        String bodyA = createApplication(userAToken, "UserA Company", "Principal Architect");
        Long appAId = objectMapper.readTree(bodyA).get("id").asLong();

        // User B attempts to GET User A's application -> 404
        mockMvc.perform(get(BASE_URL + "/" + appAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B attempts to UPDATE User A's application -> 404
        UpdateApplicationRequest update = new UpdateApplicationRequest();
        update.setStatus(ApplicationStatus.SCREENING);
        mockMvc.perform(put(BASE_URL + "/" + appAId)
                        .header("Authorization", "Bearer " + userBToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isNotFound());

        // User B attempts to DELETE User A's application -> 404
        mockMvc.perform(delete(BASE_URL + "/" + appAId)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isNotFound());

        // User B's application list does NOT contain User A's application
        mockMvc.perform(get(BASE_URL)
                        .header("Authorization", "Bearer " + userBToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }
}
