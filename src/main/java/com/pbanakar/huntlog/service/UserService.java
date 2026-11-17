package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.JobApplicationRepository;
import com.pbanakar.huntlog.repository.UserRepository;
import com.pbanakar.huntlog.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JobApplicationRepository jobApplicationRepository;

    public UserService(UserRepository userRepository, JobApplicationRepository jobApplicationRepository) {
        this.userRepository = userRepository;
        this.jobApplicationRepository = jobApplicationRepository;
    }

    @Transactional
    public void deleteCurrentUser() {
        Long userId = SecurityUtils.getCurrentUserId();
        userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        jobApplicationRepository.deleteByUserId(userId);
        userRepository.deleteById(userId);
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return toPreferencesResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getEmailRemindersEnabled() != null) {
            user.setEmailRemindersEnabled(request.getEmailRemindersEnabled());
        }
        if (request.getEmailSyncEnabled() != null) {
            user.setEmailSyncEnabled(request.getEmailSyncEnabled());
        }
        if (request.getImapHost() != null) {
            user.setImapHost(request.getImapHost().trim());
        }
        if (request.getImapPort() != null) {
            user.setImapPort(request.getImapPort());
        }
        if (request.getImapUsername() != null) {
            user.setImapUsername(request.getImapUsername().trim());
        }
        if (request.getImapPassword() != null && !request.getImapPassword().trim().isEmpty()) {
            user.setImapPassword(request.getImapPassword().trim());
        }

        userRepository.save(user);

        return toPreferencesResponse(user);
    }

    private UserPreferencesResponse toPreferencesResponse(User user) {
        boolean hasPassword = user.getImapPassword() != null && !user.getImapPassword().trim().isEmpty();
        return new UserPreferencesResponse(
                Boolean.TRUE.equals(user.getEmailRemindersEnabled()),
                Boolean.TRUE.equals(user.getEmailSyncEnabled()),
                user.getImapHost(),
                user.getImapPort(),
                user.getImapUsername(),
                hasPassword,
                user.getLastEmailSyncedAt()
        );
    }
}
