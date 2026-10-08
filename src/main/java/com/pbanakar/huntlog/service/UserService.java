package com.pbanakar.huntlog.service;

import com.pbanakar.huntlog.dto.request.UpdatePreferencesRequest;
import com.pbanakar.huntlog.dto.response.UserPreferencesResponse;
import com.pbanakar.huntlog.exception.ResourceNotFoundException;
import com.pbanakar.huntlog.model.User;
import com.pbanakar.huntlog.repository.UserRepository;
import com.pbanakar.huntlog.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getPreferences() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return new UserPreferencesResponse(Boolean.TRUE.equals(user.getEmailRemindersEnabled()));
    }

    @Transactional
    public UserPreferencesResponse updatePreferences(UpdatePreferencesRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setEmailRemindersEnabled(request.getEmailRemindersEnabled());
        userRepository.save(user);

        return new UserPreferencesResponse(user.getEmailRemindersEnabled());
    }
}
