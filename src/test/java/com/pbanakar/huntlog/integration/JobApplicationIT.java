package com.pbanakar.huntlog.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pbanakar.huntlog.dto.request.CreateApplicationRequest;
import com.pbanakar.huntlog.dto.request.UpdateApplicationRequest;
import com.pbanakar.huntlog.enums.ApplicationStatus;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JobApplicationIT {

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

    private static final String BASE_URL = "/api/v1/applications";

    private String createApplication(String company, String role) throws Exception {
        CreateApplicationRequest request = new CreateApplicationRequest();
        request.setCompany(company);
        request.setRole(role);

        MvcResult result = mockMvc.perform(post(BASE_URL)
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
    @DisplayName("GET /applications returns paginated results")
    void get_returnsPaginatedResults() throws Exception {
        createApplication("PaginationTest1", "SWE");
        createApplication("PaginationTest2", "SDE");

        mockMvc.perform(get(BASE_URL)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber())
                .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    @DisplayName("GET /applications?status=APPLIED filters correctly")
    void get_filtersbyStatus() throws Exception {
        createApplication("FilterCompany", "Analyst");

        mockMvc.perform(get(BASE_URL)
                        .param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].status", everyItem(is("APPLIED"))));
    }

    @Test
    @DisplayName("GET /applications/{id} returns correct application")
    void getById_returnsApplication() throws Exception {
        String body = createApplication("GetByIdCo", "PM");
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.company").value("GetByIdCo"));
    }

    @Test
    @DisplayName("PUT with valid transition → 200, status updated")
    void put_validTransition_updates() throws Exception {
        String body = createApplication("TransitionCo", "SWE");
        Long id = objectMapper.readTree(body).get("id").asLong();

        UpdateApplicationRequest update = new UpdateApplicationRequest();
        update.setStatus(ApplicationStatus.SCREENING);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCREENING"))
                .andExpect(jsonPath("$.allowedNextStatuses", hasItem("INTERVIEW")));
    }

    @Test
    @DisplayName("PUT with INVALID transition → 422, error message contains from and to status")
    void put_invalidTransition_returns422() throws Exception {
        String body = createApplication("InvalidTransCo", "SDE");
        Long id = objectMapper.readTree(body).get("id").asLong();

        UpdateApplicationRequest update = new UpdateApplicationRequest();
        update.setStatus(ApplicationStatus.OFFER);

        mockMvc.perform(put(BASE_URL + "/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("APPLIED")))
                .andExpect(jsonPath("$.message", containsString("OFFER")));
    }

    @Test
    @DisplayName("DELETE → 204, subsequent GET → 404")
    void delete_thenGet_returns404() throws Exception {
        String body = createApplication("DeleteCo", "QA");
        Long id = objectMapper.readTree(body).get("id").asLong();

        mockMvc.perform(delete(BASE_URL + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE_URL + "/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /applications/{nonExistentId} → 404")
    void getById_nonExistent_returns404() throws Exception {
        mockMvc.perform(get(BASE_URL + "/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message", containsString("99999")));
    }
}
