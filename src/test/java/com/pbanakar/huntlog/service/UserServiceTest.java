package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JobApplicationRepository jobApplicationRepository;

    @InjectMocks
    private UserService userService;

    private static final Long USER_ID = 42L;

    @BeforeEach
    void setUpSecurityContext() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("user@example.com", null, Collections.emptyList());
        auth.setDetails(USER_ID);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getPreferences returns current user preference")
    void testGetPreferences() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmailRemindersEnabled(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UserPreferencesResponse response = userService.getPreferences();

        assertThat(response.getEmailRemindersEnabled()).isTrue();
        verify(userRepository).findById(USER_ID);
    }

    @Test
    @DisplayName("updatePreferences updates user email reminder setting")
    void testUpdatePreferences() {
        User user = new User();
        user.setId(USER_ID);
        user.setEmailRemindersEnabled(true);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        UpdatePreferencesRequest req = new UpdatePreferencesRequest(false);
        UserPreferencesResponse response = userService.updatePreferences(req);

        assertThat(response.getEmailRemindersEnabled()).isFalse();
        assertThat(user.getEmailRemindersEnabled()).isFalse();
        verify(userRepository).save(user);
    }

    @Test
    @DisplayName("deleteCurrentUser deletes user applications and user record")
    void testDeleteCurrentUser() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        userService.deleteCurrentUser();

        verify(jobApplicationRepository).deleteByUserId(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("deleteCurrentUser throws ResourceNotFoundException when user does not exist")
    void testDeleteCurrentUserNotFound() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser())
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: " + USER_ID);

        verify(jobApplicationRepository, never()).deleteByUserId(any());
        verify(userRepository, never()).deleteById(any());
    }
}
