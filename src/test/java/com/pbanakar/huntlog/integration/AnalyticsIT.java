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
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AnalyticsIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("huntlog")
            .withUsername("huntlog")
            .withPassword("huntlog");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ANALYTICS_URL = "/api/v1/analytics";
    private static final String APPLICATIONS_URL = "/api/v1/applications";

    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        userToken = registerAndGetToken("analytics_user_" + UUID.randomUUID() + "@test.com", "Analytics User");
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

    private Long createApplication(String token, String company, String role) throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setCompany(company);
        request.setRole(role);

        MvcResult result = mockMvc.perform(post(APPLICATIONS_URL)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("id").asLong();
    }

    @Test
    @DisplayName("GET /api/v1/analytics with no token → 401 Unauthorized")
    void getAnalytics_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get(ANALYTICS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/analytics with token and no applications → 200 with zeros and nulls")
    void getAnalytics_noApplications_returnsZerosAndNulls() throws Exception {
        mockMvc.perform(get(ANALYTICS_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(0))
                .andExpect(jsonPath("$.responseRate").value(0.0))
                .andExpect(jsonPath("$.averageDaysToFirstUpdate").value(nullValue()))
                .andExpect(jsonPath("$.oldestPendingDays").value(nullValue()))
                .andExpect(jsonPath("$.topCompaniesByApplications", hasSize(0)))
                .andExpect(jsonPath("$.byStatus.APPLIED").value(0))
                .andExpect(jsonPath("$.byStatus.SCREENING").value(0))
                .andExpect(jsonPath("$.byStatus.INTERVIEW").value(0))
                .andExpect(jsonPath("$.byStatus.OFFER").value(0))
                .andExpect(jsonPath("$.byStatus.ACCEPTED").value(0))
                .andExpect(jsonPath("$.byStatus.REJECTED").value(0))
                .andExpect(jsonPath("$.byStatus.WITHDRAWN").value(0));
    }

    @Test
    @DisplayName("Create 3 applications (2 APPLIED, 1 moved to SCREENING) → computes byStatus and responseRate (33.3%)")
    void getAnalytics_withApplications_computesMetricsCorrectly() throws Exception {
        createApplication(userToken, "Google", "SWE");
        createApplication(userToken, "Amazon", "SDE");
        Long metaAppId = createApplication(userToken, "Meta", "Staff SWE");

        // Move Meta app from APPLIED -> SCREENING
        UpdateApplicationRequest updateReq = new UpdateApplicationRequest();
        updateReq.setStatus(ApplicationStatus.SCREENING);
        mockMvc.perform(put(APPLICATIONS_URL + "/" + metaAppId)
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk());

        // responseRate = (3 total - 2 APPLIED - 0 WITHDRAWN) / 3 * 100 = 33.3%
        mockMvc.perform(get(ANALYTICS_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalApplications").value(3))
                .andExpect(jsonPath("$.byStatus.APPLIED").value(2))
                .andExpect(jsonPath("$.byStatus.SCREENING").value(1))
                .andExpect(jsonPath("$.byStatus.INTERVIEW").value(0))
                .andExpect(jsonPath("$.responseRate").value(33.3))
                .andExpect(jsonPath("$.appliedThisWeek").value(3))
                .andExpect(jsonPath("$.appliedThisMonth").value(3))
                .andExpect(jsonPath("$.oldestPendingDays").value(greaterThanOrEqualTo(0)));
    }

    @Test
    @DisplayName("Verify topCompaniesByApplications returns companies sorted descending by count, then alphabetically")
    void getAnalytics_topCompanies_sortedCorrectly() throws Exception {
        // Create 2 for Google, 3 for Microsoft, 1 for Apple, 1 for Amazon
        createApplication(userToken, "Google", "SWE 1");
        createApplication(userToken, "Google", "SWE 2");
        createApplication(userToken, "Microsoft", "SDE 1");
        createApplication(userToken, "Microsoft", "SDE 2");
        createApplication(userToken, "Microsoft", "SDE 3");
        createApplication(userToken, "Apple", "iOS Dev");
        createApplication(userToken, "Amazon", "Cloud Eng");

        mockMvc.perform(get(ANALYTICS_URL)
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.topCompaniesByApplications", hasSize(4)))
                .andExpect(jsonPath("$.topCompaniesByApplications[0].company").value("Microsoft"))
                .andExpect(jsonPath("$.topCompaniesByApplications[0].count").value(3))
                .andExpect(jsonPath("$.topCompaniesByApplications[1].company").value("Google"))
                .andExpect(jsonPath("$.topCompaniesByApplications[1].count").value(2))
                // Amazon and Apple both have count 1, Amazon should be first alphabetically
                .andExpect(jsonPath("$.topCompaniesByApplications[2].company").value("Amazon"))
                .andExpect(jsonPath("$.topCompaniesByApplications[2].count").value(1))
                .andExpect(jsonPath("$.topCompaniesByApplications[3].company").value("Apple"))
                .andExpect(jsonPath("$.topCompaniesByApplications[3].count").value(1));
    }
}
